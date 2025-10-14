package gov.cms.ab2d.snsclient.config;

import gov.cms.ab2d.snsclient.exception.SNSClientException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class SNSClientExceptionTest {

    @Test
    void messageOnlyConstructor() {
        SNSClientException ex = new SNSClientException("SNS error");
        assertEquals("SNS error", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void messageAndCauseConstructor() {
        Throwable cause = new IllegalStateException("bad state");
        SNSClientException ex = new SNSClientException("SNS failure", cause);
        assertEquals("SNS failure", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    void thrownAndCaught() {
        Throwable cause = new IOException("IO fail");
        SNSClientException thrown = assertThrows(
                SNSClientException.class,
                () -> {
                    throw new SNSClientException("SNS send failed", cause);
                }
        );
        assertEquals("SNS send failed", thrown.getMessage());
        assertSame(cause, thrown.getCause());
    }

    @Test
    void allowsNulls() {
        SNSClientException ex1 = new SNSClientException(null);
        assertNull(ex1.getMessage());
        assertNull(ex1.getCause());

        SNSClientException ex2 = new SNSClientException(null, null);
        assertNull(ex2.getMessage());
        assertNull(ex2.getCause());
    }
}
