package io.github.fetchurl;

import java.io.InputStream;
import java.util.Objects;

/** Result of a GET via {@link Fetcher}: status code and response body stream. */
public final class FetchResponse {
    private final int statusCode;
    private final InputStream body;

    public FetchResponse(int statusCode, InputStream body) {
        this.statusCode = statusCode;
        this.body = Objects.requireNonNull(body, "body");
    }

    public int getStatusCode() {
        return statusCode;
    }

    public InputStream getBody() {
        return body;
    }
}
