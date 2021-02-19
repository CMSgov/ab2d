package gov.cms.ab2d.fhir;

import org.junit.jupiter.api.Test;

import static gov.cms.ab2d.fhir.Versions.FhirVersions.R4;
import static gov.cms.ab2d.fhir.Versions.FhirVersions.STU3;
import static org.junit.jupiter.api.Assertions.*;

class SearchUtilsTest {
    @Test
    void testGetPatientClass() {
        assertEquals(org.hl7.fhir.dstu3.model.Patient.class, SearchUtils.getPatientClass(STU3));
        assertEquals(org.hl7.fhir.r4.model.Patient.class, SearchUtils.getPatientClass(R4));
        assertNull(SearchUtils.getPatientClass(null));
    }

    @Test
    void testGetBundleClass() {
        assertEquals(org.hl7.fhir.dstu3.model.Bundle.class, SearchUtils.getBundleClass(STU3));
        assertEquals(org.hl7.fhir.r4.model.Bundle.class, SearchUtils.getBundleClass(R4));
        assertNull(SearchUtils.getBundleClass(null));
    }
}