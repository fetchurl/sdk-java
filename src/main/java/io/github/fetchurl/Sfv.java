package io.github.fetchurl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** RFC 8941 structured field string-list helpers for FETCHURL_SERVER and X-Source-Urls. */
public final class Sfv {
    private Sfv() {}

    /** Encode URLs as an RFC 8941 string list for the {@code X-Source-Urls} header. */
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
