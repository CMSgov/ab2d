package gov.cms.ab2d.common.util;

import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class FHIRUtilTest {

    @Test
    void testGetErrorOutcome() {
        final String errText = "SOMETHING BROKE";
        final OperationOutcome oo = FHIRUtil.getErrorOutcome(errText);
        assertTrue(oo instanceof  OperationOutcome);
        assertEquals(ResourceType.OperationOutcome, oo.getResourceType());
        assertEquals(1, oo.getIssue().size());
        assertEquals(errText, oo.getIssue().get(0).getDetails().getText());
    }

    @Test
    void testOutcomeToJSON() {
        final String errText = "SOMETHING BROKE";
        final OperationOutcome oo = FHIRUtil.getErrorOutcome(errText);
        final String payload = FHIRUtil.outcomeToJSON(oo);
        assertNotNull(payload);
    }
}
