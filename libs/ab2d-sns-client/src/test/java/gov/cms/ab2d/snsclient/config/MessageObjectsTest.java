package gov.cms.ab2d.snsclient.config;

import gov.cms.ab2d.snsclient.messages.CoverageCountDTO;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;

import static gov.cms.ab2d.snsclient.messages.AB2DServices.AB2D;
import static gov.cms.ab2d.snsclient.messages.AB2DServices.BFD;
import static gov.cms.ab2d.snsclient.messages.AB2DServices.HPMS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class MessageObjectsTest {

    @Test
    void coverageCountDTO() {
        Timestamp time = Timestamp.from(Instant.now());
        CoverageCountDTO coverage = new CoverageCountDTO();
        coverage.setContractNumber("test");
        coverage.setService("test");
        coverage.setYear(1);
        coverage.setCount(1);
        coverage.setMonth(1);
        coverage.setCountedAt(time);
        CoverageCountDTO coverageConst = new CoverageCountDTO("test", "test", 1, 1, 1, time);
        assertEquals(coverage.getContractNumber(), coverageConst.getContractNumber());
        assertEquals(coverage.getService(), coverageConst.getService());
        assertEquals(coverage.getCount(), coverageConst.getCount());
        assertEquals(coverage.getYear(), coverageConst.getYear());
        assertEquals(coverage.getMonth(), coverageConst.getMonth());
        assertEquals(coverage.getCountedAt(), coverageConst.getCountedAt());
        assertNotEquals("", coverage.toString());
    }

    @Test
    void aB2DServices() {
        assertEquals("AB2D", AB2D.toString());
        assertEquals("BFD", BFD.toString());
        assertEquals("HPMS", HPMS.toString());

    }
}
