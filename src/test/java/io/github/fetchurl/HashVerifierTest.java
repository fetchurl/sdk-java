package io.github.fetchurl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.junit.jupiter.api.Test;

class HashVerifierTest {
    static String sha256Hex(byte[] data) throws Exception {
        return HashVerifier.toHex(MessageDigest.getInstance("SHA-256").digest(data));
    }

    @Test
    void success() throws Exception {
        byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);
        String h = sha256Hex(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HashVerifier v = new HashVerifier("sha256", h, out);
        v.write(data);
        assertEquals(data.length, v.getBytesWritten());
        v.finish();
        assertArrayEquals(data, out.toByteArray());
    }

    @Test
    void mismatch() throws Exception {
        byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);
        String wrong = sha256Hex("wrong".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HashVerifier v = new HashVerifier("sha256", wrong, out);
        v.write(data);
        HashMismatchException ex = assertThrows(HashMismatchException.class, v::finish);
        assertEquals(wrong, ex.getExpected());
    }

    @Test
    void acceptsUppercaseExpectedHex() throws Exception {
        byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);
        String h = sha256Hex(data).toUpperCase();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HashVerifier v = new HashVerifier("sha256", h, out);
        v.write(data);
        v.finish();
        assertArrayEquals(data, out.toByteArray());
    }

    @Test
    void finishIsOnceOnly() throws Exception {
        byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);
        String h = sha256Hex(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HashVerifier v = new HashVerifier("sha256", h, out);
        v.write(data);
        v.finish();
        assertThrows(IllegalStateException.class, v::finish);
    }

    @Test
    void writeAfterFinishFails() throws Exception {
        byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);
        String h = sha256Hex(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HashVerifier v = new HashVerifier("sha256", h, out);
        v.write(data);
        v.finish();
        assertThrows(IOException.class, () -> v.write(1));
        assertThrows(IOException.class, () -> v.write(data, 0, data.length));
    }

    @Test
    void rejectsNonHexExpected() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String bad =
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b85g";
        assertThrows(FetchUrlException.class, () -> new HashVerifier("sha256", bad, out));
    }

    @Test
    void rejectsWrongLengthExpected() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertThrows(FetchUrlException.class, () -> new HashVerifier("sha256", "abcd", out));
    }

    @Test
    void rejectsBlankExpected() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertThrows(FetchUrlException.class, () -> new HashVerifier("sha256", "  ", out));
    }
}
