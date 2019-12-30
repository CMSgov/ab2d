package gov.cms.ab2d.filter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExplanationOfBenefitsTrimmerTest {
    private static ExplanationOfBenefit eobCarrier = null;
    private static FhirContext context = FhirContext.forDstu3();

    static {
        eobCarrier = ExplanationOfBenefitsTrimmer.getBenefit(EOBLoadUtilities.getEOBFromFileInClassPath("eobdata/EOB-for-Carrier-Claims.json", context));
    }

    @Test
    public void testEmptyList() {
        ExplanationOfBenefitsTrimmer.clearOutList(null);
        List<Integer> list = new ArrayList<>();
        ExplanationOfBenefitsTrimmer.clearOutList(list);
        assertTrue(list.isEmpty());
        list.add(5);
        assertFalse(list.isEmpty());
        ExplanationOfBenefitsTrimmer.clearOutList(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void validateEmpty() {
        assertNull(ExplanationOfBenefitsTrimmer.getBenefit(null));
        // Since getting a patient target creates a new one, make sure the object is empty
        assertTrue(eobCarrier.getPatientTarget().getIdentifier().isEmpty());
        assertNull(eobCarrier.getPatientTarget().getId());
        assertFalse(eobCarrier.getPatientTarget().getActive());
        assertNull(eobCarrier.getPatientTarget().getBirthDate());
        assertNull(eobCarrier.getBillablePeriod().getStart());
        assertNull(eobCarrier.getBillablePeriod().getEnd());
        assertNull(eobCarrier.getCreated());
        assertTrue(StringUtils.isBlank(eobCarrier.getEnterer().getReference()));
        assertTrue(eobCarrier.getEntererTarget().getName().isEmpty());
        assertNull(eobCarrier.getInsurer().getId());
        assertNull(eobCarrier.getInsurerTarget().getId());
        assertNull(eobCarrier.getProviderTarget().getId());
        assertNull(eobCarrier.getOrganizationTarget().getId());
        assertNull(eobCarrier.getReferral().getId());
        assertNull(eobCarrier.getReferralTarget().getId());
        assertNull(eobCarrier.getFacilityTarget().getId());
        assertNull(eobCarrier.getClaim().getId());
        assertNull(eobCarrier.getClaimTarget().getId());
        assertNull(eobCarrier.getClaimResponse().getId());
        assertNull(eobCarrier.getClaimResponseTarget().getId());
        assertNull(eobCarrier.getOutcome().getId());
        assertNull(eobCarrier.getDisposition());
        assertTrue(isNullOrEmpty(eobCarrier.getRelated()));
        assertNull(eobCarrier.getPrescription().getId());
        assertNull(eobCarrier.getPrescriptionTarget());
        assertNull(eobCarrier.getOriginalPrescription().getId());
        assertNull(eobCarrier.getOriginalPrescriptionTarget().getId());
        assertNull(eobCarrier.getPayee().getId());
        assertTrue(isNullOrEmpty(eobCarrier.getInformation()));
        assertEquals(eobCarrier.getPrecedence(), 0);
        assertNull(eobCarrier.getInsurance().getId());
        assertNull(eobCarrier.getAccident().getId());
        assertNull(eobCarrier.getEmploymentImpacted().getId());
        assertNull(eobCarrier.getHospitalization().getId());
        assertTrue(isNullOrEmpty(eobCarrier.getAddItem()));
        assertNull(eobCarrier.getTotalCost().getId());
        assertNull(eobCarrier.getUnallocDeductable().getId());
        assertNull(eobCarrier.getTotalBenefit().getId());
        assertNull(eobCarrier.getPayment().getId());
        assertNull(eobCarrier.getForm().getId());
        assertTrue(isNullOrEmpty(eobCarrier.getProcessNote()));
        assertTrue(isNullOrEmpty(eobCarrier.getBenefitBalance()));
    }

    @Test
    public void testItemValues() {
        if (eobCarrier.getItem() != null) {
            for (var item : eobCarrier.getItem()) {
                assertTrue(isNullOrEmpty(item.getDiagnosisLinkId()));
                assertTrue(isNullOrEmpty(item.getProcedureLinkId()));
                assertTrue(isNullOrEmpty(item.getInformationLinkId()));
                assertNull(item.getRevenue().getId());
                assertNull(item.getCategory().getId());
                assertTrue(isNullOrEmpty(item.getModifier()));
                assertTrue(isNullOrEmpty(item.getProgramCode()));
                assertNull(item.getUnitPrice().getId());
                assertNull(item.getFactor());
                assertNull(item.getNet().getId());
                assertTrue(isNullOrEmpty(item.getUdi()));
                assertTrue(isNullOrEmpty(item.getUdiTarget()));
                assertNull(item.getBodySite().getId());
                assertTrue(isNullOrEmpty(item.getSubSite()));
                assertTrue(isNullOrEmpty(item.getEncounter()));
                assertTrue(isNullOrEmpty(item.getEncounterTarget()));
                assertTrue(isNullOrEmpty(item.getNoteNumber()));
                assertTrue(isNullOrEmpty(item.getAdjudication()));
                assertTrue(isNullOrEmpty(item.getDetail()));
            }
        }
    }

    private boolean isNullOrEmpty(List<?> items) {
        return items == null || items.isEmpty();
    }

    private void printItOut(String file) {
        IParser jsonParser = context.newJsonParser().setPrettyPrint(true);

        ExplanationOfBenefit eCarrier = ExplanationOfBenefitsTrimmer.getBenefit(
                EOBLoadUtilities.getEOBFromFileInClassPath(file, context));

        String result = jsonParser.encodeResourceToString(eCarrier);
        System.out.println(result);
    }

    @Test
    void demo1() {
        printItOut("eobdata/EOB-for-Carrier-Claims.json");
    }

    @Test
    void isPartD() {
        ExplanationOfBenefit ePartD = ExplanationOfBenefitsTrimmer.getBenefit(
                EOBLoadUtilities.getEOBFromFileInClassPath("eobdata/EOB-for-Part-D-Claims.json", context));
        assertTrue(EOBLoadUtilities.isPartD(ePartD));
        assertFalse(EOBLoadUtilities.isPartD(eobCarrier));
        assertFalse(EOBLoadUtilities.isPartD(null));
    }
}