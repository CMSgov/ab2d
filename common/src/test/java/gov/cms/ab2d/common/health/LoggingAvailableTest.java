package gov.cms.ab2d.common.health;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoggingAvailableTest {

    @Mock
    org.slf4j.Logger logger;

    @Test
    void canLog() {
        when(logger.isErrorEnabled()).thenReturn(true);
        assertTrue(LoggingAvailable.canLog(logger));
    }

    @Test
    void canNotLog() {
        when(logger.isErrorEnabled()).thenReturn(false);
        assertFalse(LoggingAvailable.canLog(logger));
    }
}
