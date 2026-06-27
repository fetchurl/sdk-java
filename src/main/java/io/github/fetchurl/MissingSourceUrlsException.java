package io.github.fetchurl;

/** Source URLs are required by the protocol. */
public class MissingSourceUrlsException extends FetchUrlException {
    public MissingSourceUrlsException() {
        super("source_urls is required");
    }
}
