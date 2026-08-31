package io.github.fetchurl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** RFC 8941 structured field string-list helpers for FETCHURL_SERVER and X-Source-Urls. */
public final class Sfv {
    private Sfv() {}

    /**
     * Encode URLs as an RFC 8941 string list for the {@code X-Source-Urls} header.
     *
     * <p>Rejects null, blank, and ASCII control characters so callers fail early instead of
     * hitting {@code NullPointerException} or a late HTTP-client rejection when the value is
     * used as a header.
     *
     * @throws FetchUrlException if any URL is null, blank, or contains a control character
     */
    public static String encodeSourceUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < urls.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            String url = urls.get(i);
            requireEncodableUrl(url);
            sb.append('"');
            for (int j = 0; j < url.length(); j++) {
                char c = url.charAt(j);
                if (c == '\\' || c == '"') {
                    sb.append('\\');
                }
                sb.append(c);
            }
            sb.append('"');
        }
        return sb.toString();
    }

    /**
     * Validate a URL before embedding it in an RFC 8941 string / HTTP header field.
     *
     * @throws FetchUrlException if {@code url} is null, blank, or contains ASCII controls
     */
    static void requireEncodableUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new FetchUrlException("source URL must not be blank");
        }
        requireNoControlChars(url, "source URL");
    }

    /**
     * Reject ASCII control characters and DEL (RFC 9110 header field-value / request-target).
     *
     * @throws FetchUrlException if {@code value} contains a control character
     */
    static void requireNoControlChars(String value, String what) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x20 || c == 0x7f) {
                throw new FetchUrlException(what + " must not contain control characters");
            }
        }
    }

    /**
     * Parse the {@code FETCHURL_SERVER} environment variable value (RFC 8941 string list).
     *
     * <p>If the value does not start with {@code "}, the whole string is a single server URL.
     */
    public static List<String> parseFetchurlServer(String value) {
        if (value == null) {
            return Collections.emptyList();
        }
        value = value.trim();
        if (value.isEmpty()) {
            return Collections.emptyList();
        }
        if (!value.startsWith("\"")) {
            return Collections.singletonList(value);
        }
        return parseSfvStringList(value);
    }

    static List<String> parseSfvStringList(String input) {
        List<String> results = new ArrayList<>();
        int i = 0;
        int n = input.length();
        while (i < n) {
            while (i < n && (input.charAt(i) == ' ' || input.charAt(i) == '\t')) {
                i++;
            }
            if (i >= n) {
                break;
            }
            if (input.charAt(i) != '"') {
                while (i < n && input.charAt(i) != ',') {
                    i++;
                }
                if (i < n) {
                    i++;
                }
                continue;
            }
            i++;
            StringBuilder s = new StringBuilder();
            while (i < n) {
                char c = input.charAt(i);
                if (c == '\\' && i + 1 < n) {
                    s.append(input.charAt(i + 1));
                    i += 2;
                } else if (c == '"') {
                    i++;
                    break;
                } else {
                    s.append(c);
                    i++;
                }
            }
            results.add(s.toString());
            while (i < n && input.charAt(i) != ',') {
                i++;
            }
            if (i < n) {
                i++;
            }
        }
        return results;
    }
}
