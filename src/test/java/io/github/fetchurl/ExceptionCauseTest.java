package io.github.fetchurl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class ExceptionCauseTest {
    @Test
    void partialWriteCauseAccessorsMatch() {
        IOException cause = new IOException("disk full");
        PartialWriteException ex = new PartialWriteException(cause);
        assertSame(cause, ex.getCause());
        assertSame(cause, ex.getCauseError());
        assertTrue(ex.getMessage().contains("disk full"));
    }

    @Test
    void partialWriteNullCause() {
        PartialWriteException ex = new PartialWriteException(null);
        assertNull(ex.getCause());
        assertNull(ex.getCauseError());
        assertEquals("partial write: unknown", ex.getMessage());
    }

    @Test
    void partialWriteNullMessageUsesToString() {
        Throwable cause = new Throwable() {
            @Override
            public String getMessage() {
                return null;
            }

            @Override
            public String toString() {
                return "anonymous-failure";
            }
        };
        PartialWriteException ex = new PartialWriteException(cause);
        assertSame(cause, ex.getCauseError());
        assertTrue(ex.getMessage().contains("anonymous-failure"));
    }

    @Test
    void allSourcesFailedCauseAccessorsMatch() {
        IOException cause = new IOException("timeout");
        AllSourcesFailedException ex = new AllSourcesFailedException(cause);
        assertSame(cause, ex.getCause());
        assertSame(cause, ex.getLastError());
        assertEquals("all sources failed", ex.getMessage());
    }

    @Test
    void allSourcesFailedNoArgHasNoCause() {
        AllSourcesFailedException ex = new AllSourcesFailedException();
        assertNull(ex.getCause());
        assertNull(ex.getLastError());
    }
}
