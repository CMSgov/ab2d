package gov.cms.ab2d.common.health;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MemoryUtilizationTest {

    public static final int ALLOCATABLE_MEMORY_MB = 2;
    public static final int UNALLOCATABLE_MEMORY_MB = (int) (Math.pow(2, 31) - 1);

    @Test
    void testMemory() {
        assertFalse(MemoryUtilization.outOfMemory(ALLOCATABLE_MEMORY_MB));
        assertTrue(MemoryUtilization.outOfMemory(UNALLOCATABLE_MEMORY_MB));
    }
}
