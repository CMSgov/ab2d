package gov.cms.ab2d.filter;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExplanationOfBenefitTrimmerR4V3InPlaceTest {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();

    @Test
    void nullInputReturnsNull() {
        assertNull(ExplanationOfBenefitTrimmerR4V3.getBenefitInPlace(null));
    }

    @Test
    void returnsSameInstance() {
        IBaseResource eob = EOBLoadUtilities.getR4EOBFromFileInClassPath("eobdata/EOB-for-Inpatient-R4V3.json");
        assertSame(eob, ExplanationOfBenefitTrimmerR4V3.getBenefitInPlace(eob));
    }

    @ParameterizedTest(name = "inPlace json equals legacy: {0}")
    @ValueSource(strings = {
            "eobdata/EOB-for-Carrier-R4.json",
            "eobdata/EOB-for-DME-R4.json",
            "eobdata/EOB-for-HHA-R4.json",
            "eobdata/EOB-for-Hospice-R4.json",
            "eobdata/EOB-for-Inpatient-R4V3.json",
            "eobdata/EOB-for-Outpatient-R4.json",
            "eobdata/EOB-for-SNF-R4.json"
    })
    void jsonEqualsLegacyOutput(String resourcePath) {
        IBaseResource eobForLegacy = EOBLoadUtilities.getR4EOBFromFileInClassPath(resourcePath);
        IBaseResource eobForInPlace = EOBLoadUtilities.getR4EOBFromFileInClassPath(resourcePath);

        IBaseResource legacyResult = ExplanationOfBenefitTrimmerR4V3.getBenefit(eobForLegacy);
        IBaseResource inPlaceResult = ExplanationOfBenefitTrimmerR4V3.getBenefitInPlace(eobForInPlace);

        String legacyJson = FHIR_CONTEXT.newJsonParser().encodeResourceToString(legacyResult);
        String inPlaceJson = FHIR_CONTEXT.newJsonParser().encodeResourceToString(inPlaceResult);
        assertEquals(legacyJson, inPlaceJson, "JSON output differs for " + resourcePath);
    }

    @Test
    void droppedFieldsAreCleared() {
        ExplanationOfBenefit eob = buildFullEob();
        ExplanationOfBenefit result = (ExplanationOfBenefit) ExplanationOfBenefitTrimmerR4V3.getBenefitInPlace(eob);

        assertAll("dropped fields",
                () -> assertFalse(result.hasPrecedence()),
                () -> assertNull(result.getDisposition()),
                () -> assertFalse(result.hasPayee()),
                () -> assertNull(result.getAccident().getDate()),
                () -> assertNull(result.getPayment().getDate()),
                () -> assertNull(result.getForm().getCreation()),
                () -> assertNull(result.getPriority().getText()),
                () -> assertNull(result.getFundsReserveRequested().getText()),
                () -> assertNull(result.getFundsReserve().getText()),
                () -> assertNull(result.getFormCode().getText()),
                () -> assertNull(result.getBenefitPeriod().getStart()),
                () -> assertEquals(0, result.getPreAuthRef().size()),
                () -> assertEquals(0, result.getPreAuthRefPeriod().size()),
                () -> assertEquals(0, result.getRelated().size()),
                () -> assertEquals(0, result.getAddItem().size()),
                () -> assertEquals(0, result.getProcessNote().size()),
                () -> assertEquals(0, result.getBenefitBalance().size()),
                () -> assertEquals(0, result.getAdjudication().size()),
                () -> assertEquals(0, result.getTotal().size()),
                () -> assertEquals(0, result.getExtension().size()),
                () -> assertEquals(0, result.getModifierExtension().size())
        );
    }

    @Test
    void keptFieldsArePreserved() {
        ExplanationOfBenefit eob = buildFullEob();
        ExplanationOfBenefit result = (ExplanationOfBenefit) ExplanationOfBenefitTrimmerR4V3.getBenefitInPlace(eob);

        assertAll("kept fields",
                () -> assertNotNull(result.getMeta()),
                () -> assertNotNull(result.getType()),
                () -> assertNotNull(result.getText()),
                () -> assertNotNull(result.getSubType()),
                () -> assertEquals(1, result.getIdentifier().size()),
                () -> assertNotNull(result.getPatient()),
                () -> assertNotNull(result.getFacility()),
                () -> assertNotNull(result.getProvider()),
                () -> assertNotNull(result.getBillablePeriod()),
                () -> assertEquals(ExplanationOfBenefit.Use.CLAIM, result.getUse()),
                () -> assertEquals(ExplanationOfBenefit.RemittanceOutcome.COMPLETE, result.getOutcome()),
                () -> assertEquals(ExplanationOfBenefit.ExplanationOfBenefitStatus.CANCELLED, result.getStatus()),
                () -> assertEquals(1, result.getDiagnosis().size()),
                () -> assertEquals(1, result.getProcedure().size()),
                () -> assertEquals(1, result.getItem().size())
        );
    }

    @Test
    void insuranceKeptWithFocalAndCoverageOnly() {
        ExplanationOfBenefit eob = buildFullEob();
        ExplanationOfBenefit result = (ExplanationOfBenefit) ExplanationOfBenefitTrimmerR4V3.getBenefitInPlace(eob);

        assertEquals(1, result.getInsurance().size());
        ExplanationOfBenefit.InsuranceComponent ins = result.getInsuranceFirstRep();
        assertEquals("coverage", ins.getCoverage().getReference());
        assertTrue(ins.getFocal());
    }

    @Test
    void careTeamFilteredToRoleCodes() {
        ExplanationOfBenefit eob = buildFullEob();
        ExplanationOfBenefit result = (ExplanationOfBenefit) ExplanationOfBenefitTrimmerR4V3.getBenefitInPlace(eob);

        List<String> roles = result.getCareTeam().stream()
                .map(ct -> ct.getRole().getCodingFirstRep().getCode())
                .toList();
        assertTrue(roles.stream().allMatch(r -> List.of("attending", "referring", "operating", "otheroperating", "rendering").contains(r)));
    }

    @Test
    void supportingInfoDrgKept() {
        ExplanationOfBenefit eob = buildFullEob();
        ExplanationOfBenefit result = (ExplanationOfBenefit) ExplanationOfBenefitTrimmerR4V3.getBenefitInPlace(eob);

        assertEquals(1, result.getSupportingInfo().size());
        ExplanationOfBenefit.SupportingInformationComponent si = result.getSupportingInfoFirstRep();
        assertEquals("http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBSupportingInfoType",
                si.getCategory().getCodingFirstRep().getSystem());
        assertEquals("drg", si.getCategory().getCodingFirstRep().getCode());
    }

    @Test
    void itemComponentCleaned() {
        ExplanationOfBenefit eob = buildFullEob();
        ExplanationOfBenefit result = (ExplanationOfBenefit) ExplanationOfBenefitTrimmerR4V3.getBenefitInPlace(eob);

        ExplanationOfBenefit.ItemComponent item = result.getItem().get(0);
        assertAll("item cleaned",
                () -> assertNull(item.getCategory().getText()),
                () -> assertEquals(3, item.getSequence()),
                () -> assertEquals(1, item.getCareTeamSequence().get(0).getValue()),
                () -> assertEquals(1, item.getDiagnosisSequence().get(0).getValue()),
                () -> assertEquals(3, item.getProcedureSequence().get(0).getValue())
        );
    }

    @Test
    void removableElementsAreTheComplementOfTheAllowlist() {
        // The allowlist is the source of truth. Every element the resource declares must be either
        // kept (allowlisted) or removable — nothing may fall through the cracks, and the two sets
        // must be disjoint. This proves the trimmer is allowlist-driven rather than denylist-driven.
        List<String> declared = new ExplanationOfBenefit().children().stream()
                .map(Property::getName)
                .toList();

        for (String name : declared) {
            boolean kept = ExplanationOfBenefitTrimmerR4V3.KEPT_ELEMENTS.contains(name);
            boolean removable = ExplanationOfBenefitTrimmerR4V3.REMOVABLE_ELEMENTS.contains(name);
            assertTrue(kept ^ removable,
                    "Element '" + name + "' must be exactly one of kept/removable (kept=" + kept
                            + ", removable=" + removable + ")");
        }

        assertTrue(ExplanationOfBenefitTrimmerR4V3.REMOVABLE_ELEMENTS.stream()
                        .noneMatch(ExplanationOfBenefitTrimmerR4V3.KEPT_ELEMENTS::contains),
                "removable set must not intersect the allowlist");
    }

    @Test
    void elementNotInAllowlistIsStrippedInPlace() {
        // A field that is NOT in KEPT_ELEMENTS must be removed even though getBenefitInPlace never
        // names it explicitly — it is dropped purely because it is absent from the allowlist.
        // This is the "new field is excluded by default" / opt-in guarantee from the ticket.
        String dropped = "disposition";
        assertFalse(ExplanationOfBenefitTrimmerR4V3.KEPT_ELEMENTS.contains(dropped),
                "precondition: '" + dropped + "' must not be allowlisted");

        ExplanationOfBenefit eob = buildFullEob();
        eob.setDisposition("should-be-removed");
        assertTrue(eob.hasDisposition());

        ExplanationOfBenefit result =
                (ExplanationOfBenefit) ExplanationOfBenefitTrimmerR4V3.getBenefitInPlace(eob);

        assertFalse(result.hasDisposition(), "non-allowlisted element should be stripped in place");
    }

    private static ExplanationOfBenefit buildFullEob() {
        ExplanationOfBenefit eob = new ExplanationOfBenefit();
        eob.setText(new Narrative().setStatus(Narrative.NarrativeStatus.ADDITIONAL));
        eob.setMeta(new Meta().setLastUpdated(new java.util.Date()));
        eob.setPatient(new Reference("Patient/1234"));
        eob.setFacility(new Reference("Facility/1"));
        eob.setProvider(new Reference("Provider/1"));
        eob.setCreated(new java.util.Date());
        eob.setDisposition("D");
        eob.setPrecedence(2);
        eob.setPriority(new CodeableConcept().setText("p"));
        eob.setFundsReserveRequested(new CodeableConcept().setText("frr"));
        eob.setFundsReserve(new CodeableConcept().setText("fr"));
        eob.setFormCode(new CodeableConcept().setText("fc"));
        eob.setBenefitPeriod(new Period().setStart(new java.util.Date()));
        eob.setPayee(new ExplanationOfBenefit.PayeeComponent().setParty(new Reference("party")));
        eob.setAccident(new ExplanationOfBenefit.AccidentComponent().setDate(new java.util.Date()));
        eob.setPayment(new ExplanationOfBenefit.PaymentComponent().setDate(new java.util.Date()));
        eob.setForm(new Attachment().setCreation(new java.util.Date()));
        eob.setPreAuthRef(List.of(new StringType("preauth")));
        eob.setPreAuthRefPeriod(List.of(new Period()));
        eob.setRelated(List.of(new ExplanationOfBenefit.RelatedClaimComponent()));
        eob.setAddItem(List.of(new ExplanationOfBenefit.AddedItemComponent()));
        eob.setProcessNote(List.of(new ExplanationOfBenefit.NoteComponent()));
        eob.setBenefitBalance(List.of(new ExplanationOfBenefit.BenefitBalanceComponent()));
        eob.setTotal(List.of(new ExplanationOfBenefit.TotalComponent()));
        eob.setAdjudication(List.of(new ExplanationOfBenefit.AdjudicationComponent()));
        eob.setUse(ExplanationOfBenefit.Use.CLAIM);
        eob.setOutcome(ExplanationOfBenefit.RemittanceOutcome.COMPLETE);
        eob.setStatus(ExplanationOfBenefit.ExplanationOfBenefitStatus.CANCELLED);
        eob.setType(new CodeableConcept().setText("type"));
        eob.setSubType(new CodeableConcept().setText("subtype"));
        eob.setBillablePeriod(new Period().setStart(new java.util.Date()));
        // Mutable lists mirror real parsed EOBs; getBenefitInPlace prunes these in place
        eob.setInsurance(new ArrayList<>(List.of(
                new ExplanationOfBenefit.InsuranceComponent()
                        .setCoverage(new Reference("coverage"))
                        .setFocal(true))));
        eob.setIdentifier(List.of(new Identifier().setValue("id1")));
        eob.setDiagnosis(List.of(new ExplanationOfBenefit.DiagnosisComponent().setSequence(1)));
        eob.setProcedure(List.of(new ExplanationOfBenefit.ProcedureComponent().setSequence(1)));

        String drgSystem = "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBSupportingInfoType";
        String msDrgSystem = "https://www.cms.gov/Medicare/Medicare-Fee-for-Service-Payment/AcuteInpatientPPS/MS-DRG-Classifications-and-Software";
        eob.setSupportingInfo(new ArrayList<>(List.of(
                new ExplanationOfBenefit.SupportingInformationComponent()
                        .setCategory(new CodeableConcept().addCoding(new Coding().setSystem(drgSystem).setCode("drg")))
                        .setCode(new CodeableConcept().addCoding(new Coding().setSystem(msDrgSystem).setCode("123"))))));

        ExplanationOfBenefit.ItemComponent item = new ExplanationOfBenefit.ItemComponent()
                .setSequence(3)
                .setCategory(new CodeableConcept().setText("category"))
                .setDiagnosisSequence(List.of(new PositiveIntType(1)))
                .setCareTeamSequence(List.of(new PositiveIntType(1)))
                .setProcedureSequence(List.of(new PositiveIntType(3)));
        eob.setItem(List.of(item));

        Practitioner attending = new Practitioner();
        attending.setId("careteam-attending");
        attending.addIdentifier().setSystem("http://hl7.org/fhir/sid/us-npi").setValue("npi-1");

        eob.setContained(new ArrayList<>(List.of(attending)));
        eob.setCareTeam(new ArrayList<>(List.of(
                new ExplanationOfBenefit.CareTeamComponent()
                        .setSequence(1)
                        .setRole(new CodeableConcept().addCoding(
                                new Coding().setSystem("http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole").setCode("attending")))
                        .setProvider(new Reference("#careteam-attending")))));

        return eob;
    }
}
