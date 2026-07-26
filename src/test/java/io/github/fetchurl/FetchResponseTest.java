package io.github.fetchurl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class FetchResponseTest {
    @Test
    void rejectsNullBody() {
        assertThrows(NullPointerException.class, () -> new FetchResponse(200, null));
    }

    @Test
    void exposesStatusAndBody() throws IOException {
        byte[] data = new byte[] {1, 2, 3};
        InputStream raw = new ByteArrayInputStream(data);
        FetchResponse response = new FetchResponse(204, raw);
        assertEquals(204, response.getStatusCode());
        assertEquals(1, response.getBody().read());
        assertEquals(2, response.getBody().read());
        assertEquals(3, response.getBody().read());
        assertEquals(-1, response.getBody().read());
    }

    @Test
    void getBodyIsStableView() {
        InputStream raw = new ByteArrayInputStream(new byte[] {1});
        FetchResponse response = new FetchResponse(200, raw);
        assertSame(response.getBody(), response.getBody());
    }

    @Test
    void closeClosesBody() throws IOException {
        AtomicBoolean closed = new AtomicBoolean();
        InputStream body =
                new InputStream() {
                    @Override
                    public int read() {
                        return -1;
                    }

                    @Override
                    public void close() {
                        closed.set(true);
                    }
                };
        FetchResponse response = new FetchResponse(200, body);
        response.close();
        assertTrue(closed.get());
    }

    @Test
    void tryWithResourcesClosesBody() throws IOException {
        AtomicInteger closeCount = new AtomicInteger();
        InputStream body =
                new InputStream() {
                    @Override
                    public int read() {
                        return -1;
                    }

                    @Override
                    public void close() {
                        closeCount.incrementAndGet();
                    }
                };
        try (FetchResponse response = new FetchResponse(404, body)) {
            assertEquals(404, response.getStatusCode());
        }
        assertEquals(1, closeCount.get());
    }

    @Test
    void closeIsIdempotent() throws IOException {
        AtomicInteger closeCount = new AtomicInteger();
        InputStream body =
                new InputStream() {
                    @Override
                    public int read() {
                        return -1;
                    }

                    @Override
                    public void close() {
                        closeCount.incrementAndGet();
                    }
                };
        FetchResponse response = new FetchResponse(200, body);
        response.close();
        response.close();
        assertEquals(1, closeCount.get());
    }

    @Test
    void closingBodyStreamClosesResponseOnce() throws IOException {
        AtomicInteger closeCount = new AtomicInteger();
        InputStream body =
                new InputStream() {
                    @Override
                    public int read() {
                        return -1;
                    }

                    @Override
                    public void close() {
                        closeCount.incrementAndGet();
                    }
                };
        FetchResponse response = new FetchResponse(200, body);
        response.getBody().close();
        response.close();
        assertEquals(1, closeCount.get());
    }

    @Test
    void nestedTryWithResourcesClosesUnderlyingOnce() throws IOException {
        AtomicInteger closeCount = new AtomicInteger();
        InputStream body =
                new InputStream() {
                    private boolean closed;

                    @Override
                    public int read() {
                        return -1;
                    }

                    @Override
                    public void close() throws IOException {
                        if (closed) {
                            throw new IOException("already closed");
                        }
                        closed = true;
                        closeCount.incrementAndGet();
                    }
                };
        try (FetchResponse response = new FetchResponse(200, body)) {
            try (InputStream in = response.getBody()) {
                assertEquals(-1, in.read());
            }
        }
        assertEquals(1, closeCount.get());
    }
}
