package gov.cms.ab2d.fhir;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SearchUtilsTest {
    @Test
    void testGetPatientClass() {
        assertEquals(org.hl7.fhir.dstu3.model.Patient.class, SearchUtils.getPatientClass(Versions.FHIR_VERSIONS.R3));
        assertEquals(org.hl7.fhir.r4.model.Patient.class, SearchUtils.getPatientClass(Versions.FHIR_VERSIONS.R4));
        assertNull(SearchUtils.getPatientClass(null));
    }

    @Test
    void testGetBundleClass() {
        assertEquals(org.hl7.fhir.dstu3.model.Bundle.class, SearchUtils.getBundleClass(Versions.FHIR_VERSIONS.R3));
        assertEquals(org.hl7.fhir.r4.model.Bundle.class, SearchUtils.getBundleClass(Versions.FHIR_VERSIONS.R4));
        assertNull(SearchUtils.getBundleClass(null));
    }
}