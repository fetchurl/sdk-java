package io.github.fetchurl;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    }

    @Test
    void singleUnquoted() {
        assertEquals(
                Collections.singletonList("http://cache.local:8080/api/fetchurl"),
                Sfv.parseFetchurlServer("http://cache.local:8080/api/fetchurl"));
    }
}
