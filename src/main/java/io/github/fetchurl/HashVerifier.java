package io.github.fetchurl;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Wraps an {@link OutputStream}, computes a hash of all written data, and verifies it on {@link
 * #finish()}.
 */
public final class HashVerifier extends OutputStream {
    private final OutputStream out;
    private final MessageDigest digest;
    private final String expectedHash;
    private long bytesWritten;
    private boolean closed;
    private boolean finished;

    public HashVerifier(String algo, String expectedHash, OutputStream out) {
        this.out = Objects.requireNonNull(out, "out");
        String normalized = Algo.normalize(algo);
        // Spec: hashes MUST be lowercase hex of the full digest. Fail early on garbage.
        this.expectedHash = Algo.normalizeContentHash(normalized, expectedHash);
        try {
            this.digest = MessageDigest.getInstance(Algo.messageDigestName(normalized));
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedAlgorithmException(normalized);
        }
    }

    public long getBytesWritten() {
        return bytesWritten;
    }

    private void ensureWritable() throws IOException {
        if (closed || finished) {
            throw new IOException("HashVerifier is closed");
        }
    }

    @Override
    public void write(int b) throws IOException {
        ensureWritable();
        out.write(b);
        digest.update((byte) b);
        bytesWritten++;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        ensureWritable();
        out.write(b, off, len);
        digest.update(b, off, len);
        bytesWritten += len;
    }

    @Override
    public void flush() throws IOException {
        if (closed) {
            throw new IOException("HashVerifier is closed");
        }
        out.flush();
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            out.close();
        }
    }

    /**
     * Finalize the hash and verify it matches the expected value.
     *
     * <p>May be called only once. Further {@link #write} calls fail.
     *
     * @throws HashMismatchException if the digest does not match
     * @throws IllegalStateException if {@code finish()} was already called
     */
    public void finish() {
        if (finished) {
            throw new IllegalStateException("HashVerifier already finished");
        }
        finished = true;
        String actual = toHex(digest.digest());
        if (!actual.equals(expectedHash)) {
            throw new HashMismatchException(expectedHash, actual);
        }
    }

    static String toHex(byte[] bytes) {
        char[] hex = new char[bytes.length * 2];
        final char[] digits = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xff;
            hex[i * 2] = digits[v >>> 4];
            hex[i * 2 + 1] = digits[v & 0x0f];
        }
        return new String(hex);
    }
}
