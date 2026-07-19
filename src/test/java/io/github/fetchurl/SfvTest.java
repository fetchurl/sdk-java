package io.github.fetchurl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class SfvTest {
    @Test
    void encode() {
        assertEquals(
                "\"https://a.com\", \"https://b.com\"",
                Sfv.encodeSourceUrls(Arrays.asList("https://a.com", "https://b.com")));
    }

    @Test
    void parse() {
        List<String> parsed = Sfv.parseFetchurlServer("\"https://a.com\", \"https://b.com\"");
        assertEquals(Arrays.asList("https://a.com", "https://b.com"), parsed);
    }

    @Test
    void roundtrip() {
        List<String> urls =
                Arrays.asList("https://cdn.example.com/f.tar.gz", "https://mirror.org/a.tgz");
        String encoded = Sfv.encodeSourceUrls(urls);
        assertEquals(urls, Sfv.parseFetchurlServer(encoded));
    }

    @Test
    void parseWithParams() {
        List<String> parsed =
                Sfv.parseFetchurlServer("\"https://a.com\";q=0.9, \"https://b.com\"");
        assertEquals(Arrays.asList("https://a.com", "https://b.com"), parsed);
    }

    @Test
    void empty() {
        assertEquals(Collections.emptyList(), Sfv.parseFetchurlServer(""));
        assertEquals(Collections.emptyList(), Sfv.parseFetchurlServer(null));
        assertEquals("", Sfv.encodeSourceUrls(null));
        assertEquals("", Sfv.encodeSourceUrls(Collections.emptyList()));
    }

    @Test
    void singleUnquoted() {
        assertEquals(
                Collections.singletonList("http://cache.local:8080/api/fetchurl"),
                Sfv.parseFetchurlServer("http://cache.local:8080/api/fetchurl"));
    }

    @Test
    void encodeRejectsNullUrl() {
        List<String> urls = new ArrayList<>();
        urls.add("https://a.com");
        urls.add(null);
        assertThrows(FetchUrlException.class, () -> Sfv.encodeSourceUrls(urls));
    }

    @Test
    void encodeRejectsBlankUrl() {
        assertThrows(
                FetchUrlException.class,
                () -> Sfv.encodeSourceUrls(Arrays.asList("https://a.com", "  ")));
        assertThrows(
                FetchUrlException.class,
                () -> Sfv.encodeSourceUrls(Collections.singletonList("")));
    }

    @Test
    void encodeRejectsControlCharacters() {
        assertThrows(
                FetchUrlException.class,
                () ->
                        Sfv.encodeSourceUrls(
                                Collections.singletonList("https://a.com/\r\nX-Injected: 1")));
        assertThrows(
                FetchUrlException.class,
                () -> Sfv.encodeSourceUrls(Collections.singletonList("https://a.com/\u007f")));
    }

    @Test
    void encodeEscapesQuotesAndBackslashes() {
        assertEquals(
                "\"https://a.com/\\\"x\\\\y\"",
                Sfv.encodeSourceUrls(Collections.singletonList("https://a.com/\"x\\y")));
    }
}
