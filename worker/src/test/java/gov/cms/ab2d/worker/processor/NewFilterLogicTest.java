package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.filter.ExplanationOfBenefitTrimmerSTU3;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.PositiveIntType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NewFilterLogicTest {
    @Test
    void makeSureLinksAreNotFilteredOut() {
        ExplanationOfBenefit eob = (ExplanationOfBenefit) EobTestDataUtil.createEOB();
        ExplanationOfBenefit.ItemComponent item = eob.getItem().get(0);
        item.getProcedureLinkId().add(new PositiveIntType(4));
        ExplanationOfBenefit newEob = (ExplanationOfBenefit) ExplanationOfBenefitTrimmerSTU3.getBenefit(eob);
        ExplanationOfBenefit.ItemComponent newItem = newEob.getItem().get(0);
        assertEquals(2, newItem.getCareTeamLinkId().get(0).getValue());
        assertEquals(4, newItem.getProcedureLinkId().get(0).getValue());
        assertEquals(5, newItem.getDiagnosisLinkId().get(0).getValue());
    }
}
