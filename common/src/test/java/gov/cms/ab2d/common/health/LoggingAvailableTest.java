package gov.cms.ab2d.common.health;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class LoggingAvailableTest {

    @Mock
    org.slf4j.Logger mockLogger;

    @Test
    void canLog() {
        assertTrue(LoggingAvailable.canLog());
    }

    @Test
    void canNotLog() {
        when(mockLogger.isErrorEnabled()).thenReturn(false);
        assertFalse(LoggingAvailable.canLog(mockLogger));
    }
}
