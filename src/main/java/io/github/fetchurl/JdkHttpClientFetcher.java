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

/** {@link Fetcher} using {@link java.net.http.HttpClient} (Java 11+, zero extra deps). */
public final class JdkHttpClientFetcher implements Fetcher {
    private final HttpClient client;

    public JdkHttpClientFetcher() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build());
    }

    public JdkHttpClientFetcher(HttpClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    public static JdkHttpClientFetcher of(HttpClient client) {
        return new JdkHttpClientFetcher(client);
    }

    @Override
    public FetchResponse get(String url, Map<String, String> headers) throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).GET();
        if (headers != null) {
            for (Map.Entry<String, String> e : headers.entrySet()) {
                builder.header(e.getKey(), e.getValue());
            }
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
