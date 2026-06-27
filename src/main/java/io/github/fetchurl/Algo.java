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

    /** Map normalized algo to {@link java.security.MessageDigest} name. */
    static String messageDigestName(String normalizedAlgo) {
        switch (normalizedAlgo) {
            case "sha1":
                return "SHA-1";
            case "sha256":
                return "SHA-256";
            case "sha512":
                return "SHA-512";
            default:
                throw new UnsupportedAlgorithmException(normalizedAlgo);
        }
    }
}
