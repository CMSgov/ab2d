package gov.cms.ab2d.common.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class FHIRUtilTest {

    @Test
    void testGetErrorOutcome() {
        final String errText = "SOMETHING BROKE";
        final org.hl7.fhir.dstu3.model.OperationOutcome oo = FHIRUtil.getErrorOutcome(errText);
        assertTrue(oo instanceof  org.hl7.fhir.dstu3.model.OperationOutcome);
        assertEquals(org.hl7.fhir.dstu3.model.ResourceType.OperationOutcome, oo.getResourceType());
        assertEquals(1, oo.getIssue().size());
        assertEquals(errText, oo.getIssue().get(0).getDetails().getText());
    }

    @Test
    void testOutcomeToJSON() {
        final String errText = "SOMETHING BROKE";
        final org.hl7.fhir.dstu3.model.OperationOutcome oo = FHIRUtil.getErrorOutcome(errText);
        final String payload = FHIRUtil.outcomeToJSON(oo);
        assertNotNull(payload);
    }
}
