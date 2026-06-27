package io.github.fetchurl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AlgoTest {
    @Test
    void normalizeLowercase() {
        assertEquals("sha256", Algo.normalize("SHA-256"));
    }

    @Test
    void normalizeAlreadyNormalized() {
        assertEquals("sha256", Algo.normalize("sha256"));
    }

    @Test
    void normalizeStripsNonAlnum() {
        assertEquals("sha512", Algo.normalize("SHA_512"));
    }

    @Test
    void supported() {
        assertTrue(Algo.isSupported("sha256"));
        assertTrue(Algo.isSupported("SHA-256"));
        assertTrue(Algo.isSupported("sha1"));
        assertTrue(Algo.isSupported("sha512"));
    }

    @Test
    void unsupported() {
        assertFalse(Algo.isSupported("md5"));
    }
}
