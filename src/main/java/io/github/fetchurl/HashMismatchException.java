package io.github.fetchurl;

/** The content hash does not match the expected hash. */
public class HashMismatchException extends FetchUrlException {
    private final String expected;
    private final String actual;

    public HashMismatchException(String expected, String actual) {
        super("hash mismatch: expected " + expected + ", got " + actual);
        this.expected = expected;
        this.actual = actual;
    }

    public String getExpected() {
        return expected;
    }

    public String getActual() {
        return actual;
    }
}
