package gov.cms.ab2d.fhir;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.Test;

import static gov.cms.ab2d.fhir.Versions.FhirVersions.R4;
import static gov.cms.ab2d.fhir.Versions.FhirVersions.STU3;
import static org.junit.jupiter.api.Assertions.*;

class FHIRUtilTest {
    @Test
    void testGetErrorOutcome() {
        final String errText = "SOMETHING BROKE";
        final IBaseResource o = FHIRUtil.getErrorOutcome(errText, R4);
        org.hl7.fhir.r4.model.OperationOutcome oo = (org.hl7.fhir.r4.model.OperationOutcome) o;
        assertTrue(oo instanceof  org.hl7.fhir.r4.model.OperationOutcome);
        assertEquals(org.hl7.fhir.r4.model.ResourceType.OperationOutcome, oo.getResourceType());
        assertEquals(1, oo.getIssue().size());
        assertEquals(errText, oo.getIssue().get(0).getDetails().getText());
    }

    @Test
    void testOutcomeToJSON() {
        final String errText = "SOMETHING BROKE";
        final IBaseResource oo = FHIRUtil.getErrorOutcome(errText, STU3);
        final String payload = FHIRUtil.outcomeToJSON(oo, STU3);
        assertNotNull(payload);
    }
}