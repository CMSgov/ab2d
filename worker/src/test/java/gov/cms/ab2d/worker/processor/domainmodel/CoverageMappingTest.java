package gov.cms.ab2d.worker.processor.domainmodel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoverageMappingTest {


    @DisplayName("coverage mapping constructor works")
    @Test
    void build() {
        CoverageMapping coverageMapping = new CoverageMapping(null);

        assertNull(coverageMapping.getLastAttempt());

        coverageMapping.addLog("log1");
        coverageMapping.addLog("log2");

        assertEquals(2, coverageMapping.getLogs().size());
        assertEquals("log2", coverageMapping.getLastLog());

        coverageMapping.completed("successfully");

        assertTrue(coverageMapping.isSuccessful());
        assertEquals(1, coverageMapping.getAttempts());
        assertEquals("successfully", coverageMapping.getLastLog());

        coverageMapping.failed("failed");
        assertEquals(2, coverageMapping.getAttempts());
        assertFalse(coverageMapping.isSuccessful());
        assertEquals("failed", coverageMapping.getLastLog());
    }

}
