package gov.cms.ab2d.filter;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Date;
import java.util.List;

import static gov.cms.ab2d.filter.ExplanationOfBenefitTrimmerR4.*;
import static org.hl7.fhir.r4.model.ExplanationOfBenefit.RemittanceOutcome.COMPLETE;
import static org.junit.jupiter.api.Assertions.*;

class ExplanationOfBenefitTrimmerR4V3Test {
    private static final ExplanationOfBenefit EOB = new ExplanationOfBenefit();
    private static final Date SAMPLE_DATE = new Date();
    private static final String DUMMY_REF = "1234";
    private static final String DUMMY_TYPE = "type";
    private static final String DUMMY_TXT = "text";

    private static final String SUPPORTING_INFO_SYSTEM = "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBSupportingInfoType";
    private static final String DRG_SUPPORTING_INFO_CODE = "drg";
    private static final String DRG_SYSTEM = "https://www.cms.gov/Medicare/Medicare-Fee-for-Service-Payment/AcuteInpatientPPS/MS-DRG-Classifications-and-Software";
    private static final String CARETEAM_ROLE_SYSTEM = "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole";
    private static final String NPI_SYSTEM = "http://hl7.org/fhir/sid/us-npi";

    @BeforeEach
    void initFullEob() {
        // Populate every sub-object of ExplanationOfBenefit
        populate();
    }

    static void populate() {
        EOB.setText(new Narrative().setStatus(Narrative.NarrativeStatus.ADDITIONAL));
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
        EOB.setRelated(List.of(
                new ExplanationOfBenefit.RelatedClaimComponent()
                        .setReference(new Identifier().setValue("one"))));
        EOB.setPayee(new ExplanationOfBenefit.PayeeComponent().setParty(new Reference("party")));
        EOB.setInsurance(List.of(
                new ExplanationOfBenefit.InsuranceComponent().setCoverage(new Reference("coverage"))));
        EOB.setAccident(new ExplanationOfBenefit.AccidentComponent().setDate(SAMPLE_DATE));
        EOB.setAddItem(List.of(
                new ExplanationOfBenefit.AddedItemComponent()
                        .setUnitPrice(new Money().setValue(10))));
        EOB.setPayment(new ExplanationOfBenefit.PaymentComponent().setDate(SAMPLE_DATE));
        EOB.setForm(new Attachment().setCreation(SAMPLE_DATE));
        EOB.setProcessNote(List.of(
                new ExplanationOfBenefit.NoteComponent()
                        .setType(Enumerations.NoteType.DISPLAY)));
        EOB.setBenefitBalance(List.of(
                new ExplanationOfBenefit.BenefitBalanceComponent()
                        .setDescription("Desc")));
        EOB.setTotal(List.of(
                new ExplanationOfBenefit.TotalComponent()
                        .setAmount(new Money().setValue(13))));
        EOB.setUse(ExplanationOfBenefit.Use.CLAIM);
        EOB.setAdjudication(List.of(
                new ExplanationOfBenefit.AdjudicationComponent()
                        .setAmount(new Money().setValue(11))));

        // DRG supportingInfo per FHIRPath
        ExplanationOfBenefit.SupportingInformationComponent drgSupportingInfo =
                new ExplanationOfBenefit.SupportingInformationComponent()
                        .setSequence(3)
                        .setCategory(
                                new CodeableConcept().addCoding(
                                        new Coding()
                                                .setSystem(SUPPORTING_INFO_SYSTEM)
                                                .setCode(DRG_SUPPORTING_INFO_CODE)
                                )
                        )
                        .setCode(
                                new CodeableConcept().addCoding(
                                        new Coding()
                                                .setSystem(DRG_SYSTEM)
                                                .setCode("123") // sample DRG code
                                )
                        );
        EOB.setSupportingInfo(List.of(drgSupportingInfo));

        ExplanationOfBenefit.ItemComponent item = new ExplanationOfBenefit.ItemComponent()
                .setSequence(3)
                .setCategory(new CodeableConcept().setText("category"))
                .setDiagnosisSequence(List.of(new PositiveIntType(1)))
                .setCareTeamSequence(List.of(new PositiveIntType(1))) // points to careTeam sequence 1
                .setProcedureSequence(List.of(new PositiveIntType(3)));
        EOB.setItem(List.of(item));

        EOB.setIdentifier(List.of(
                new Identifier()
                        .setType(new CodeableConcept().setText("one"))
                        .setValue("value")));
        EOB.setStatus(ExplanationOfBenefit.ExplanationOfBenefitStatus.CANCELLED);
        EOB.setType(new CodeableConcept().setText(DUMMY_TYPE));
        EOB.setSubType(new CodeableConcept().setText("subtype"));
        EOB.setBillablePeriod(new Period().setEnd(SAMPLE_DATE).setStart(SAMPLE_DATE));

        Practitioner attending = new Practitioner();
        attending.setId("careteam-attending");
        attending.addIdentifier()
                .setSystem(NPI_SYSTEM)
                .setValue("rendering-npi");

        Practitioner referring = new Practitioner();
        referring.setId("attending-referring");
        referring.addIdentifier()
                .setSystem(NPI_SYSTEM)
                .setValue("referring-npi");

        Practitioner operating = new Practitioner();
        operating.setId("careteam-operating");
        operating.addIdentifier()
                .setSystem(NPI_SYSTEM)
                .setValue("operating-npi");

        Practitioner otherOperating = new Practitioner();
        otherOperating.setId("careteam-otheroperating");
        otherOperating.addIdentifier()
                .setSystem(NPI_SYSTEM)
                .setValue("otheroperating-npi");

        Practitioner rendering = new Practitioner();
        rendering.setId("careteam-rendering");
        rendering.addIdentifier()
                .setSystem(NPI_SYSTEM)
                .setValue("rendering-npi");

// extensions for rendering provider (test expects them on careteam-attending)
        attending.addExtension(
                new Extension()
                        .setUrl("https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-PRVDR-TYPE-CD")
                        .setValue(
                                new CodeableConcept().addCoding(
                                        new Coding()
                                                .setSystem("http://example.org/prvdr-type")
                                                .setCode("01")
                                )
                        )
        );
        attending.addExtension(
                new Extension()
                        .setUrl("https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-RNDRG-PRVDR-PRTCPTG-CD")
                        .setValue(
                                new CodeableConcept().addCoding(
                                        new Coding()
                                                .setSystem("http://example.org/rendering-participation")
                                                .setCode("F")
                                )
                        )
        );


        rendering.addExtension(
                new Extension()
                        .setUrl("https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-PRVDR-TYPE-CD")
                        .setValue(
                                new CodeableConcept().addCoding(
                                        new Coding()
                                                .setSystem("http://example.org/prvdr-type")
                                                .setCode("01") // sample code
                                )
                        )
        );
        rendering.addExtension(
                new Extension()
                        .setUrl("https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-RNDRG-PRVDR-PRTCPTG-CD")
                        .setValue(
                                new CodeableConcept().addCoding(
                                        new Coding()
                                                .setSystem("http://example.org/rendering-participation")
                                                .setCode("F") // sample code
                                )
                        )
        );

        EOB.setContained(List.of(attending, referring, operating, otherOperating, rendering));

        ExplanationOfBenefit.CareTeamComponent ctAttending =
                new ExplanationOfBenefit.CareTeamComponent()
                        .setSequence(1)
                        .setResponsible(true)
                        .setRole(new CodeableConcept().addCoding(
                                new Coding()
                                        .setSystem(CARETEAM_ROLE_SYSTEM)
                                        .setCode("attending")))
                        .setProvider(new Reference("#careteam-attending"));

        ExplanationOfBenefit.CareTeamComponent ctReferring =
                new ExplanationOfBenefit.CareTeamComponent()
                        .setSequence(2)
                        .setRole(new CodeableConcept().addCoding(
                                new Coding()
                                        .setSystem(CARETEAM_ROLE_SYSTEM)
                                        .setCode("referring")))
                        .setProvider(new Reference("#careteam-referring"));

        ExplanationOfBenefit.CareTeamComponent ctOperating =
                new ExplanationOfBenefit.CareTeamComponent()
                        .setSequence(3)
                        .setRole(new CodeableConcept().addCoding(
                                new Coding()
                                        .setSystem(CARETEAM_ROLE_SYSTEM)
                                        .setCode("operating")))
                        .setProvider(new Reference("#careteam-operating"));

        ExplanationOfBenefit.CareTeamComponent ctOtherOperating =
                new ExplanationOfBenefit.CareTeamComponent()
                        .setSequence(4)
                        .setRole(new CodeableConcept().addCoding(
                                new Coding()
                                        .setSystem(CARETEAM_ROLE_SYSTEM)
                                        .setCode("otheroperating")))
                        .setProvider(new Reference("#careteam-otheroperating"));

        ExplanationOfBenefit.CareTeamComponent ctRendering =
                new ExplanationOfBenefit.CareTeamComponent()
                        .setSequence(5)
                        .setRole(new CodeableConcept().addCoding(
                                new Coding()
                                        .setSystem(CARETEAM_ROLE_SYSTEM)
                                        .setCode("rendering")))
                        .setProvider(new Reference("#careteam-rendering"));

        EOB.setCareTeam(List.of(
                ctAttending,
                ctReferring,
                ctOperating,
                ctOtherOperating,
                ctRendering
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
        ExplanationOfBenefit eobtrim = (ExplanationOfBenefit) ExplanationOfBenefitTrimmerR4V3.getBenefit(EOB);

        assertAll("top level",
                () -> assertEquals(Narrative.NarrativeStatus.ADDITIONAL, eobtrim.getText().getStatus()),
                () -> assertEquals(0, eobtrim.getExtension().size()),
                () -> assertEquals(6, eobtrim.getContained().size()),
                () -> assertEquals(SAMPLE_DATE, eobtrim.getMeta().getLastUpdated()),
                () -> assertEquals("Patient/1234", eobtrim.getPatient().getReference())
        );

        Practitioner renderingProvider = (Practitioner) eobtrim.getContained().get(0);
        assertAll("rendering provider",
                () -> assertEquals("careteam-attending", renderingProvider.getIdPart()),
                () -> assertEquals(1, renderingProvider.getIdentifier().size()),
                () -> assertEquals(NPI_SYSTEM, renderingProvider.getIdentifierFirstRep().getSystem()),
                () -> assertEquals("rendering-npi", renderingProvider.getIdentifierFirstRep().getValue()),
                () -> assertEquals(2, renderingProvider.getExtension().size())
        );

        List<Extension> renderingExtensions = renderingProvider.getExtension();
        Extension prvdrTypeExt = extByUrl(renderingExtensions,
                "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-PRVDR-TYPE-CD");
        Extension renderingPrtcptgExt = extByUrl(renderingExtensions,
                "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-RNDRG-PRVDR-PRTCPTG-CD");

        assertAll("rendering extensions",
                () -> assertEquals("01", ((CodeableConcept) prvdrTypeExt.getValue()).getCodingFirstRep().getCode()),
                () -> assertEquals("F", ((CodeableConcept) renderingPrtcptgExt.getValue()).getCodingFirstRep().getCode())
        );

        assertAll("trimmed null/empty fields",
                () -> assertNulls(
                        eobtrim.getPatientTarget().getBirthDate(),
                        eobtrim.getCreated(),
                        eobtrim.getEnterer().getReference(),
                        eobtrim.getEntererTarget(),
                        eobtrim.getInsurer().getReference(),
                        eobtrim.getInsurerTarget().getName(),
                        eobtrim.getReferral().getReference(),
                        eobtrim.getReferralTarget().getId(),
                        eobtrim.getFacilityTarget().getName(),
                        eobtrim.getClaim().getReference(),
                        eobtrim.getClaimTarget().getCreated(),
                        eobtrim.getClaimResponse().getReference(),
                        eobtrim.getClaimResponseTarget().getCreated(),
                        eobtrim.getDisposition(),
                        eobtrim.getPrescription().getReference(),
                        eobtrim.getPrescriptionTarget(),
                        eobtrim.getOriginalPrescription().getReference(),
                        eobtrim.getOriginalPrescriptionTarget().getAuthoredOn(),
                        eobtrim.getPriority().getText(),
                        eobtrim.getFundsReserveRequested().getText(),
                        eobtrim.getFundsReserve().getText(),
                        eobtrim.getFormCode().getText(),
                        eobtrim.getBenefitPeriod().getStart(),
                        eobtrim.getBenefitPeriod().getEnd(),
                        eobtrim.getOutcome(),
                        eobtrim.getPayee().getParty().getReference(),
                        eobtrim.getAccident().getDate(),
                        eobtrim.getPayment().getDate(),
                        eobtrim.getForm().getCreation(),
                        eobtrim.getUse()
                ),
                () -> assertEquals(DUMMY_REF, eobtrim.getFacility().getReference()),
                () -> assertEquals(0, eobtrim.getPrecedence()),
                () -> assertZeros(
                        eobtrim.getPreAuthRef().size(),
                        eobtrim.getPreAuthRefPeriod().size(),
                        eobtrim.getRelated().size(),
                        eobtrim.getInsurance().size(),
                        eobtrim.getAddItem().size(),
                        eobtrim.getProcessNote().size(),
                        eobtrim.getBenefitBalance().size(),
                        eobtrim.getAdjudication().size()
                )
        );

        ExplanationOfBenefit.SupportingInformationComponent si = eobtrim.getSupportingInfoFirstRep();
        assertAll("supportingInfo DRG",
                () -> assertEquals(1, eobtrim.getSupportingInfo().size()),
                () -> assertEquals("http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBSupportingInfoType",
                        si.getCategory().getCodingFirstRep().getSystem()),
                () -> assertEquals("drg", si.getCategory().getCodingFirstRep().getCode()),
                () -> assertEquals("https://www.cms.gov/Medicare/Medicare-Fee-for-Service-Payment/AcuteInpatientPPS/MS-DRG-Classifications-and-Software",
                        si.getCode().getCodingFirstRep().getSystem()),
                () -> assertEquals("123", si.getCode().getCodingFirstRep().getCode())
        );

        ExplanationOfBenefit.ItemComponent component = eobtrim.getItem().get(0);
        assertAll("item + ids + careTeam/diagnosis/procedure",
                () -> assertEquals(1, eobtrim.getItem().size()),
                () -> assertNull(component.getCategory().getText()),
                () -> assertEquals(3, component.getSequence()),
                () -> assertEquals(1, eobtrim.getIdentifier().size()),
                () -> assertEquals(1, component.getCareTeamSequence().get(0).getValue()),
                () -> assertEquals(1, component.getDiagnosisSequence().get(0).getValue()),
                () -> assertEquals(3, component.getProcedureSequence().get(0).getValue()),
                () -> assertEquals("value", eobtrim.getIdentifierFirstRep().getValue()),
                () -> assertEquals("one", eobtrim.getIdentifierFirstRep().getType().getText()),
                () -> assertEquals(ExplanationOfBenefit.ExplanationOfBenefitStatus.CANCELLED, eobtrim.getStatus()),
                () -> assertEquals(DUMMY_TYPE, eobtrim.getType().getText()),
                () -> assertEquals("subtype", eobtrim.getSubType().getText()),
                () -> assertTrue(Math.abs(SAMPLE_DATE.getTime() - eobtrim.getBillablePeriod().getStart().getTime()) < 1000),
                () -> assertTrue(Math.abs(SAMPLE_DATE.getTime() - eobtrim.getBillablePeriod().getEnd().getTime()) < 1000),
                () -> assertEquals(5, eobtrim.getCareTeam().size()),
                () -> assertEquals("attending", eobtrim.getCareTeamFirstRep().getRole().getCodingFirstRep().getCode()),
                () -> assertEquals("#careteam-attending", eobtrim.getCareTeamFirstRep().getProvider().getReference()),
                () -> assertEquals(1, eobtrim.getDiagnosis().size()),
                () -> assertNotNull(eobtrim.getDiagnosisFirstRep().getOnAdmission().getText()),
                () -> assertEquals(1, eobtrim.getProcedure().size()),
                () -> assertEquals("procedure", ((CodeableConcept) eobtrim.getProcedureFirstRep().getProcedure()).getText())
        );
    }


    void giveStats(IBaseResource resource) {
        ExplanationOfBenefit eob = (ExplanationOfBenefit) resource;
        System.out.println(eob.getId() + " has " + eob.getExtension().size() + " extensions");
        System.out.println(eob.getId() + " has " + eob.getSupportingInfo().size() + " supporting info");
        System.out.println(eob.getId() + " has " + eob.getItem().get(0).getExtension().size() + " item extensions");
    }

    @Test
    void testNullEOB() {
        ExplanationOfBenefit eob = (ExplanationOfBenefit) ExplanationOfBenefitTrimmerR4V3.getBenefit((ExplanationOfBenefit) null);
        assertNull(eob);
    }

    @ParameterizedTest(name = "extensionCleanup: {0}")
    @ValueSource(strings = {
            "eobdata/EOB-for-Carrier-R4.json",
            "eobdata/EOB-for-DME-R4.json",
            "eobdata/EOB-for-HHA-R4.json",
            "eobdata/EOB-for-Hospice-R4.json",
            "eobdata/EOB-for-Inpatient-R4V3.json",
            "eobdata/EOB-for-Outpatient-R4.json",
            "eobdata/EOB-for-SNF-R4.json"
    })
    void extensionCleanupParameterized(String resourcePath) {
        IBaseResource eob = EOBLoadUtilities.getR4EOBFromFileInClassPath(resourcePath);
        giveStats(eob);
        ExplanationOfBenefit ab2dEob = (ExplanationOfBenefit) ExplanationOfBenefitTrimmerR4V3.getBenefit(eob);
        validateEOB(ab2dEob);
    }


    void validateEOB(ExplanationOfBenefit eob) {
        assertNotNull(eob);
        ExplanationOfBenefit.ItemComponent item = eob.getItem().get(0);
        List<Extension> itemExtensions = item.getExtension();
        assertNotNull(itemExtensions);

        assertTrue(itemExtensions.size() <= 3);
        List<String> urls = itemExtensions.stream().map(Extension::getUrl).toList();
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

    private static Extension extByUrl(List<Extension> exts, String url) {
        return exts.stream()
                .filter(e -> url.equals(e.getUrl()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing extension " + url));
    }

    private static void assertNulls(Object... values) {
        for (Object v : values) {
            assertNull(v);
        }
    }

    private static void assertZeros(int... values) {
        for (int v : values) {
            assertEquals(0, v);
        }
    }

}
