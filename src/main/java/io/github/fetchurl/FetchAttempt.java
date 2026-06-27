package io.github.fetchurl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** A single fetch attempt with URL and headers. */
public final class FetchAttempt {
    private final String url;
    private final Map<String, String> headers;

    public FetchAttempt(String url, Map<String, String> headers) {
        this.url = Objects.requireNonNull(url, "url");
        if (headers == null || headers.isEmpty()) {
            this.headers = Collections.emptyMap();
        } else {
            this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(headers));
        }
    }

    public FetchAttempt(String url) {
        this(url, null);
    }

    /** The URL to make a GET request to. */
    public String getUrl() {
        return url;
    }

    /** Headers to include (e.g. {@code X-Source-Urls}). Never null. */
    public Map<String, String> getHeaders() {
        return headers;
    }
}
