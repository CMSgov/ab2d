package gov.cms.ab2d.coverage.model;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

class CoverageCountTest {


    @Test
    void testCompareToContractNumber() {
        CoverageCount coverageCountTest = new CoverageCount("contractNumber", 2020, 1, 0, 0, 0);

        assertEquals(0,coverageCountTest.compareTo(new CoverageCount("contractNumber", 2020, 1, 0, 0, 0)));
        assertNotEquals(0,coverageCountTest.compareTo(new CoverageCount("contractNumber2", 2020, 1, 0, 0, 0)));

    }

    @Test
    void testCompareToContractYearandMonth() {
        CoverageCount coverageCountTest = new CoverageCount("contractNumber", 2020, 1, 0, 0, 0);

        assertEquals(0, coverageCountTest.compareTo(new CoverageCount("contractNumber", 2020, 1, 1, 1, 1)));

        assertNotEquals(0,coverageCountTest.compareTo(new CoverageCount("contractNumber", 2021, 1, 1, 1, 1)));

        assertNotEquals(0,coverageCountTest.compareTo(new CoverageCount("contractNumber", 2020, 2, 1, 1, 1)));
    }

}
