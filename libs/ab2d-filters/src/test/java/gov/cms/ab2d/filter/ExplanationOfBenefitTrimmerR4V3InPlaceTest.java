package gov.cms.ab2d.filter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
    void allocationCountNotWorseThanLegacy() {
        // Warm up
        for (int i = 0; i < 1000; i++) {
            IBaseResource eob = EOBLoadUtilities.getR4EOBFromFileInClassPath("eobdata/EOB-for-Inpatient-R4V3.json");
            ExplanationOfBenefitTrimmerR4V3.getBenefit(eob);
        }
        for (int i = 0; i < 1000; i++) {
            IBaseResource eob = EOBLoadUtilities.getR4EOBFromFileInClassPath("eobdata/EOB-for-Inpatient-R4V3.json");
            ExplanationOfBenefitTrimmerR4V3.getBenefitInPlace(eob);
        }

        long gcBefore = totalGcCount();
        for (int i = 0; i < 10_000; i++) {
            IBaseResource eob = EOBLoadUtilities.getR4EOBFromFileInClassPath("eobdata/EOB-for-Inpatient-R4V3.json");
            ExplanationOfBenefitTrimmerR4V3.getBenefit(eob);
        }
        long legacyGc = totalGcCount() - gcBefore;

        gcBefore = totalGcCount();
        for (int i = 0; i < 10_000; i++) {
            IBaseResource eob = EOBLoadUtilities.getR4EOBFromFileInClassPath("eobdata/EOB-for-Inpatient-R4V3.json");
            ExplanationOfBenefitTrimmerR4V3.getBenefitInPlace(eob);
        }
        long inPlaceGc = totalGcCount() - gcBefore;

        assertTrue(inPlaceGc <= legacyGc,
                "In-place GC count (%d) must not exceed legacy GC count (%d)".formatted(inPlaceGc, legacyGc));
    }

    /**
     * High-load allocation comparison: measures thread-local bytes allocated by the legacy
     * copy-based path vs the in-place mutation path across all claim types.
     *
     * Prints per-op bytes, GC counts, and throughput. Fails if in-place allocates more than legacy.
     */
    @Test
    @Tag("allocation")
    void highLoadAllocationComparison() throws InterruptedException {
        final int WARMUP = 3_000;
        final int ITERATIONS = 30_000;
        final String[] FIXTURES = {
            "eobdata/EOB-for-Inpatient-R4V3.json",
            "eobdata/EOB-for-HHA-R4.json",
            "eobdata/EOB-for-Hospice-R4.json",
            "eobdata/EOB-for-Outpatient-R4.json",
            "eobdata/EOB-for-SNF-R4.json",
            "eobdata/EOB-for-Carrier-R4.json",
            "eobdata/EOB-for-DME-R4.json"
        };

        String[] rawJsons = loadFixtureJsons(FIXTURES);
        IParser parser = FHIR_CONTEXT.newJsonParser();

        com.sun.management.ThreadMXBean threadBean =
                (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        assumeTrue(threadBean.isThreadAllocatedMemorySupported(),
                "Thread allocated memory tracking not supported on this JVM");
        assumeTrue(threadBean.isThreadAllocatedMemoryEnabled(),
                "Thread allocated memory tracking not enabled on this JVM");
        long threadId = Thread.currentThread().getId();

        // Warm up both paths to JIT steady state
        for (int i = 0; i < WARMUP; i++) {
            String json = rawJsons[i % rawJsons.length];
            ExplanationOfBenefitTrimmerR4V3.getBenefit(parser.parseResource(json));
            ExplanationOfBenefitTrimmerR4V3.getBenefitInPlace(parser.parseResource(json));
        }

        System.gc();
        Thread.sleep(200);

        // --- Measure legacy (copy-based) ---
        long gcBefore    = totalGcCount();
        long allocBefore = threadBean.getThreadAllocatedBytes(threadId);
        long nsBefore    = System.nanoTime();

        for (int i = 0; i < ITERATIONS; i++) {
            ExplanationOfBenefitTrimmerR4V3.getBenefit(
                    parser.parseResource(rawJsons[i % rawJsons.length]));
        }

        long legacyNs    = System.nanoTime() - nsBefore;
        long legacyBytes = threadBean.getThreadAllocatedBytes(threadId) - allocBefore;
        long legacyGcs   = totalGcCount() - gcBefore;

        System.gc();
        Thread.sleep(200);

        // --- Measure in-place (mutation-based) ---
        gcBefore    = totalGcCount();
        allocBefore = threadBean.getThreadAllocatedBytes(threadId);
        nsBefore    = System.nanoTime();

        for (int i = 0; i < ITERATIONS; i++) {
            ExplanationOfBenefitTrimmerR4V3.getBenefitInPlace(
                    parser.parseResource(rawJsons[i % rawJsons.length]));
        }

        long inPlaceNs    = System.nanoTime() - nsBefore;
        long inPlaceBytes = threadBean.getThreadAllocatedBytes(threadId) - allocBefore;
        long inPlaceGcs   = totalGcCount() - gcBefore;

        double savedPct = 100.0 * (legacyBytes - inPlaceBytes) / legacyBytes;

        System.out.printf(
                "%n=== EOB Trimmer High-Load Allocation Report (%,d iterations) ===%n" +
                "Legacy  : %,15d bytes | %,10.0f bytes/op | GCs: %2d | %.2f µs/op%n" +
                "In-Place: %,15d bytes | %,10.0f bytes/op | GCs: %2d | %.2f µs/op%n" +
                "Saved   : %,.0f bytes/op  (%.1f%% reduction in allocations)%n",
                ITERATIONS,
                legacyBytes,  (double) legacyBytes  / ITERATIONS, legacyGcs,  legacyNs  / 1e3 / ITERATIONS,
                inPlaceBytes, (double) inPlaceBytes  / ITERATIONS, inPlaceGcs, inPlaceNs / 1e3 / ITERATIONS,
                (double) (legacyBytes - inPlaceBytes) / ITERATIONS, savedPct);

        assertTrue(inPlaceBytes < legacyBytes,
                "In-place must allocate fewer bytes than legacy. " +
                "Legacy: %,d bytes, In-place: %,d bytes".formatted(legacyBytes, inPlaceBytes));
    }

    private static String[] loadFixtureJsons(String[] paths) {
        return Arrays.stream(paths)
                .map(path -> {
                    try (InputStream is = ExplanationOfBenefitTrimmerR4V3InPlaceTest.class
                            .getClassLoader().getResourceAsStream(path)) {
                        if (is == null) throw new IllegalStateException("Resource not found: " + path);
                        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to load fixture: " + path, e);
                    }
                })
                .toArray(String[]::new);
    }

    private static long totalGcCount() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(GarbageCollectorMXBean::getCollectionCount)
                .filter(c -> c >= 0)
                .sum();
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
        eob.setInsurance(List.of(
                new ExplanationOfBenefit.InsuranceComponent()
                        .setCoverage(new Reference("coverage"))
                        .setFocal(true)));
        eob.setIdentifier(List.of(new Identifier().setValue("id1")));
        eob.setDiagnosis(List.of(new ExplanationOfBenefit.DiagnosisComponent().setSequence(1)));
        eob.setProcedure(List.of(new ExplanationOfBenefit.ProcedureComponent().setSequence(1)));

        String drgSystem = "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBSupportingInfoType";
        String msDrgSystem = "https://www.cms.gov/Medicare/Medicare-Fee-for-Service-Payment/AcuteInpatientPPS/MS-DRG-Classifications-and-Software";
        eob.setSupportingInfo(List.of(
                new ExplanationOfBenefit.SupportingInformationComponent()
                        .setCategory(new CodeableConcept().addCoding(new Coding().setSystem(drgSystem).setCode("drg")))
                        .setCode(new CodeableConcept().addCoding(new Coding().setSystem(msDrgSystem).setCode("123")))));

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

        eob.setContained(List.of(attending));
        eob.setCareTeam(List.of(
                new ExplanationOfBenefit.CareTeamComponent()
                        .setSequence(1)
                        .setRole(new CodeableConcept().addCoding(
                                new Coding().setSystem("http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole").setCode("attending")))
                        .setProvider(new Reference("#careteam-attending"))));

        return eob;
    }
}
