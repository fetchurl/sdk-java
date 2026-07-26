package io.github.fetchurl;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Result of a GET via {@link Fetcher}: status code and response body stream.
 *
 * <p>Owns {@link #getBody()}: callers should close this response (or the body stream) when finished,
 * including non-200 responses, so the underlying HTTP connection can be released. Prefer
 * try-with-resources on the {@code FetchResponse}. Closing the body stream and {@link #close()} are
 * equivalent and idempotent — either path closes the underlying stream at most once.
 */
public final class FetchResponse implements AutoCloseable {
    private final int statusCode;
    /** Underlying body from the HTTP client; closed at most once via {@link #close()}. */
    private final InputStream rawBody;
    /**
     * View returned by {@link #getBody()}. Its {@link InputStream#close()} delegates to {@link
     * #close()} so nested try-with-resources does not double-close the raw stream.
     */
    private final InputStream body;
    private boolean closed;

    public FetchResponse(int statusCode, InputStream body) {
        this.statusCode = statusCode;
        this.rawBody = Objects.requireNonNull(body, "body");
        this.body =
                new FilterInputStream(rawBody) {
                    @Override
                    public void close() throws IOException {
                        FetchResponse.this.close();
                    }
                };
    }

    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Response body. Closing this stream is the same as {@link #close()}. Never null.
     *
     * <p>The returned stream may be a view over the stream passed to the constructor; do not
     * assume reference equality with that argument.
     */
    public InputStream getBody() {
        return body;
    }

    /**
     * Closes the response body stream.
     *
     * <p>Idempotent: safe to call more than once, and safe after the caller already closed {@link
     * #getBody()}. Matches the close discipline used by {@link HashVerifier}.
     */
    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            rawBody.close();
        }
    }
}
