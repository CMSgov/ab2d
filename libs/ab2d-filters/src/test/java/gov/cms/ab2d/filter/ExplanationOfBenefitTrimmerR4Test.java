package gov.cms.ab2d.filter;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.ClaimResponse;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Money;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.PositiveIntType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.VisionPrescription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static gov.cms.ab2d.filter.ExplanationOfBenefitTrimmerR4.ANESTHESIA_UNIT_COUNT;
import static gov.cms.ab2d.filter.ExplanationOfBenefitTrimmerR4.NL_RECORD_IDENTIFICATION;
import static gov.cms.ab2d.filter.ExplanationOfBenefitTrimmerR4.PRICING_STATE;
import static gov.cms.ab2d.filter.ExplanationOfBenefitTrimmerR4.SUPPLIER_TYPE;
import static org.hl7.fhir.r4.model.ExplanationOfBenefit.RemittanceOutcome.COMPLETE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplanationOfBenefitTrimmerR4Test {
    private static final ExplanationOfBenefit EOB = new ExplanationOfBenefit();
    private static final Date SAMPLE_DATE = new Date();
    private static final String DUMMY_REF = "1234";
    private static final String DUMMY_TYPE = "type";
    private static final String DUMMY_TXT = "text";

    @BeforeEach
    void initFullEob() {
        // Populate every sub-object of ExplanationOfBenefit
        populate();
    }

    static void populate() {
        EOB.setText(new Narrative().setStatus(Narrative.NarrativeStatus.ADDITIONAL));
        EOB.setContained(List.of(EOB));
        EOB.setMeta(new Meta().setLastUpdated(SAMPLE_DATE));
        EOB.setPatient(new Reference("Patient/1234"));
        EOB.setPatientTarget(new Patient().setBirthDate(SAMPLE_DATE));
        EOB.setCreated(SAMPLE_DATE);
        EOB.setEnterer(new Reference(DUMMY_REF));
        EOB.setEntererTarget(new Patient().setBirthDate(SAMPLE_DATE));
        EOB.setInsurer(new Reference(DUMMY_REF));
        EOB.setInsurerTarget(new Organization().setName("Org"));
        EOB.setProvider(new Reference(DUMMY_REF));
        EOB.setProviderTarget(new Organization().setName("Org"));
        EOB.setReferral(new Reference(DUMMY_REF));
        ServiceRequest req = new ServiceRequest();
        req.setId("Ref");
        EOB.setReferralTarget(req);
        EOB.setFacility(new Reference(DUMMY_REF));
        EOB.setFacilityTarget(new Location().setName("Facility"));
        EOB.setClaim(new Reference(DUMMY_REF));
        EOB.setClaimTarget(new Claim().setCreated(SAMPLE_DATE));
        EOB.setClaimResponse(new Reference(DUMMY_REF));
        EOB.setClaimResponseTarget(new ClaimResponse().setCreated(SAMPLE_DATE));
        EOB.setDisposition("Disposition");
        EOB.setPrescription(new Reference(DUMMY_REF));
        EOB.setPrescriptionTarget(new VisionPrescription().setCreated(SAMPLE_DATE));
        EOB.setOriginalPrescription(new Reference(DUMMY_REF));
        EOB.setOriginalPrescriptionTarget(new MedicationRequest().setAuthoredOn(SAMPLE_DATE));
        EOB.setPrecedence(2);
        EOB.setPriority(new CodeableConcept().setText(DUMMY_TXT));
        EOB.setFundsReserveRequested(new CodeableConcept().setText(DUMMY_TXT));
        EOB.setFundsReserve(new CodeableConcept().setText(DUMMY_TXT));
        EOB.setPreAuthRef(List.of(new StringType("preauth")));
        EOB.setPreAuthRefPeriod(List.of(new Period().setEnd(SAMPLE_DATE).setStart(SAMPLE_DATE)));
        EOB.setFormCode(new CodeableConcept().setText(DUMMY_TXT));
        EOB.setBenefitPeriod(new Period().setEnd(SAMPLE_DATE).setStart(SAMPLE_DATE));
        EOB.setOutcome(COMPLETE);
        EOB.setRelated(List.of(new ExplanationOfBenefit.RelatedClaimComponent().setReference(new Identifier().setValue("one"))));
        EOB.setPayee(new ExplanationOfBenefit.PayeeComponent().setParty(new Reference("party")));
        EOB.setInsurance(List.of(new ExplanationOfBenefit.InsuranceComponent().setCoverage(new Reference("coverage"))));
        EOB.setAccident(new ExplanationOfBenefit.AccidentComponent().setDate(SAMPLE_DATE));
        EOB.setAddItem(List.of(new ExplanationOfBenefit.AddedItemComponent().setUnitPrice(new Money().setValue(10))));
        EOB.setPayment(new ExplanationOfBenefit.PaymentComponent().setDate(SAMPLE_DATE));
        EOB.setForm(new Attachment().setCreation(SAMPLE_DATE));
        EOB.setProcessNote(List.of(new ExplanationOfBenefit.NoteComponent().setType(Enumerations.NoteType.DISPLAY)));
        EOB.setBenefitBalance(List.of(new ExplanationOfBenefit.BenefitBalanceComponent().setDescription("Desc")));
        EOB.setTotal(List.of(new ExplanationOfBenefit.TotalComponent().setAmount(new Money().setValue(13))));
        EOB.setUse(ExplanationOfBenefit.Use.CLAIM);
        EOB.setAdjudication(List.of(new ExplanationOfBenefit.AdjudicationComponent().setAmount(new Money().setValue(11))));
        EOB.setSupportingInfo(List.of(new ExplanationOfBenefit.SupportingInformationComponent().setSequence(3).setReason(new Coding().setCode("code"))));
        ExplanationOfBenefit.ItemComponent item = new ExplanationOfBenefit.ItemComponent()
                .setSequence(3)
                .setCategory(new CodeableConcept().setText("category"))
                .setDiagnosisSequence(List.of(new PositiveIntType(1)))
                .setCareTeamSequence(List.of(new PositiveIntType(2)))
                .setProcedureSequence(List.of(new PositiveIntType(3)));
        EOB.setItem(List.of(item));
        EOB.setIdentifier(List.of(new Identifier().setType(new CodeableConcept().setText("one")).setValue("value")));
        EOB.setStatus(ExplanationOfBenefit.ExplanationOfBenefitStatus.CANCELLED);
        EOB.setType(new CodeableConcept().setText(DUMMY_TYPE));
        EOB.setSubType(new CodeableConcept().setText("subtype"));
        EOB.setBillablePeriod(new Period().setEnd(SAMPLE_DATE).setStart(SAMPLE_DATE));

        EOB.setCareTeam(List.of(
                new ExplanationOfBenefit.CareTeamComponent().setResponsible(true)
                .setSequence(2)
                .setRole(new CodeableConcept().setText("care"))
                .setProvider(new Reference("provider"))
        ));
        EOB.setDiagnosis(List.of(
                new ExplanationOfBenefit.DiagnosisComponent()
                .setSequence(1)
                .setOnAdmission(new CodeableConcept().setText("admission"))
                .setType(List.of(new CodeableConcept().setText(DUMMY_TYPE)))
        ));
        EOB.setProcedure(List.of(
                new ExplanationOfBenefit.ProcedureComponent()
                .setSequence(3)
                .setType(List.of(new CodeableConcept().setText(DUMMY_TYPE)))
                .setUdi(List.of(new Reference("udi")))
                .setProcedure(new CodeableConcept().setText("procedure"))
                .setDate(SAMPLE_DATE)
        ));
    }

    /**
     * Verify that all the data that is not available to the PDP is not in the filtered object
     */
    @Test
    void testFilterIt() {
        ExplanationOfBenefit eobtrim = (ExplanationOfBenefit) ExplanationOfBenefitTrimmerR4.getBenefit(EOB);
        assertEquals(Narrative.NarrativeStatus.ADDITIONAL, eobtrim.getText().getStatus());
        assertEquals(0, eobtrim.getExtension().size());
        assertEquals(0, eobtrim.getContained().size());
        assertEquals(SAMPLE_DATE, eobtrim.getMeta().getLastUpdated());
        assertEquals("Patient/1234", eobtrim.getPatient().getReference());
        assertNull(eobtrim.getPatientTarget().getBirthDate());
        assertNull(eobtrim.getCreated());
        assertNull(eobtrim.getEnterer().getReference());
        assertNull(eobtrim.getEntererTarget());
        assertNull(eobtrim.getInsurer().getReference());
        assertNull(eobtrim.getInsurerTarget().getName());
        assertNull(eobtrim.getReferral().getReference());
        assertNull(eobtrim.getReferralTarget().getId());
        assertEquals(DUMMY_REF, eobtrim.getFacility().getReference());
        assertNull(eobtrim.getFacilityTarget().getName());
        assertNull(eobtrim.getClaim().getReference());
        assertNull(eobtrim.getClaimTarget().getCreated());
        assertNull(eobtrim.getClaimResponse().getReference());
        assertNull(eobtrim.getClaimResponseTarget().getCreated());
        assertNull(eobtrim.getDisposition());
        assertNull(eobtrim.getPrescription().getReference());
        assertNull(eobtrim.getPrescriptionTarget());
        assertNull(eobtrim.getOriginalPrescription().getReference());
        assertNull(eobtrim.getOriginalPrescriptionTarget().getAuthoredOn());
        assertEquals(0, eobtrim.getPrecedence());
        assertNull(eobtrim.getPriority().getText());
        assertNull(eobtrim.getFundsReserveRequested().getText());
        assertNull(eobtrim.getFundsReserve().getText());
        assertEquals(0, eobtrim.getPreAuthRef().size());
        assertEquals(0, eobtrim.getPreAuthRefPeriod().size());
        assertNull(eobtrim.getFormCode().getText());
        assertNull(eobtrim.getBenefitPeriod().getStart());
        assertNull(eobtrim.getBenefitPeriod().getEnd());
        assertNull(eobtrim.getOutcome());
        assertEquals(0, eobtrim.getRelated().size());
        assertNull(eobtrim.getPayee().getParty().getReference());
        assertEquals(0, eobtrim.getInsurance().size());
        assertNull(eobtrim.getAccident().getDate());
        assertEquals(0, eobtrim.getAddItem().size());
        assertNull(eobtrim.getPayment().getDate());
        assertNull(eobtrim.getForm().getCreation());
        assertEquals(0, eobtrim.getProcessNote().size());
        assertEquals(0, eobtrim.getBenefitBalance().size());
        assertNull(eobtrim.getUse());
        assertEquals(0, eobtrim.getAdjudication().size());
        assertEquals(0, eobtrim.getSupportingInfo().size());
        assertEquals(1, eobtrim.getItem().size());
        ExplanationOfBenefit.ItemComponent component = eobtrim.getItem().get(0);
        assertNull(component.getCategory().getText());
        assertEquals(3, component.getSequence());
        assertEquals(1, eobtrim.getIdentifier().size());
        assertEquals(2, component.getCareTeamSequence().get(0).getValue());
        assertEquals(1, component.getDiagnosisSequence().get(0).getValue());
        assertEquals(3, component.getProcedureSequence().get(0).getValue());
        Identifier id = eobtrim.getIdentifier().get(0);
        assertEquals("value", id.getValue());
        assertEquals("one", id.getType().getText());
        assertEquals(ExplanationOfBenefit.ExplanationOfBenefitStatus.CANCELLED, eobtrim.getStatus());
        assertEquals(DUMMY_TYPE, eobtrim.getType().getText());
        assertEquals("subtype", eobtrim.getSubType().getText());
        assertTrue(Math.abs(SAMPLE_DATE.getTime() -  eobtrim.getBillablePeriod().getStart().getTime()) < 1000);
        assertTrue(Math.abs(SAMPLE_DATE.getTime() -  eobtrim.getBillablePeriod().getEnd().getTime()) < 1000);
        assertEquals(1, eobtrim.getCareTeam().size());
        ExplanationOfBenefit.CareTeamComponent careTeamComponent = eobtrim.getCareTeamFirstRep();
        assertTrue(careTeamComponent.getResponsible());
        assertEquals("care", careTeamComponent.getRole().getText());
        assertEquals("provider", careTeamComponent.getProvider().getReference());
        assertEquals(1, eobtrim.getDiagnosis().size());
        ExplanationOfBenefit.DiagnosisComponent diagnosisComponent = eobtrim.getDiagnosisFirstRep();
        assertEquals(1, diagnosisComponent.getSequence());
        assertNotNull(diagnosisComponent.getOnAdmission().getText());
        assertEquals(DUMMY_TYPE, diagnosisComponent.getType().get(0).getText());
        assertEquals(1, eobtrim.getProcedure().size());
        ExplanationOfBenefit.ProcedureComponent procedureComponent = eobtrim.getProcedureFirstRep();
        assertTrue(Math.abs(SAMPLE_DATE.getTime() -  procedureComponent.getDate().getTime()) < 1000);
        assertNotEquals(0, procedureComponent.getType().size());
        assertNotEquals(0, procedureComponent.getUdi().size());
        assertEquals("procedure", ((CodeableConcept) procedureComponent.getProcedure()).getText());
    }

    void giveStats(IBaseResource resource) {
        ExplanationOfBenefit eob = (ExplanationOfBenefit) resource;
        System.out.println(eob.getId() + " has " + eob.getExtension().size() + " extensions");
        System.out.println(eob.getId() + " has " + eob.getSupportingInfo().size() + " supporting info");
        System.out.println(eob.getId() + " has " + eob.getItem().get(0).getExtension().size() + " item extensions");
    }

    @Test
    void testNullEOB() {
        ExplanationOfBenefit eob = (ExplanationOfBenefit) ExplanationOfBenefitTrimmerR4.getBenefit((ExplanationOfBenefit) null);
        assertNull(eob);
    }

    @Test
    void extensionCleanupTestCarrier() {
        IBaseResource eob = EOBLoadUtilities.getR4EOBFromFileInClassPath("eobdata/EOB-for-Carrier-R4.json");
        giveStats(eob);
        ExplanationOfBenefit ab2dEob = (ExplanationOfBenefit) ExplanationOfBenefitTrimmerR4.getBenefit(eob);
        validateEOB(ab2dEob);
    }

    @Test
    void extensionCleanupTestDME() {
        IBaseResource eob = EOBLoadUtilities.getR4EOBFromFileInClassPath("eobdata/EOB-for-DME-R4.json");
        giveStats(eob);
        ExplanationOfBenefit ab2dEob = (ExplanationOfBenefit) ExplanationOfBenefitTrimmerR4.getBenefit(eob);
        validateEOB(ab2dEob);
    }

    @Test
    void extensionCleanupTestHHA() {
        IBaseResource eob = EOBLoadUtilities.getR4EOBFromFileInClassPath("eobdata/EOB-for-HHA-R4.json");
        giveStats(eob);
        ExplanationOfBenefit ab2dEob = (ExplanationOfBenefit) ExplanationOfBenefitTrimmerR4.getBenefit(eob);
        validateEOB(ab2dEob);
    }

    @Test
    void extensionCleanupTestHospice() {
        IBaseResource eob = EOBLoadUtilities.getR4EOBFromFileInClassPath("eobdata/EOB-for-Hospice-R4.json");
        giveStats(eob);
        ExplanationOfBenefit ab2dEob = (ExplanationOfBenefit) ExplanationOfBenefitTrimmerR4.getBenefit(eob);
        validateEOB(ab2dEob);
    }

    @Test
    void extensionCleanupTestInpatient() {
        IBaseResource eob = EOBLoadUtilities.getR4EOBFromFileInClassPath("eobdata/EOB-for-Inpatient-R4.json");
        giveStats(eob);
        ExplanationOfBenefit ab2dEob = (ExplanationOfBenefit) ExplanationOfBenefitTrimmerR4.getBenefit(eob);
        validateEOB(ab2dEob);
    }

    @Test
    void extensionCleanupTestOutpatient() {
        IBaseResource eob = EOBLoadUtilities.getR4EOBFromFileInClassPath("eobdata/EOB-for-Outpatient-R4.json");
        giveStats(eob);
        ExplanationOfBenefit ab2dEob = (ExplanationOfBenefit) ExplanationOfBenefitTrimmerR4.getBenefit(eob);
        validateEOB(ab2dEob);
    }

    @Test
    void extensionCleanupTestSNF() {
        IBaseResource eob = EOBLoadUtilities.getR4EOBFromFileInClassPath("eobdata/EOB-for-SNF-R4.json");
        giveStats(eob);
        ExplanationOfBenefit ab2dEob = (ExplanationOfBenefit) ExplanationOfBenefitTrimmerR4.getBenefit(eob);
        validateEOB(ab2dEob);
    }

    void validateEOB(ExplanationOfBenefit eob) {
        assertNotNull(eob);
        ExplanationOfBenefit.ItemComponent item = eob.getItem().get(0);
        List<Extension> itemExtensions = item.getExtension();
        assertNotNull(itemExtensions);

        assertTrue(itemExtensions.size() <= 3);
        List<String> urls = itemExtensions.stream().map(Extension::getUrl).collect(Collectors.toList());
        List<String> validUrls = List.of(PRICING_STATE, ANESTHESIA_UNIT_COUNT, SUPPLIER_TYPE);
        for (String url : urls) {
            assertTrue(validUrls.contains(url));
            System.out.println(eob.getId() + " Item Extension Found: " + url);
        }

        List<Extension> extensions = eob.getExtension();
        assertTrue(extensions.size() <= 1);
        if (extensions.size() == 1) {
            Extension e = extensions.get(0);
            assertEquals(NL_RECORD_IDENTIFICATION, e.getUrl());
            System.out.println(eob.getId() + " Extension Found: " + e.getUrl());
        }

        List<ExplanationOfBenefit.SupportingInformationComponent> suppInfoComps = eob.getSupportingInfo();
        assertTrue(suppInfoComps.size() <= 1);
        if (suppInfoComps.size() == 1) {
            ExplanationOfBenefit.SupportingInformationComponent component = suppInfoComps.get(0);
            CodeableConcept concept = component.getCode();
            Coding code = concept.getCoding().get(0);
            assertEquals(ExplanationOfBenefitTrimmerR4.RELATED_DIAGNOSIS_GROUP, code.getSystem());
            System.out.println(eob.getId() + " Supporting Info Found: " + code.getSystem());
        }

        int numExtensions = extensions.size();
        Extension newExtension = new Extension();
        newExtension.setUrl("http://ab2d.cms.gov");
        eob.addExtension(newExtension);
        assertEquals((numExtensions + 1), eob.getExtension().size());
    }
}
