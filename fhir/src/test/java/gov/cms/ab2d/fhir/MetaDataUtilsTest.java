package gov.cms.ab2d.fhir;

import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Enumerations;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetaDataUtilsTest {
    @Test
    void testValid() {
        CapabilityStatement statement = new CapabilityStatement();
        assertFalse(MetaDataUtils.metaDataValid(statement, Versions.FHIR_VERSIONS.R4));
        statement.setStatus(Enumerations.PublicationStatus.ACTIVE);
        assertTrue(MetaDataUtils.metaDataValid(statement, Versions.FHIR_VERSIONS.R4));
        statement.setStatus(Enumerations.PublicationStatus.UNKNOWN);
        assertFalse(MetaDataUtils.metaDataValid(statement, Versions.FHIR_VERSIONS.R4));
        assertFalse(MetaDataUtils.metaDataValid(null, Versions.FHIR_VERSIONS.R4));
        assertFalse(MetaDataUtils.metaDataValid(null, Versions.FHIR_VERSIONS.R4));
        assertFalse(MetaDataUtils.metaDataValid(null, null));
    }

    @Test
    void testClass() {
        assertEquals(CapabilityStatement.class, MetaDataUtils.getCapabilityClass(Versions.FHIR_VERSIONS.R4));
    }
}