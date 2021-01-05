package gov.cms.ab2d.common.health;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MemoryUtilizationTest {
    @Test
    void testMemory() {
        assertFalse(MemoryUtilization.outOfMemory(2));
        assertTrue(MemoryUtilization.outOfMemory((int) (Math.pow(2, 31) - 1)));
    }
}