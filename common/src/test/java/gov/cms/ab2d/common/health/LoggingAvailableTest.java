package gov.cms.ab2d.common.health;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoggingAvailableTest {

    @Test
    void canLog() {
        assertTrue(LoggingAvailable.canLog());
    }
}