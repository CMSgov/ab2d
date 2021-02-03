package gov.cms.ab2d.fhir;

import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Enumerations;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetaDataUtilsTest {
    @Test
    void testValid() {
        CapabilityStatement statement = new CapabilityStatement();
        assertFalse(MetaDataUtils.metaDataValid(statement, Versions.FhirVersions.R4));
        statement.setStatus(Enumerations.PublicationStatus.ACTIVE);
        assertTrue(MetaDataUtils.metaDataValid(statement, Versions.FhirVersions.R4));
        statement.setStatus(Enumerations.PublicationStatus.UNKNOWN);
        assertFalse(MetaDataUtils.metaDataValid(statement, Versions.FhirVersions.R4));
        assertFalse(MetaDataUtils.metaDataValid(null, Versions.FhirVersions.R4));
        assertFalse(MetaDataUtils.metaDataValid(null, Versions.FhirVersions.R4));
        assertFalse(MetaDataUtils.metaDataValid(null, null));
    }

    @Test
    void testClass() {
        assertEquals(CapabilityStatement.class, MetaDataUtils.getCapabilityClass(Versions.FhirVersions.R4));
    }
}