package gov.cms.ab2d.common.util;

import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class FHIRUtilTest {

    @Test
    void testGetErrorOutcome() {
        final String errText = "SOMETHING BROKE";
        final OperationOutcome oo = FHIRUtil.getErrorOutcome(errText);
        assertTrue(oo instanceof  OperationOutcome);
        assertThat(oo.getResourceType(), is(ResourceType.OperationOutcome));
        assertThat(oo.getIssue().size(), is(1));
        assertThat(oo.getIssue().get(0).getDetails().getText(), is(errText));
    }

    @Test
    void testOutcomeToJSON() {
        final String errText = "SOMETHING BROKE";
        final OperationOutcome oo = FHIRUtil.getErrorOutcome(errText);
        final String payload = FHIRUtil.outcomeToJSON(oo);
        assertThat(payload, notNullValue());
    }
}
