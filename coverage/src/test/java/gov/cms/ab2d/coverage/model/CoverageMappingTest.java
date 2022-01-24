package gov.cms.ab2d.coverage.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

class CoverageMappingTest {


    @DisplayName("coverage mapping constructor works")
    @Test
    void build() {
        CoverageMapping coverageMapping = new CoverageMapping(null, new CoverageSearchDTO());

        coverageMapping.completed();

        assertTrue(coverageMapping.isSuccessful());
        assertEquals(1, coverageMapping.getCoverageSearchDTO().getAttempts());

        coverageMapping.failed();
        assertEquals(2, coverageMapping.getCoverageSearchDTO().getAttempts());
        assertFalse(coverageMapping.isSuccessful());
    }

}
