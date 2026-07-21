package io.github.fetchurl;

import java.util.Locale;

/** Hash algorithm helpers per the fetchurl spec. */
public final class Algo {
    /**
     * Algorithms in scope for this SDK. Single table so support checks, hex length, and
     * {@link java.security.MessageDigest} names cannot drift.
     */
    private enum Known {
        SHA1("sha1", "SHA-1", 40),
        SHA256("sha256", "SHA-256", 64),
        SHA512("sha512", "SHA-512", 128);

        final String token;
        final String messageDigestName;
        final int hexLength;

        Known(String token, String messageDigestName, int hexLength) {
            this.token = token;
            this.messageDigestName = messageDigestName;
            this.hexLength = hexLength;
        }

        static Known find(String normalized) {
            for (Known k : values()) {
                if (k.token.equals(normalized)) {
                    return k;
                }
            }
            return null;
        }
    }

    private Algo() {}

    /**
     * Normalize algorithm name per spec: lowercase, only {@code [a-z0-9]}.
     *
     * <p>Examples: {@code "SHA-256"} → {@code "sha256"}, {@code "SHA_512"} → {@code "sha512"}.
     */
    public static String normalize(String name) {
        if (name == null) {
            return "";
        }
        String lower = name.toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Check if a hash algorithm is supported ({@code sha1}, {@code sha256}, {@code sha512}). */
    public static boolean isSupported(String algo) {
        return Known.find(normalize(algo)) != null;
    }

    /**
     * Expected hex length of a full digest for an algorithm name.
     *
     * <p>The name is {@link #normalize(String) normalized} first, matching {@link
     * #isSupported(String)} so callers can pass either {@code "sha256"} or {@code "SHA-256"}.
     *
     * @throws UnsupportedAlgorithmException if the algorithm is not supported
     */
    public static int expectedHexLength(String algo) {
        return requireKnown(algo).hexLength;
    }

    /**
     * Normalize a content hash per the fetchurl spec: full-length lowercase hex.
     *
     * <p>Rejects null, blank, non-hex, and wrong-length values before any network I/O. The
     * algorithm name is {@link #normalize(String) normalized} first (same rules as {@link
     * #isSupported(String)}).
     *
     * @param algo hash algorithm name (e.g. {@code "sha256"} or {@code "SHA-256"})
     * @param hash expected hash (mixed case accepted)
     * @return lowercase hex of the correct length for {@code algo}
     * @throws FetchUrlException if the hash is missing or not valid hex for the algorithm
     * @throws UnsupportedAlgorithmException if {@code algo} is not supported
     */
    public static String normalizeContentHash(String algo, String hash) {
        if (hash == null || hash.isBlank()) {
            throw new FetchUrlException("hash is required");
        }
        Known known = requireKnown(algo);
        String lower = hash.toLowerCase(Locale.ROOT);
        if (lower.length() != known.hexLength) {
            throw new FetchUrlException(
                    "hash must be "
                            + known.hexLength
                            + " hex characters for "
                            + known.token
                            + " (got "
                            + lower.length()
                            + ")");
        }
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if ((c < '0' || c > '9') && (c < 'a' || c > 'f')) {
                throw new FetchUrlException("hash must be hexadecimal");
            }
        }
        return lower;
    }

    /** Map algo to {@link java.security.MessageDigest} name (normalizes first). */
    static String messageDigestName(String algo) {
        return requireKnown(algo).messageDigestName;
    }

    private static Known requireKnown(String algo) {
        String normalized = normalize(algo);
        Known known = Known.find(normalized);
        if (known == null) {
            throw new UnsupportedAlgorithmException(normalized);
        }
        return known;
    }
}
