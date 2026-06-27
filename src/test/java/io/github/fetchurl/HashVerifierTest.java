package io.github.fetchurl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
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
}
