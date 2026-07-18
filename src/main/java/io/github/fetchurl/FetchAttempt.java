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
        // Fail early — blank URLs only surface later as opaque HTTP client errors.
        if (url == null || url.isBlank()) {
            throw new FetchUrlException("url must not be blank");
        }
        this.url = url;
        if (headers == null || headers.isEmpty()) {
            this.headers = Collections.emptyMap();
        } else {
            Map<String, String> copy = new LinkedHashMap<>(headers.size());
            for (Map.Entry<String, String> e : headers.entrySet()) {
                Objects.requireNonNull(e.getKey(), "header name");
                Objects.requireNonNull(e.getValue(), "header value");
                copy.put(e.getKey(), e.getValue());
            }
            this.headers = Collections.unmodifiableMap(copy);
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
