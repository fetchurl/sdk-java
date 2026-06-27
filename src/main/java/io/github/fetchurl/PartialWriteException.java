package io.github.fetchurl;

/** Bytes were written before failure; output is tainted. */
public class PartialWriteException extends FetchUrlException {
    private final Throwable causeError;

    public PartialWriteException(Throwable cause) {
        super("partial write: " + (cause != null ? cause.getMessage() : "unknown"), cause);
        this.causeError = cause;
    }

    public Throwable getCauseError() {
        return causeError;
    }
}
