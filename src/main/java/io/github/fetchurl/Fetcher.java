package io.github.fetchurl;

import java.io.IOException;
import java.util.Map;

/**
 * Sync HTTP client abstraction. Implement this to plug in any HTTP library.
 *
 * <p>Example with a custom client: perform GET, return {@code (statusCode, bodyStream)}.
 */
@FunctionalInterface
public interface Fetcher {
    /**
     * Make a GET request.
     *
     * @param url request URL
     * @param headers headers to send (may be empty)
     * @return status code and readable body; the caller owns the response and must close it
     *     (see {@link FetchResponse#close()}) when finished, including on non-200 status when the
     *     body is not fully consumed
     * @throws IOException on transport failure
     */
    FetchResponse get(String url, Map<String, String> headers) throws IOException;
}
