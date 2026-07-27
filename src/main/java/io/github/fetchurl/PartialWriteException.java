package io.github.fetchurl;

/** Bytes were written before failure; output is tainted. */
public class PartialWriteException extends FetchUrlException {
    public PartialWriteException(Throwable cause) {
        super("partial write: " + describe(cause), cause);
    }

    /**
     * Underlying failure that left the output tainted.
     *
     * <p>Same as {@link #getCause()}; retained for existing callers.
     */
    public Throwable getCauseError() {
        return getCause();
    }

    private static String describe(Throwable cause) {
        if (cause == null) {
            return "unknown";
        }
        String message = cause.getMessage();
        return message != null ? message : cause.toString();
    }
}
