package io.github.fetchurl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * {@link Fetcher} using {@link java.net.http.HttpClient} (Java 11+, zero extra deps).
 *
 * <p>The default client uses a 30s connect timeout. Each GET also has a request timeout (default
 * 60s) so a stalled peer after connect cannot hang the caller forever.
 */
public final class JdkHttpClientFetcher implements Fetcher {
    static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(30);
    static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(60);
    /** Default User-Agent so operators can identify SDK traffic in access logs. */
    static final String DEFAULT_USER_AGENT = "fetchurl-sdk";

    private final HttpClient client;
    private final Duration requestTimeout;

    public JdkHttpClientFetcher() {
        this(
                HttpClient.newBuilder().connectTimeout(DEFAULT_CONNECT_TIMEOUT).build(),
                DEFAULT_REQUEST_TIMEOUT);
    }

    public JdkHttpClientFetcher(HttpClient client) {
        this(client, DEFAULT_REQUEST_TIMEOUT);
    }

    public JdkHttpClientFetcher(HttpClient client, Duration requestTimeout) {
        this.client = Objects.requireNonNull(client, "client");
        this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout");
    }

    public static JdkHttpClientFetcher of(HttpClient client) {
        return new JdkHttpClientFetcher(client);
    }

    @Override
    public FetchResponse get(String url, Map<String, String> headers) throws IOException {
        HttpRequest.Builder builder =
                HttpRequest.newBuilder(URI.create(url)).timeout(requestTimeout).GET();
        boolean hasUserAgent = false;
        if (headers != null) {
            for (Map.Entry<String, String> e : headers.entrySet()) {
                if ("user-agent".equalsIgnoreCase(e.getKey())) {
                    hasUserAgent = true;
                }
                builder.header(e.getKey(), e.getValue());
            }
        }
        if (!hasUserAgent) {
            builder.header("User-Agent", DEFAULT_USER_AGENT);
        }
        try {
            HttpResponse<InputStream> resp =
                    client.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
            InputStream body = resp.body();
            if (body == null) {
                body = new ByteArrayInputStream(new byte[0]);
            }
            return new FetchResponse(resp.statusCode(), body);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("request interrupted", e);
        }
    }
}
