package io.github.fetchurl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FetchAttemptTest {
    @Test
    void storesUrlAndEmptyHeaders() {
        FetchAttempt a = new FetchAttempt("https://cdn.example.com/file.bin");
        assertEquals("https://cdn.example.com/file.bin", a.getUrl());
        assertTrue(a.getHeaders().isEmpty());
    }

    @Test
    void copiesHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Source-Urls", "\"https://a.com\"");
        FetchAttempt a = new FetchAttempt("https://cache.example/api", headers);
        assertEquals("\"https://a.com\"", a.getHeaders().get("X-Source-Urls"));
        // Defensive copy: mutating the input must not affect the attempt.
        headers.put("X-Source-Urls", "mutated");
        assertEquals("\"https://a.com\"", a.getHeaders().get("X-Source-Urls"));
    }

    @Test
    void rejectsNullUrl() {
        assertThrows(FetchUrlException.class, () -> new FetchAttempt(null));
        assertThrows(FetchUrlException.class, () -> new FetchAttempt(null, Collections.emptyMap()));
    }

    @Test
    void rejectsBlankUrl() {
        assertThrows(FetchUrlException.class, () -> new FetchAttempt(""));
        assertThrows(FetchUrlException.class, () -> new FetchAttempt("   "));
        assertThrows(FetchUrlException.class, () -> new FetchAttempt("\t\n"));
    }

    @Test
    void rejectsNullHeaderName() {
        Map<String, String> headers = new HashMap<>();
        headers.put(null, "value");
        assertThrows(NullPointerException.class, () -> new FetchAttempt("https://x", headers));
    }

    @Test
    void rejectsNullHeaderValue() {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Source-Urls", null);
        assertThrows(NullPointerException.class, () -> new FetchAttempt("https://x", headers));
    }
}
