package gov.cms.ab2d.audit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ExceptionTest {

    @Test
    void stringOnly() {
        assertThrows(AuditException.class, () -> {
            throw new AuditException("test");
        });
    }

    @Test
    void stringAndExceptionOnly() {
        assertThrows(AuditException.class, () -> {
            throw new AuditException("test", new Exception());
        });
    }

    @Test
    void exceptionOnly() {
        assertThrows(AuditException.class, () -> {
            throw new AuditException(new Exception());
        });
    }
}
