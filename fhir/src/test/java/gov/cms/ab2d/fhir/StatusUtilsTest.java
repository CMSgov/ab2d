package gov.cms.ab2d.fhir;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class StatusUtilsTest {
    @Test
    void testTimeUtils() {
        OffsetDateTime now = OffsetDateTime.now();
        final org.hl7.fhir.dstu3.model.DateTimeType jobStartedAt = new org.hl7.fhir.dstu3.model.DateTimeType(now.toString());
        String val2 = jobStartedAt.toHumanDisplay();
        String val = StatusUtils.getFhirTime(Versions.FhirVersions.STU3, now);
        assertEquals(val, val2);
    }
}