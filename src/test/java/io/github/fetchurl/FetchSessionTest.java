package io.github.fetchurl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FetchSessionTest {
    static String sha256Hex(byte[] data) throws Exception {
        return HashVerifier.toHex(MessageDigest.getInstance("SHA-256").digest(data));
    }

    @Test
    void missingSourceUrls() {
        assertThrows(
                MissingSourceUrlsException.class,
                () -> FetchSession.withServers(Collections.emptyList(), "sha256", "abc", Collections.emptyList()));
    }

    @Test
    void unsupportedAlgo() {
        assertThrows(
                UnsupportedAlgorithmException.class,
                () ->
                        FetchSession.withServers(
                                Collections.emptyList(),
                                "md5",
                                "abc",
                                Collections.singletonList("http://src")));
    }

    @Test
    void attemptOrdering() throws Exception {
        String h = sha256Hex("test".getBytes(StandardCharsets.UTF_8));
        List<String> servers =
                Arrays.asList("http://cache1/api/fetchurl", "http://cache2/api/fetchurl");
        FetchSession session =
                FetchSession.withServers(servers, "sha256", h, Collections.singletonList("http://src1"));

        Optional<FetchAttempt> a1 = session.nextAttempt();
        assertTrue(a1.isPresent());
        assertTrue(a1.get().getUrl().startsWith("http://cache1/api/fetchurl/sha256/"));
        assertTrue(a1.get().getHeaders().containsKey("X-Source-Urls"));

        Optional<FetchAttempt> a2 = session.nextAttempt();
        assertTrue(a2.isPresent());
        assertTrue(a2.get().getUrl().startsWith("http://cache2/api/fetchurl/sha256/"));

        Optional<FetchAttempt> a3 = session.nextAttempt();
        assertTrue(a3.isPresent());
        assertEquals("http://src1", a3.get().getUrl());
        assertTrue(a3.get().getHeaders().isEmpty());

        assertFalse(session.nextAttempt().isPresent());
        assertFalse(session.succeeded());
    }

    @Test
    void successStops() throws Exception {
        String h = sha256Hex("test".getBytes(StandardCharsets.UTF_8));
        FetchSession session =
                FetchSession.withServers(
                        Collections.singletonList("http://cache/api/fetchurl"),
                        "sha256",
                        h,
                        Collections.singletonList("http://src"));
        session.nextAttempt();
        session.reportSuccess();
        assertTrue(session.succeeded());
        assertFalse(session.nextAttempt().isPresent());
    }

    @Test
    void partialStops() throws Exception {
        String h = sha256Hex("test".getBytes(StandardCharsets.UTF_8));
        FetchSession session =
                FetchSession.withServers(
                        Collections.singletonList("http://cache/api/fetchurl"),
                        "sha256",
                        h,
                        Collections.singletonList("http://src"));
        session.nextAttempt();
        session.reportPartial();
        assertFalse(session.succeeded());
        assertFalse(session.nextAttempt().isPresent());
    }

    @Test
    void serverHasSourceHeader() throws Exception {
        String h = sha256Hex("test".getBytes(StandardCharsets.UTF_8));
        FetchSession session =
                FetchSession.withServers(
                        Collections.singletonList("http://cache/api/fetchurl"),
                        "sha256",
                        h,
                        Arrays.asList("http://src1", "http://src2"));
        FetchAttempt attempt = session.nextAttempt().orElseThrow(AssertionError::new);
        String header = attempt.getHeaders().get("X-Source-Urls");
        List<String> parsed = Sfv.parseFetchurlServer(header);
        assertTrue(parsed.contains("http://src1"));
        assertTrue(parsed.contains("http://src2"));
    }

    @Test
    void lowercasesHashInServerUrl() throws Exception {
        String h = sha256Hex("test".getBytes(StandardCharsets.UTF_8));
        String upper = h.toUpperCase();
        FetchSession session =
                FetchSession.withServers(
                        Collections.singletonList("http://cache/api/fetchurl"),
                        "sha256",
                        upper,
                        Collections.singletonList("http://src"));
        FetchAttempt attempt = session.nextAttempt().orElseThrow(AssertionError::new);
        assertTrue(
                attempt.getUrl().endsWith("/sha256/" + h),
                "server URL must use lowercase hash, got " + attempt.getUrl());
    }

    @Test
    void nullHashRejected() {
        assertThrows(
                FetchUrlException.class,
                () ->
                        FetchSession.withServers(
                                Collections.emptyList(),
                                "sha256",
                                null,
                                Collections.singletonList("http://src")));
    }

    @Test
    void blankHashRejected() {
        assertThrows(
                FetchUrlException.class,
                () ->
                        FetchSession.withServers(
                                Collections.emptyList(),
                                "sha256",
                                "   ",
                                Collections.singletonList("http://src")));
    }
}
