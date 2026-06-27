package io.github.fetchurl;

/** Base exception for the fetchurl SDK. */
public class FetchUrlException extends RuntimeException {
    public FetchUrlException(String message) {
        super(message);
    }

    public FetchUrlException(String message, Throwable cause) {
        super(message, cause);
    }
}
