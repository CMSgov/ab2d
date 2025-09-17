package gov.cms.ab2d.filter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplanationOfBenefitTrimmerSTU3Test {
    private static IBaseResource eobResource = null;
    private static FhirContext context = FhirContext.forDstu3();

    static {
        eobResource = ExplanationOfBenefitTrimmerSTU3.getBenefit(EOBLoadUtilities.getSTU3EOBFromFileInClassPath("eobdata/EOB-for-Carrier-Claims.json"));
    }

    @Test
    void testEmptyList() {
        ExplanationOfBenefitTrimmerSTU3.clearOutList(null);
        List<Integer> list = new ArrayList<>();
        ExplanationOfBenefitTrimmerSTU3.clearOutList(list);
        assertTrue(list.isEmpty());
        list.add(5);
        assertFalse(list.isEmpty());
        ExplanationOfBenefitTrimmerSTU3.clearOutList(list);
        assertTrue(list.isEmpty());
    }

    @Test
    void validateDefaultEobStartingBillablePeriod() {
        ExplanationOfBenefit eob = (ExplanationOfBenefit) eobResource;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        assertEquals("1999-10-27", sdf.format(eob.getBillablePeriod().getStart()));
        assertEquals("1999-10-27", sdf.format(eob.getBillablePeriod().getEnd()));
    }

    @Test
    void validateDefaultEobClaimEmpty() {
        ExplanationOfBenefit eob = (ExplanationOfBenefit) eobResource;

        assertNull(eob.getClaim().getId());
        assertNull(eob.getClaimTarget().getId());
        assertNull(eob.getClaimResponse().getId());
        assertNull(eob.getClaimResponseTarget().getId());
    }

    @Test
    void validateDefaultEobFieldsEmpty() {
        ExplanationOfBenefit eob = (ExplanationOfBenefit) eobResource;

        assertTrue(isNullOrEmpty(eob.getProcessNote()));
        assertTrue(isNullOrEmpty(eob.getBenefitBalance()));
        assertTrue(isNullOrEmpty(eob.getAddItem()));
        assertTrue(isNullOrEmpty(eob.getInformation()));
        assertTrue(isNullOrEmpty(eob.getRelated()));
        assertTrue(eob.getPatientTarget().isEmpty());
        assertTrue(StringUtils.isBlank(eob.getEnterer().getReference()));
        assertTrue(eob.getEntererTarget().getName().isEmpty());
        assertEquals(0, eob.getPrecedence());
    }

    @Test
    void validateDefaultEobFieldsNull() {
        ExplanationOfBenefit eobCarrier = (ExplanationOfBenefit) eobResource;
        assertNull(ExplanationOfBenefitTrimmerSTU3.getBenefit(null));
        // Since getting a patient target creates a new one, make sure the object is empty

        assertNull(eobCarrier.getCreated());
        assertNull(eobCarrier.getInsurer().getId());
        assertNull(eobCarrier.getInsurerTarget().getId());
        assertNull(eobCarrier.getProviderTarget().getId());
        assertNull(eobCarrier.getOrganizationTarget().getId());
        assertNull(eobCarrier.getReferral().getId());
        assertNull(eobCarrier.getReferralTarget().getId());
        assertNull(eobCarrier.getFacilityTarget().getId());
        assertNull(eobCarrier.getOutcome().getId());
        assertNull(eobCarrier.getDisposition());
        assertNull(eobCarrier.getPrescription().getId());
        assertNull(eobCarrier.getPrescriptionTarget());
        assertNull(eobCarrier.getOriginalPrescription().getId());
        assertNull(eobCarrier.getOriginalPrescriptionTarget().getId());
        assertNull(eobCarrier.getPayee().getId());
        assertNull(eobCarrier.getInsurance().getId());
        assertNull(eobCarrier.getAccident().getId());
        assertNull(eobCarrier.getEmploymentImpacted().getId());
        assertNull(eobCarrier.getHospitalization().getId());
        assertNull(eobCarrier.getTotalCost().getId());
        assertNull(eobCarrier.getUnallocDeductable().getId());
        assertNull(eobCarrier.getTotalBenefit().getId());
        assertNull(eobCarrier.getPayment().getId());
        assertNull(eobCarrier.getForm().getId());
    }

    @Test
    void testItemValues() {
        ExplanationOfBenefit eobCarrier = (ExplanationOfBenefit) eobResource;
        if (eobCarrier.getItem() != null) {
            for (var item : eobCarrier.getItem()) {
                assertFalse(isNullOrEmpty(item.getCareTeamLinkId()));
                assertFalse(isNullOrEmpty(item.getDiagnosisLinkId()));
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

    private String printItOut(String file) {
        IParser jsonParser = context.newJsonParser().setPrettyPrint(true);

        IBaseResource eCarrier = ExplanationOfBenefitTrimmerSTU3.getBenefit(
                EOBLoadUtilities.getSTU3EOBFromFileInClassPath(file));

        String result = jsonParser.encodeResourceToString(eCarrier);
        System.out.println(result);
        return result;
    }

    @Test
    void testPrintItOut() {
        String result = printItOut("eobdata/EOB-for-Carrier-Claims.json");
        assertNotEquals("", result);
    }

    @Test
    void isPartD() {
        IBaseResource ePartD = ExplanationOfBenefitTrimmerSTU3.getBenefit(
                EOBLoadUtilities.getSTU3EOBFromFileInClassPath("eobdata/EOB-for-Part-D-Claims.json"));
        assertTrue(EobUtils.isPartD(ePartD));
        assertFalse(EobUtils.isPartD(eobResource));
        assertFalse(EobUtils.isPartD((ExplanationOfBenefit) null));
    }
}