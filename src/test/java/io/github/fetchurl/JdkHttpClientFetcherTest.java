package io.github.fetchurl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class JdkHttpClientFetcherTest {
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void requestTimeoutAbortsStalledPeer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/",
                exchange -> {
                    // Accept the connection but never send a response.
                    try {
                        Thread.sleep(30_000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
        JdkHttpClientFetcher fetcher =
                new JdkHttpClientFetcher(
                        java.net.http.HttpClient.newBuilder()
                                .connectTimeout(Duration.ofSeconds(5))
                                .build(),
                        Duration.ofMillis(500));

        IOException ex =
                assertThrows(
                        IOException.class, () -> fetcher.get(url, Collections.emptyMap()));
        // java.net.http.HttpTimeoutException extends IOException
        assertTrue(
                ex.getClass().getName().contains("Timeout")
                        || (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("timed")),
                "expected timeout IOException, got " + ex);
    }

    @Test
    void sendsDefaultUserAgent() throws Exception {
        AtomicReference<String> userAgent = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/",
                exchange -> {
                    userAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
                    byte[] body = new byte[0];
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().close();
                });
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
        try (java.io.InputStream ignored =
                new JdkHttpClientFetcher().get(url, Collections.emptyMap()).getBody()) {
            // consume
        }
        assertEquals(JdkHttpClientFetcher.DEFAULT_USER_AGENT, userAgent.get());
    }

    @Test
    void callerUserAgentWins() throws Exception {
        AtomicReference<String> userAgent = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/",
                exchange -> {
                    userAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
                    byte[] body = new byte[0];
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().close();
                });
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
        try (java.io.InputStream ignored =
                new JdkHttpClientFetcher()
                        .get(url, Collections.singletonMap("User-Agent", "my-app/1"))
                        .getBody()) {
            // consume
        }
        assertEquals("my-app/1", userAgent.get());
    }

    @Test
    void defaultConstructorReusesSharedClient() {
        JdkHttpClientFetcher a = new JdkHttpClientFetcher();
        JdkHttpClientFetcher b = new JdkHttpClientFetcher();
        assertSame(JdkHttpClientFetcher.DEFAULT_CLIENT, a.client());
        assertSame(JdkHttpClientFetcher.DEFAULT_CLIENT, b.client());
        assertEquals(
                JdkHttpClientFetcher.DEFAULT_CONNECT_TIMEOUT,
                JdkHttpClientFetcher.DEFAULT_CLIENT.connectTimeout().orElseThrow());
    }

    @Test
    void injectedClientIsNotReplacedByDefault() {
        HttpClient custom =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkHttpClientFetcher fetcher = new JdkHttpClientFetcher(custom);
        assertSame(custom, fetcher.client());
    }
}
