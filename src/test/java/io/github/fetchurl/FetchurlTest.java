package io.github.fetchurl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class FetchurlTest {
    private HttpServer serverA;
    private HttpServer serverB;

    static String sha256Hex(byte[] data) throws Exception {
        return HashVerifier.toHex(MessageDigest.getInstance("SHA-256").digest(data));
    }

    private static HttpServer start(int status, byte[] body) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/",
                exchange -> {
                    exchange.sendResponseHeaders(status, body == null ? -1 : body.length);
                    if (body != null && status == 200) {
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(body);
                        }
                    } else {
                        exchange.close();
                    }
                });
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        return server;
    }

    private static String urlOf(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/";
    }

    @AfterEach
    void tearDown() {
        if (serverA != null) {
            serverA.stop(0);
        }
        if (serverB != null) {
            serverB.stop(0);
        }
    }

    @Test
    void directDownload() throws Exception {
        byte[] content = "test content".getBytes(StandardCharsets.UTF_8);
        String h = sha256Hex(content);
        serverA = start(200, content);
        String url = urlOf(serverA);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FetchSession session =
                FetchSession.withServers(Collections.emptyList(), "sha256", h, Collections.singletonList(url));
        Fetchurl.fetch(new JdkHttpClientFetcher(), session, out);
        assertArrayEquals(content, out.toByteArray());
    }

    @Test
    void hashMismatchRaisesPartial() throws Exception {
        serverA = start(200, "wrong content".getBytes(StandardCharsets.UTF_8));
        String url = urlOf(serverA);
        String h = sha256Hex("right".getBytes(StandardCharsets.UTF_8));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FetchSession session =
                FetchSession.withServers(Collections.emptyList(), "sha256", h, Collections.singletonList(url));
        assertThrows(
                PartialWriteException.class,
                () -> Fetchurl.fetch(new JdkHttpClientFetcher(), session, out));
    }

    @Test
    void allSourcesFailed() throws Exception {
        serverA = start(404, null);
        String url = urlOf(serverA);
        String h = sha256Hex("x".getBytes(StandardCharsets.UTF_8));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FetchSession session =
                FetchSession.withServers(Collections.emptyList(), "sha256", h, Collections.singletonList(url));
        assertThrows(
                AllSourcesFailedException.class,
                () -> Fetchurl.fetch(new JdkHttpClientFetcher(), session, out));
    }

    @Test
    void serverFallbackToDirect() throws Exception {
        byte[] content = "fallback content".getBytes(StandardCharsets.UTF_8);
        String h = sha256Hex(content);
        serverA = start(500, null);
        serverB = start(200, content);
        String badBase = "http://127.0.0.1:" + serverA.getAddress().getPort() + "/api/fetchurl";
        // HttpServer only has "/"; server paths will 404 unless we use root as base...
        // Use full URL path on bad server: any path returns 500 from our single context "/"
        // Actually createContext("/") matches all prefixes in HttpServer.
        String goodUrl = urlOf(serverB);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FetchSession session =
                FetchSession.withServers(
                        Collections.singletonList(badBase),
                        "sha256",
                        h,
                        Collections.singletonList(goodUrl));
        Fetchurl.fetch(new JdkHttpClientFetcher(), session, out);
        assertArrayEquals(content, out.toByteArray());
    }
}
