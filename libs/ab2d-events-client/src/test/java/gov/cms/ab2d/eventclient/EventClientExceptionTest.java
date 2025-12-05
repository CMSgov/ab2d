package gov.cms.ab2d.eventclient;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class EventClientExceptionTest {

    @Test
    void messageConstructor() {
        EventClientException ex = new EventClientException("test");
        assertEquals("test", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void thrownAndCaught() {
        Throwable cause = new IOException("io");
        EventClientException thrown = assertThrows(
                EventClientException.class,
                () -> {
                    throw new EventClientException("test", cause);
                }
        );
        assertEquals("test", thrown.getMessage());
        assertSame(cause, thrown.getCause());
    }


    @Test
    void allowsNulls() {
        EventClientException ex1 = new EventClientException(null);
        assertNull(ex1.getMessage());
        assertNull(ex1.getCause());

        EventClientException ex2 = new EventClientException(null, null);
        assertNull(ex2.getMessage());
        assertNull(ex2.getCause());
    }
}
