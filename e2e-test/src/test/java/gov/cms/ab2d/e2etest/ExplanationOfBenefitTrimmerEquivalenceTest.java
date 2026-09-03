package gov.cms.ab2d.e2etest;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.ab2d.fhir.FhirVersion;
import gov.cms.ab2d.filter.ExplanationOfBenefitTrimmer;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PositiveIntType;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end verification that flipping the {@code eob.v3.inplace.enabled} ({@code EOB_V3_IN_PLACE})
 * feature toggle does not change the trimmed R4V3 output.
 *
 * <p>The test reproduces the production sequence: it first runs the trimmer for every input with the
 * in-place property <b>off</b> (legacy copy-based logic), then <b>switches the property on</b>, then
 * runs the trimmer again for the same inputs (in-place logic), and finally asserts the two passes
 * produced identical output, both as encoded JSON and as a HAPI structural deep-equals.
 *
 * <p>The {@link #inPlaceEnabled} flag mirrors the toggle the worker reads via
 * {@code propertiesService.isToggleOn(EOB_V3_IN_PLACE, false)} in {@code PatientClaimsProcessorImpl};
 * {@link #trim(IBaseResource)} feeds it to the real {@link ExplanationOfBenefitTrimmer} dispatcher,
 * the same call the worker makes.
 *
 * <p>Note: the {@code e2e-test} module consumes {@code ab2d-filters} as the published
 * {@code ${filters-lib.version}} artifact, so this validates the released trimmer the app is pinned
 * to. Source-level equivalence for the in-place refactor is covered in the {@code ab2d-filters}
 * module's unit tests.
 */
@Tag("e2e-test")
class ExplanationOfBenefitTrimmerEquivalenceTest {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();

    private static final String NPI_SYSTEM = "http://hl7.org/fhir/sid/us-npi";
    private static final String CARE_TEAM_ROLE_SYSTEM =
            "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBClaimCareTeamRole";
    private static final String RENDERING_EXT_URL =
            "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-RNDRG-PRVDR-PRTCPTG-CD";

    /** Mirrors the {@code eob.v3.inplace.enabled} property value the worker reads at runtime. */
    private boolean inPlaceEnabled;

    @Test
    void switchingInPlacePropertyDoesNotChangeOutput() {
        // Every named input as a fresh supplier -- the in-place path mutates its input, so each pass
        // must operate on its own freshly-parsed/built EOB instance.
        Map<String, Supplier<IBaseResource>> inputs = inputs();

        // 1. Run the legacy (copy-based) trimmer for every input with the in-place property OFF.
        inPlaceEnabled = false;
        Map<String, ExplanationOfBenefit> legacyResults = new LinkedHashMap<>();
        Map<String, String> legacyJson = new LinkedHashMap<>();
        for (Map.Entry<String, Supplier<IBaseResource>> input : inputs.entrySet()) {
            ExplanationOfBenefit result = trim(input.getValue().get());
            legacyResults.put(input.getKey(), result);
            legacyJson.put(input.getKey(), FHIR_CONTEXT.newJsonParser().encodeResourceToString(result));
        }

        // 2. Switch the in-place property ON.
        inPlaceEnabled = true;

        // 3. Run the in-place trimmer for the same inputs and assert the output is unchanged.
        for (Map.Entry<String, Supplier<IBaseResource>> input : inputs.entrySet()) {
            String name = input.getKey();
            ExplanationOfBenefit inPlaceResult = trim(input.getValue().get());
            String inPlaceJson = FHIR_CONTEXT.newJsonParser().encodeResourceToString(inPlaceResult);

            assertEquals(legacyJson.get(name), inPlaceJson,
                    "JSON output changed after switching eob.v3.inplace.enabled on for " + name);
            assertTrue(legacyResults.get(name).equalsDeep(inPlaceResult),
                    "HAPI structural deep-equals changed after switching eob.v3.inplace.enabled on for " + name);
        }
    }

    @Test
    void nullResourceReturnsNullRegardlessOfToggle() {
        assertNull(ExplanationOfBenefitTrimmer.getBenefit(null, FhirVersion.R4V3, false));
        assertNull(ExplanationOfBenefitTrimmer.getBenefit(null, FhirVersion.R4V3, true));
    }

    /**
     * Trims an EOB through the real {@link ExplanationOfBenefitTrimmer} dispatcher, feeding it the
     * current {@link #inPlaceEnabled} toggle value -- exactly as the worker does with the value it
     * reads from {@code propertiesService.isToggleOn(EOB_V3_IN_PLACE, false)}.
     */
    private ExplanationOfBenefit trim(IBaseResource eob) {
        return (ExplanationOfBenefit) ExplanationOfBenefitTrimmer.getBenefit(eob, FhirVersion.R4V3, inPlaceEnabled);
    }

    /**
     * Loads an EOB fixture from the test classpath. Uses this test's own classloader (not the
     * thread context classloader) so it resolves reliably across runners, and fails loudly with the
     * offending path if the resource is missing rather than yielding a null EOB downstream.
     */
    private static ExplanationOfBenefit loadFixture(String resourcePath) {
        try (InputStream is =
                     ExplanationOfBenefitTrimmerEquivalenceTest.class.getClassLoader().getResourceAsStream(resourcePath)) {
            assertNotNull(is, "Fixture not found on test classpath: " + resourcePath);
            return FHIR_CONTEXT.newJsonParser().parseResource(ExplanationOfBenefit.class, is);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read fixture: " + resourcePath, e);
        }
    }

    /**
     * All inputs to exercise: every R4/R4V3 fixture (including the richer {@code *-R4v3} variants)
     * plus programmatically-built EOBs covering branches the static fixtures do not reliably hit.
     */
    private static Map<String, Supplier<IBaseResource>> inputs() {
        Map<String, Supplier<IBaseResource>> inputs = new LinkedHashMap<>();

        for (String path : List.of(
                "eobdata/EOB-for-Carrier-R4.json",
                "eobdata/EOB-for-DME-R4.json",
                "eobdata/EOB-for-HHA-R4.json",
                "eobdata/EOB-for-Hospice-R4.json",
                "eobdata/EOB-for-Inpatient-R4.json",
                "eobdata/EOB-for-Outpatient-R4.json",
                "eobdata/EOB-for-SNF-R4.json",
                "eobdata/EOB-for-Inpatient-R4V3.json",
                "eobdata/EOB-for-Carrier-R4v3.json",
                "eobdata/EOB-for-HHA-R4v3.json",
                "eobdata/EOB-for-Hospice-R4v3.json",
                "eobdata/EOB-for-Outpatient-R4v3.json",
                "eobdata/EOB-for-SNF-R4v3.json",
                "eobdata/EOB-for-1-R4v3.json",
                "eobdata/EOB-for-2-R4v3.json",
                "eobdata/EOB-for-3-R4v3.json")) {
            inputs.put(path, () -> loadFixture(path));
        }

        inputs.put("built: mixed careTeam and contained providers",
                ExplanationOfBenefitTrimmerEquivalenceTest::buildMixedCareTeamEob);
        inputs.put("built: insurance entries with id and extensions to strip",
                ExplanationOfBenefitTrimmerEquivalenceTest::buildInsuranceHeavyEob);
        inputs.put("built: null and empty collections",
                ExplanationOfBenefitTrimmerEquivalenceTest::buildSparseEob);
        inputs.put("built: supportingInfo NL-record kept",
                ExplanationOfBenefitTrimmerEquivalenceTest::buildNlRecordSupportingInfoEob);
        inputs.put("built: rendering-extension contained kept without NPI",
                ExplanationOfBenefitTrimmerEquivalenceTest::buildRenderingExtensionEob);
        inputs.put("built: empty EOB", ExplanationOfBenefit::new);

        return inputs;
    }

    // ----------------------------------------------------------------------------------------------
    // Builders for edge-case scenarios
    // ----------------------------------------------------------------------------------------------

    /**
     * Three careTeam members: one role-matched with an NPI-bearing contained Practitioner (kept),
     * one role-matched whose contained Practitioner has no NPI and no rendering extension (careTeam
     * kept but contained dropped), and one with a non-matching role (dropped entirely).
     */
    private static ExplanationOfBenefit buildMixedCareTeamEob() {
        ExplanationOfBenefit eob = baseEob();

        Practitioner withNpi = new Practitioner();
        withNpi.setId("ct-npi");
        withNpi.addIdentifier().setSystem(NPI_SYSTEM).setValue("1234567890");

        Practitioner withoutNpi = new Practitioner();
        withoutNpi.setId("ct-no-npi");
        withoutNpi.addIdentifier().setSystem("http://example.com/other").setValue("x");

        Practitioner nonMatchingRolePractitioner = new Practitioner();
        nonMatchingRolePractitioner.setId("ct-nonmatch");
        nonMatchingRolePractitioner.addIdentifier().setSystem(NPI_SYSTEM).setValue("9999999999");

        eob.setContained(new ArrayList<>(List.of(withNpi, withoutNpi, nonMatchingRolePractitioner)));
        eob.setCareTeam(new ArrayList<>(List.of(
                careTeam(1, "attending", "#ct-npi"),
                careTeam(2, "referring", "#ct-no-npi"),
                careTeam(3, "approving", "#ct-nonmatch")
        )));
        return eob;
    }

    /**
     * Multiple insurance entries, each carrying an id and (modifier)extensions that the trimmer
     * must strip while preserving focal + coverage; plus a focal=false entry.
     */
    private static ExplanationOfBenefit buildInsuranceHeavyEob() {
        ExplanationOfBenefit eob = baseEob();

        ExplanationOfBenefit.InsuranceComponent primary = new ExplanationOfBenefit.InsuranceComponent()
                .setCoverage(new Reference("coverage-1"))
                .setFocal(true);
        primary.setId("ins-1");
        primary.addExtension(new Extension("http://example.com/ext", new CodeableConcept().setText("drop")));

        ExplanationOfBenefit.InsuranceComponent secondary = new ExplanationOfBenefit.InsuranceComponent()
                .setCoverage(new Reference("coverage-2"))
                .setFocal(false);
        secondary.setId("ins-2");
        secondary.addModifierExtension(
                new Extension("http://example.com/mod", new CodeableConcept().setText("drop")));

        eob.setInsurance(new ArrayList<>(List.of(primary, secondary)));
        return eob;
    }

    /**
     * Minimal EOB: explicitly empty contained/careTeam/supportingInfo/insurance lists and
     * unset single-valued fields, to verify both strategies agree on the no-data path.
     */
    private static ExplanationOfBenefit buildSparseEob() {
        ExplanationOfBenefit eob = baseEob();
        eob.setContained(new ArrayList<>());
        eob.setCareTeam(new ArrayList<>());
        eob.setSupportingInfo(new ArrayList<>());
        eob.setInsurance(new ArrayList<>());
        eob.setItem(new ArrayList<>());
        return eob;
    }

    /**
     * supportingInfo containing an NL-record-identification coded entry (kept by code system) plus
     * an unrelated entry that must be dropped.
     */
    private static ExplanationOfBenefit buildNlRecordSupportingInfoEob() {
        ExplanationOfBenefit eob = baseEob();

        ExplanationOfBenefit.SupportingInformationComponent nlRecord =
                new ExplanationOfBenefit.SupportingInformationComponent()
                        .setCode(new CodeableConcept().addCoding(new Coding()
                                .setSystem("https://bluebutton.cms.gov/fhir/CodeSystem/CLM-NRLN-RIC-CD")
                                .setCode("V")));

        ExplanationOfBenefit.SupportingInformationComponent unrelated =
                new ExplanationOfBenefit.SupportingInformationComponent()
                        .setCode(new CodeableConcept().addCoding(new Coding()
                                .setSystem("http://example.com/unrelated")
                                .setCode("x")));

        eob.setSupportingInfo(new ArrayList<>(List.of(nlRecord, unrelated)));
        return eob;
    }

    /**
     * A role-matched careTeam member whose contained Organization has no NPI but does carry a
     * rendering extension, exercising the rendering-extension keep path.
     */
    private static ExplanationOfBenefit buildRenderingExtensionEob() {
        ExplanationOfBenefit eob = baseEob();

        Organization rendering = new Organization();
        rendering.setId("ct-rendering");
        rendering.addExtension(new Extension(RENDERING_EXT_URL,
                new CodeableConcept().addCoding(new Coding().setSystem(RENDERING_EXT_URL).setCode("Y"))));

        eob.setContained(new ArrayList<>(List.of(rendering)));
        eob.setCareTeam(new ArrayList<>(List.of(careTeam(1, "rendering", "#ct-rendering"))));
        return eob;
    }

    // ----------------------------------------------------------------------------------------------
    // Small building helpers
    // ----------------------------------------------------------------------------------------------

    /**
     * A minimally-populated, well-formed EOB with the kept scalar fields set plus a representative
     * item so the item-cleaning path runs.
     */
    private static ExplanationOfBenefit baseEob() {
        ExplanationOfBenefit eob = new ExplanationOfBenefit();
        eob.setText(new Narrative().setStatus(Narrative.NarrativeStatus.GENERATED));
        eob.setMeta(new Meta().setLastUpdated(new Date(0)));
        eob.setType(new CodeableConcept().setText("type"));
        eob.setSubType(new CodeableConcept().setText("subtype"));
        eob.setPatient(new Reference("Patient/1"));
        eob.setFacility(new Reference("Facility/1"));
        eob.setProvider(new Reference("Provider/1"));
        eob.setBillablePeriod(new Period().setStart(new Date(0)));
        eob.setStatus(ExplanationOfBenefit.ExplanationOfBenefitStatus.ACTIVE);
        eob.setUse(ExplanationOfBenefit.Use.CLAIM);
        eob.setOutcome(ExplanationOfBenefit.RemittanceOutcome.COMPLETE);
        eob.setIdentifier(new ArrayList<>(List.of(new Identifier().setValue("id1"))));
        eob.setDiagnosis(new ArrayList<>(List.of(
                new ExplanationOfBenefit.DiagnosisComponent().setSequence(1))));
        eob.setProcedure(new ArrayList<>(List.of(
                new ExplanationOfBenefit.ProcedureComponent().setSequence(1))));

        // Fields that must be dropped by both strategies, present so the drop paths actually run.
        eob.setDisposition("drop-me");
        eob.setPrecedence(3);
        eob.setForm(new Attachment().setCreation(new Date(0)));

        ExplanationOfBenefit.ItemComponent item = new ExplanationOfBenefit.ItemComponent()
                .setSequence(1)
                .setCategory(new CodeableConcept().setText("drop"))
                .setCareTeamSequence(new ArrayList<>(List.of(new PositiveIntType(1))));
        eob.setItem(new ArrayList<>(List.of(item)));

        return eob;
    }

    private static ExplanationOfBenefit.CareTeamComponent careTeam(int sequence, String roleCode, String providerRef) {
        return new ExplanationOfBenefit.CareTeamComponent()
                .setSequence(sequence)
                .setRole(new CodeableConcept().addCoding(
                        new Coding().setSystem(CARE_TEAM_ROLE_SYSTEM).setCode(roleCode)))
                .setProvider(new Reference(providerRef));
    }
}
