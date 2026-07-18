package io.github.fetchurl;

import java.util.Locale;
import java.util.Set;

/** Hash algorithm helpers per the fetchurl spec. */
public final class Algo {
    private static final Set<String> SUPPORTED = Set.of("sha1", "sha256", "sha512");

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
        return SUPPORTED.contains(normalize(algo));
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
        String normalized = normalize(algo);
        switch (normalized) {
            case "sha1":
                return 40;
            case "sha256":
                return 64;
            case "sha512":
                return 128;
            default:
                throw new UnsupportedAlgorithmException(normalized);
        }
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
        String normalized = normalize(algo);
        int expectedLen = expectedHexLength(normalized);
        String lower = hash.toLowerCase(Locale.ROOT);
        if (lower.length() != expectedLen) {
            throw new FetchUrlException(
                    "hash must be "
                            + expectedLen
                            + " hex characters for "
                            + normalized
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
        String normalized = normalize(algo);
        switch (normalized) {
            case "sha1":
                return "SHA-1";
            case "sha256":
                return "SHA-256";
            case "sha512":
                return "SHA-512";
            default:
                throw new UnsupportedAlgorithmException(normalized);
        }
    }
}
