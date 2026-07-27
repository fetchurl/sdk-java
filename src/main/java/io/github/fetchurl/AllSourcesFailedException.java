package io.github.fetchurl;

/** All servers and sources failed to provide the content. */
public class AllSourcesFailedException extends FetchUrlException {
    public AllSourcesFailedException() {
        this(null);
    }

    public AllSourcesFailedException(Throwable lastError) {
        super("all sources failed", lastError);
    }

    /**
     * Last transport or protocol error when known.
     *
     * <p>Same as {@link #getCause()}; retained for existing callers.
     */
    public Throwable getLastError() {
        return getCause();
    }
}
