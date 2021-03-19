package gov.cms.ab2d.filter;

import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.hl7.fhir.r4.model.ExplanationOfBenefit.RemittanceOutcome.COMPLETE;
import static org.junit.jupiter.api.Assertions.*;

public class ExplanationOfBenefitTrimmerR4Test {
    private ExplanationOfBenefit eob;
    private Date sampleDate = new Date();

    @BeforeEach
    void initFullEob() {
        // Populate every sub-object of ExplanationOfBenefit
        eob = new ExplanationOfBenefit();
        eob.setText(new Narrative().setStatus(Narrative.NarrativeStatus.ADDITIONAL));
        eob.setContained(List.of(eob));
        eob.setMeta(new Meta().setLastUpdated(sampleDate));
        eob.setPatient(new Reference("Patient/1234"));
        eob.setPatientTarget(new Patient().setBirthDate(sampleDate));
        eob.setCreated(sampleDate);
        eob.setEnterer(new Reference("1234"));
        eob.setEntererTarget(new Patient().setBirthDate(sampleDate));
        eob.setInsurer(new Reference("1234"));
        eob.setInsurerTarget(new Organization().setName("Org"));
        eob.setProvider(new Reference("1234"));
        eob.setProviderTarget(new Organization().setName("Org"));
        eob.setReferral(new Reference("1234"));
        ServiceRequest req = new ServiceRequest();
        req.setId("Ref");
        eob.setReferralTarget(req);
        eob.setFacility(new Reference("1234"));
        eob.setFacilityTarget(new Location().setName("Facility"));
        eob.setClaim(new Reference("1234"));
        eob.setClaimTarget(new Claim().setCreated(sampleDate));
        eob.setClaimResponse(new Reference("1234"));
        eob.setClaimResponseTarget(new ClaimResponse().setCreated(sampleDate));
        eob.setDisposition("Disposition");
        eob.setPrescription(new Reference("1234"));
        eob.setPrescriptionTarget(new VisionPrescription().setCreated(sampleDate));
        eob.setOriginalPrescription(new Reference("1234"));
        eob.setOriginalPrescriptionTarget(new MedicationRequest().setAuthoredOn(sampleDate));
        eob.setPrecedence(2);
        eob.setPriority(new CodeableConcept().setText("text"));
        eob.setFundsReserveRequested(new CodeableConcept().setText("text"));
        eob.setFundsReserve(new CodeableConcept().setText("text"));
        eob.setPreAuthRef(List.of(new StringType("preauth")));
        eob.setPreAuthRefPeriod(List.of(new Period().setEnd(sampleDate).setStart(sampleDate)));
        eob.setFormCode(new CodeableConcept().setText("text"));
        eob.setBenefitPeriod(new Period().setEnd(sampleDate).setStart(sampleDate));
        eob.setOutcome(COMPLETE);
        eob.setRelated(List.of(new ExplanationOfBenefit.RelatedClaimComponent().setReference(new Identifier().setValue("one"))));
        eob.setPayee(new ExplanationOfBenefit.PayeeComponent().setParty(new Reference("party")));
        eob.setInsurance(List.of(new ExplanationOfBenefit.InsuranceComponent().setCoverage(new Reference("coverage"))));
        eob.setAccident(new ExplanationOfBenefit.AccidentComponent().setDate(sampleDate));
        eob.setAddItem(List.of(new ExplanationOfBenefit.AddedItemComponent().setUnitPrice(new Money().setValue(10))));
        eob.setPayment(new ExplanationOfBenefit.PaymentComponent().setDate(sampleDate));
        eob.setForm(new Attachment().setCreation(sampleDate));
        eob.setProcessNote(List.of(new ExplanationOfBenefit.NoteComponent().setType(Enumerations.NoteType.DISPLAY)));
        eob.setBenefitBalance(List.of(new ExplanationOfBenefit.BenefitBalanceComponent().setDescription("Desc")));
        eob.setTotal(List.of(new ExplanationOfBenefit.TotalComponent().setAmount(new Money().setValue(13))));
        eob.setUse(ExplanationOfBenefit.Use.CLAIM);
        eob.setAdjudication(List.of(new ExplanationOfBenefit.AdjudicationComponent().setAmount(new Money().setValue(11))));
        eob.setSupportingInfo(List.of(new ExplanationOfBenefit.SupportingInformationComponent().setSequence(3).setReason(new Coding().setCode("code"))));
        eob.setItem(List.of(new ExplanationOfBenefit.ItemComponent().setSequence(3).setCategory(new CodeableConcept().setText("category"))));
        eob.setIdentifier(List.of(new Identifier().setType(new CodeableConcept().setText("one")).setValue("value")));
        eob.setStatus(ExplanationOfBenefit.ExplanationOfBenefitStatus.CANCELLED);
        eob.setType(new CodeableConcept().setText("type"));
        eob.setSubType(new CodeableConcept().setText("subtype"));
        eob.setBillablePeriod(new Period().setEnd(sampleDate).setStart(sampleDate));

        eob.setCareTeam(List.of(
                new ExplanationOfBenefit.CareTeamComponent().setResponsible(true)
                .setRole(new CodeableConcept().setText("care"))
                .setProvider(new Reference("provider"))
        ));
        eob.setDiagnosis(List.of(
                new ExplanationOfBenefit.DiagnosisComponent()
                .setSequence(1)
                .setOnAdmission(new CodeableConcept().setText("admission"))
                .setType(List.of(new CodeableConcept().setText("type")))
        ));
        eob.setProcedure(List.of(
                new ExplanationOfBenefit.ProcedureComponent()
                .setType(List.of(new CodeableConcept().setText("type")))
                .setUdi(List.of(new Reference("udi")))
                .setProcedure(new CodeableConcept().setText("procedure"))
                .setDate(sampleDate)
        ));
    }

    /**
     * Verify that all the data that is not available to the PDP is not in the filtered object
     */
    @Test
    void testFilterIt() {
        ExplanationOfBenefit eobtrim = (ExplanationOfBenefit) ExplanationOfBenefitTrimmerR4.getBenefit(eob);
        assertEquals(Narrative.NarrativeStatus.ADDITIONAL, eobtrim.getText().getStatus());
        assertEquals(0, eobtrim.getExtension().size());
        assertEquals(0, eobtrim.getContained().size());
        assertEquals(sampleDate, eobtrim.getMeta().getLastUpdated());
        assertEquals("Patient/1234", eobtrim.getPatient().getReference());
        assertNull(eobtrim.getPatientTarget().getBirthDate());
        assertNull(eobtrim.getCreated());
        assertNull(eobtrim.getEnterer().getReference());
        assertNull(eobtrim.getEntererTarget());
        assertNull(eobtrim.getInsurer().getReference());
        assertNull(eobtrim.getInsurerTarget().getName());
        assertEquals("1234", eobtrim.getProvider().getReference());
        assertNull(eobtrim.getProviderTarget());
        assertNull(eobtrim.getReferral().getReference());
        assertNull(eobtrim.getReferralTarget().getId());
        assertEquals("1234", eobtrim.getFacility().getReference());
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
        assertEquals(0, eobtrim.getTotal().size());
        assertNull(eobtrim.getUse());
        assertEquals(0, eobtrim.getAdjudication().size());
        assertEquals(0, eobtrim.getSupportingInfo().size());
        assertEquals(1, eobtrim.getItem().size());
        ExplanationOfBenefit.ItemComponent component = eobtrim.getItem().get(0);
        assertNull(component.getCategory().getText());
        assertEquals(3, component.getSequence());
        assertEquals(1, eobtrim.getIdentifier().size());
        Identifier id = eobtrim.getIdentifier().get(0);
        assertEquals("value", id.getValue());
        assertEquals("one", id.getType().getText());
        assertEquals(ExplanationOfBenefit.ExplanationOfBenefitStatus.CANCELLED, eobtrim.getStatus());
        assertEquals("type", eobtrim.getType().getText());
        assertEquals("subtype", eobtrim.getSubType().getText());
        assertTrue(Math.abs(sampleDate.getTime() -  eobtrim.getBillablePeriod().getStart().getTime()) < 1000);
        assertTrue(Math.abs(sampleDate.getTime() -  eobtrim.getBillablePeriod().getEnd().getTime()) < 1000);
        assertEquals(1, eobtrim.getCareTeam().size());
        ExplanationOfBenefit.CareTeamComponent careTeamComponent = eobtrim.getCareTeamFirstRep();
        assertEquals(true, careTeamComponent.getResponsible());
        assertEquals("care", careTeamComponent.getRole().getText());
        assertEquals("provider", careTeamComponent.getProvider().getReference());
        assertEquals(1, eobtrim.getDiagnosis().size());
        ExplanationOfBenefit.DiagnosisComponent diagnosisComponent = eobtrim.getDiagnosisFirstRep();
        assertEquals(1, diagnosisComponent.getSequence());
        assertNull(diagnosisComponent.getOnAdmission().getText());
        assertEquals("type", diagnosisComponent.getType().get(0).getText());
        assertEquals(1, eobtrim.getProcedure().size());
        ExplanationOfBenefit.ProcedureComponent procedureComponent = eobtrim.getProcedureFirstRep();
        assertTrue(Math.abs(sampleDate.getTime() -  procedureComponent.getDate().getTime()) < 1000);
        assertEquals(0, procedureComponent.getType().size());
        assertEquals(0, procedureComponent.getUdi().size());
        assertEquals("procedure", ((CodeableConcept) procedureComponent.getProcedure()).getText());
    }
}
