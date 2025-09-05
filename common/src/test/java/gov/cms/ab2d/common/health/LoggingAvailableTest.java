package gov.cms.ab2d.common.health;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

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
