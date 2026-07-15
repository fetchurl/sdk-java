package io.github.fetchurl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void expectedHexLength() {
        assertEquals(40, Algo.expectedHexLength("sha1"));
        assertEquals(64, Algo.expectedHexLength("sha256"));
        assertEquals(128, Algo.expectedHexLength("sha512"));
    }

    @Test
    void normalizeContentHashLowercases() {
        String upper = "E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855";
        assertEquals(upper.toLowerCase(), Algo.normalizeContentHash("sha256", upper));
    }

    @Test
    void normalizeContentHashRejectsWrongLength() {
        assertThrows(FetchUrlException.class, () -> Algo.normalizeContentHash("sha256", "abcd"));
    }

    @Test
    void normalizeContentHashRejectsNonHex() {
        String almost =
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b85g";
        assertThrows(FetchUrlException.class, () -> Algo.normalizeContentHash("sha256", almost));
    }

    @Test
    void normalizeContentHashRejectsBlank() {
        assertThrows(FetchUrlException.class, () -> Algo.normalizeContentHash("sha256", "  "));
        assertThrows(FetchUrlException.class, () -> Algo.normalizeContentHash("sha256", null));
    }
}
