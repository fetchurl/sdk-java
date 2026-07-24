package io.github.fetchurl;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void exposesStatusAndBody() {
        InputStream body = new ByteArrayInputStream(new byte[] {1, 2, 3});
        FetchResponse response = new FetchResponse(204, body);
        assertEquals(204, response.getStatusCode());
        assertEquals(body, response.getBody());
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
}
