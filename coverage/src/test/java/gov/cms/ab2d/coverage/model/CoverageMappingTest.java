package gov.cms.ab2d.coverage.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import static org.junit.Assert.*;

class CoverageMappingTest {


    @DisplayName("coverage mapping constructor works")
    @Test
    void build() {
        CoverageMapping coverageMapping = new CoverageMapping(null, new CoverageSearch());

        coverageMapping.completed();

        assertTrue(coverageMapping.isSuccessful());
        assertEquals(1, coverageMapping.getCoverageSearch().getAttempts());

        coverageMapping.failed();
        assertEquals(2, coverageMapping.getCoverageSearch().getAttempts());
        assertFalse(coverageMapping.isSuccessful());
    }

}
