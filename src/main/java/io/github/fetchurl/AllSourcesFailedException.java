package io.github.fetchurl;

/** All servers and sources failed to provide the content. */
public class AllSourcesFailedException extends FetchUrlException {
    private final Throwable lastError;

    public AllSourcesFailedException() {
        this(null);
    }

    public AllSourcesFailedException(Throwable lastError) {
        super("all sources failed", lastError);
        this.lastError = lastError;
    }

    public Throwable getLastError() {
        return lastError;
    }
}
