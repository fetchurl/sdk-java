package io.github.fetchurl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class FetchResponseTest {
    private static InputStream countingBody(AtomicInteger closeCount) {
        return new InputStream() {
            @Override
            public int read() {
                return -1;
            }

            @Override
            public void close() {
                closeCount.incrementAndGet();
            }
        };
    }

    private static InputStream closeOnceBody(AtomicInteger closeCount) {
        return new InputStream() {
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
    }

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
        AtomicInteger closeCount = new AtomicInteger();
        FetchResponse response = new FetchResponse(200, countingBody(closeCount));
        response.close();
        assertEquals(1, closeCount.get());
    }

    @Test
    void tryWithResourcesClosesBody() throws IOException {
        AtomicInteger closeCount = new AtomicInteger();
        try (FetchResponse response = new FetchResponse(404, countingBody(closeCount))) {
            assertEquals(404, response.getStatusCode());
        }
        assertEquals(1, closeCount.get());
    }

    @Test
    void closeIsIdempotent() throws IOException {
        AtomicInteger closeCount = new AtomicInteger();
        FetchResponse response = new FetchResponse(200, countingBody(closeCount));
        response.close();
        response.close();
        assertEquals(1, closeCount.get());
    }

    @Test
    void closingBodyStreamClosesResponseOnce() throws IOException {
        AtomicInteger closeCount = new AtomicInteger();
        FetchResponse response = new FetchResponse(200, countingBody(closeCount));
        response.getBody().close();
        response.close();
        assertEquals(1, closeCount.get());
    }

    @Test
    void nestedTryWithResourcesClosesUnderlyingOnce() throws IOException {
        AtomicInteger closeCount = new AtomicInteger();
        try (FetchResponse response = new FetchResponse(200, closeOnceBody(closeCount))) {
            try (InputStream in = response.getBody()) {
                assertEquals(-1, in.read());
            }
        }
        assertEquals(1, closeCount.get());
    }
}
