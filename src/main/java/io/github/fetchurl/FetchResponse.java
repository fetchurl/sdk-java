package io.github.fetchurl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Result of a GET via {@link Fetcher}: status code and response body stream.
 *
 * <p>Owns {@link #getBody()}: callers should close this response (or the body stream) when finished,
 * including non-200 responses, so the underlying HTTP connection can be released. Prefer
 * try-with-resources on the {@code FetchResponse}.
 */
public final class FetchResponse implements AutoCloseable {
    private final int statusCode;
    private final InputStream body;

    public FetchResponse(int statusCode, InputStream body) {
        this.statusCode = statusCode;
        this.body = Objects.requireNonNull(body, "body");
    }

    public int getStatusCode() {
        return statusCode;
    }

    /** Response body. Same stream closed by {@link #close()}. Never null. */
    public InputStream getBody() {
        return body;
    }

    /**
     * Closes the response body stream.
     *
     * <p>Safe to call more than once if the underlying stream follows the {@link
     * java.io.Closeable} contract (typical for JDK HTTP bodies).
     */
    @Override
    public void close() throws IOException {
        body.close();
    }
}
