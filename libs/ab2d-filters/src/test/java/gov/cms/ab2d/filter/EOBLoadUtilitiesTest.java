package gov.cms.ab2d.filter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.IFhirVersion;

import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class EOBLoadUtilitiesTest {
    private static IBaseResource eobC;
    private static IBaseResource eobS;

    static {
        eobC = ExplanationOfBenefitTrimmerSTU3.getBenefit(EOBLoadUtilities.getSTU3EOBFromFileInClassPath("eobdata/EOB-for-Carrier-Claims.json"));
        eobS = ExplanationOfBenefitTrimmerSTU3.getBenefit(EOBLoadUtilities.getSTU3EOBFromFileInClassPath("eobdata/EOB-for-SNF-Claims.json"));
    }

    // Our mock FhirVersion class is used to test the switch case statement inside of EOBLoadUtilities.getEOBFromReader.
    // We need to extend from a FhirVersionEnum that is actually on our classpath, so we extend from the R4 version.
    // We then override the getVersion method to return a version that is not supported by the switch case statement.
    // Choosing `FhirVersionEnum.R5` as the version to return here was an arbitrary choice. If we ever add support
    // for R5, this test will need to be updated.
    class MockFhirVersion extends org.hl7.fhir.r4.hapi.ctx.FhirR4{
        public FhirVersionEnum getVersion() {
            return FhirVersionEnum.R5;
        }
    }
    class MockFhirContext extends FhirContext {
        @Override
        public IFhirVersion getVersion() {
            return new MockFhirVersion();
        }
    }

    @Test
    void testType() {
        ExplanationOfBenefit eobCarrier = (ExplanationOfBenefit) eobC;
        List<org.hl7.fhir.dstu3.model.Coding> coding = eobCarrier.getType().getCoding();
        assertEquals(4, coding.size());
        org.hl7.fhir.dstu3.model.Coding cd = coding.stream().filter(c -> c.getCode().equals("professional")).findFirst().orElse(null);
        assertNotNull(cd);
        assertEquals("http://hl7.org/fhir/ex-claimtype", cd.getSystem());
        assertEquals("professional", cd.getCode());
        assertEquals("Professional", cd.getDisplay());
    }

    @Test
    void testResourceType() {
        ExplanationOfBenefit eobCarrier = (ExplanationOfBenefit) eobC;
        assertEquals(org.hl7.fhir.dstu3.model.ResourceType.ExplanationOfBenefit, eobCarrier.getResourceType());
    }

    @Test
    void testDiagnosis() {
        ExplanationOfBenefit eobCarrier = (ExplanationOfBenefit) eobC;
        List<ExplanationOfBenefit.DiagnosisComponent> diagnoses = eobCarrier.getDiagnosis();
        assertNotNull(diagnoses);
        assertEquals(5, diagnoses.size());
        ExplanationOfBenefit.DiagnosisComponent comp = diagnoses.stream()
                .filter(c -> c.getSequence() == 2).findFirst().orElse(null);
        assertNotNull(comp);
        assertEquals(1, comp.getDiagnosisCodeableConcept().getCoding().size());
        assertEquals("H8888", comp.getDiagnosisCodeableConcept().getCoding().get(0).getCode());
    }

    @Test
    void testProcedure() throws ParseException {
        ExplanationOfBenefit eobSNF = (ExplanationOfBenefit) eobS;
        List<ExplanationOfBenefit.ProcedureComponent> procedures = eobSNF.getProcedure();
        assertNotNull(procedures);
        assertEquals(1, procedures.size());
        ExplanationOfBenefit.ProcedureComponent comp = procedures.get(0);
        assertEquals(1, comp.getSequence());
        assertNotNull(comp.getDate());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
        Date expectedTime = sdf.parse("2016-01-16T00:00:00-0600");
        assertEquals(expectedTime.getTime(), comp.getDate().getTime());
        assertEquals("http://hl7.org/fhir/sid/icd-9-cm", comp.getProcedureCodeableConcept().getCoding().get(0).getSystem());
        assertEquals("0TCCCCC", comp.getProcedureCodeableConcept().getCoding().get(0).getCode());
    }

    @Test
    void testProvider() {
        ExplanationOfBenefit eobSNF = (ExplanationOfBenefit) eobS;
        org.hl7.fhir.dstu3.model.Reference ref = eobSNF.getProvider();
        assertNotNull(ref);
        assertNotNull(ref.getIdentifier());
        assertEquals("https://bluebutton.cms.gov/resources/variables/prvdr_num", ref.getIdentifier().getSystem());
        assertEquals("299999", ref.getIdentifier().getValue());
    }

    @Test
    void testOrganization() {
        ExplanationOfBenefit eobSNF = (ExplanationOfBenefit) eobS;
        org.hl7.fhir.dstu3.model.Reference ref = eobSNF.getOrganization();
        assertNotNull(ref);
        assertNotNull(ref.getIdentifier());
        assertEquals("http://hl7.org/fhir/sid/us-npi", ref.getIdentifier().getSystem());
        assertEquals("1111111111", ref.getIdentifier().getValue());
    }

    @Test
    void testFacility() {
        ExplanationOfBenefit eobSNF = (ExplanationOfBenefit) eobS;
        org.hl7.fhir.dstu3.model.Reference ref = eobSNF.getFacility();
        assertNotNull(ref);
        assertNotNull(ref.getIdentifier());
        assertEquals("http://hl7.org/fhir/sid/us-npi", ref.getIdentifier().getSystem());
        assertEquals("1111111111", ref.getIdentifier().getValue());
    }

    @Test
    void testIdentifier() {
        ExplanationOfBenefit eobSNF = (ExplanationOfBenefit) eobS;
        List<org.hl7.fhir.dstu3.model.Identifier> ids = eobSNF.getIdentifier();
        assertNotNull(ids);
        assertEquals(2, ids.size());
        org.hl7.fhir.dstu3.model.Identifier id = ids.stream()
                .filter(c -> c.getValue().equalsIgnoreCase("900"))
                .findFirst().orElse(null);
        assertNotNull(id);
        assertEquals("https://bluebutton.cms.gov/resources/identifier/claim-group", id.getSystem());
    }

    @Test
    void testCareTeam() {
        ExplanationOfBenefit eobSNF = (ExplanationOfBenefit) eobS;
        List<ExplanationOfBenefit.CareTeamComponent> careTeamComponents = eobSNF.getCareTeam();
        assertNotNull(careTeamComponents);
        assertEquals(4, careTeamComponents.size());
        ExplanationOfBenefit.CareTeamComponent comp = careTeamComponents.stream()
                .filter(c -> c.getSequence() == 2).findFirst().orElse(null);
        assertNotNull(comp);
        assertEquals("http://hl7.org/fhir/sid/us-npi", comp.getProvider().getIdentifier().getSystem());
        assertEquals("3333333333", comp.getProvider().getIdentifier().getValue());
        assertEquals("http://hl7.org/fhir/claimcareteamrole", comp.getRole().getCoding().get(0).getSystem());
        assertEquals("assist", comp.getRole().getCoding().get(0).getCode());
        assertEquals("Assisting Provider", comp.getRole().getCoding().get(0).getDisplay());
    }

    @Test
    void testItems() throws ParseException {
        ExplanationOfBenefit eobCarrier = (ExplanationOfBenefit) eobC;
        List<ExplanationOfBenefit.ItemComponent> components = eobCarrier.getItem();
        assertNotNull(components);
        assertEquals(1, components.size());
        assertEquals(2, components.get(0).getCareTeamLinkId().get(0).getValue());
        assertEquals("1", components.get(0).getQuantity().getValue().toString());
        assertEquals(6, components.get(0).getSequence());
        assertEquals("https://bluebutton.cms.gov/resources/codesystem/hcpcs", components.get(0).getService().getCoding().get(0).getSystem());
        assertEquals("5", components.get(0).getService().getCoding().get(0).getVersion());
        assertEquals("92999", components.get(0).getService().getCoding().get(0).getCode());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Date d = sdf.parse("1999-10-27");
        org.hl7.fhir.dstu3.model.Period period = (org.hl7.fhir.dstu3.model.Period) components.get(0).getServiced();
        assertEquals(period.getStart().getTime(), d.getTime());
        assertEquals(period.getEnd().getTime(), d.getTime());

        ExplanationOfBenefit eobSNF = (ExplanationOfBenefit) eobS;
        org.hl7.fhir.dstu3.model.CodeableConcept location = (org.hl7.fhir.dstu3.model.CodeableConcept) components.get(0).getLocation();
        assertEquals("https://bluebutton.cms.gov/resources/variables/line_place_of_srvc_cd", location.getCoding().get(0).getSystem());
        assertEquals("11", location.getCoding().get(0).getCode());
        assertEquals("Office. Location, other than a hospital, skilled nursing facility (SNF), military treatment facility, community health center, State or local public health clinic, or intermediate care facility (ICF), where the health professional routinely provides health examinations, diagnosis, and treatment of illness or injury on an ambulatory basis.", location.getCoding().get(0).getDisplay());

        List<ExplanationOfBenefit.ItemComponent> components2 = eobSNF.getItem();
        org.hl7.fhir.dstu3.model.Address location2 = (org.hl7.fhir.dstu3.model.Address) components2.get(0).getLocation();
        assertEquals("FL", location2.getState());
    }

    @Test
    void testReaderEOB() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        // DSTU3
        InputStream inputStream = classLoader.getResourceAsStream("eobdata/EOB-for-Carrier-Claims.json");
        Reader reader = new java.io.InputStreamReader(inputStream, StandardCharsets.UTF_8);
        assertNull(EOBLoadUtilities.getEOBFromReader((Reader) null, FhirContext.forDstu3()));
        ExplanationOfBenefit benefit = (ExplanationOfBenefit) EOBLoadUtilities.getEOBFromReader(reader, FhirContext.forDstu3());
        assertNotNull(benefit);
        assertEquals("Patient/-199900000022040", benefit.getPatient().getReference());

        // R4
        inputStream = classLoader.getResourceAsStream("eobdata/EOB-for-Carrier-R4.json");
        reader = new java.io.InputStreamReader(inputStream, StandardCharsets.UTF_8);
        assertNull(EOBLoadUtilities.getEOBFromReader((Reader) null, FhirContext.forR4()));
        org.hl7.fhir.r4.model.ExplanationOfBenefit benefitR4 = (org.hl7.fhir.r4.model.ExplanationOfBenefit) EOBLoadUtilities.getEOBFromReader(reader, FhirContext.forR4());
        assertNotNull(benefitR4);
        assertEquals("Patient/567834", benefitR4.getPatient().getReference());

        // invalid context
        inputStream = classLoader.getResourceAsStream("eobdata/EOB-for-Carrier-R4.json");
        reader = new java.io.InputStreamReader(inputStream, StandardCharsets.UTF_8);
        assertNull(EOBLoadUtilities.getEOBFromReader(null, new MockFhirContext()));
        assertNull(EOBLoadUtilities.getEOBFromReader(reader, new MockFhirContext()));
    }

    @Test
    void testGetSTU3EOB() {
        // null tests
        assertNull(EOBLoadUtilities.getSTU3EOBFromFileInClassPath(""));
        assertNull(EOBLoadUtilities.getSTU3EOBFromFileInClassPath("does-not-exist.json"));

        // not null tests
        var jsonParser = FhirContext.forDstu3().newJsonParser();
        org.hl7.fhir.dstu3.model.ExplanationOfBenefit eob = EOBLoadUtilities.getSTU3EOBFromFileInClassPath("eobdata/EOB-for-Carrier-Claims.json");
        org.hl7.fhir.dstu3.model.ExplanationOfBenefit eobNew = (org.hl7.fhir.dstu3.model.ExplanationOfBenefit) ExplanationOfBenefitTrimmer.getBenefit((IBaseResource) eob);
        String payload = jsonParser.encodeResourceToString(eobNew) + System.lineSeparator();
        assertNotNull(payload);
    }

    @Test
    void testGetR4EOB() {
        // null tests
        assertNull(EOBLoadUtilities.getR4EOBFromFileInClassPath(""));
        assertNull(EOBLoadUtilities.getR4EOBFromFileInClassPath("does-not-exist.json"));

        // not null tests
        var jsonParser = FhirContext.forR4().newJsonParser();
        org.hl7.fhir.r4.model.ExplanationOfBenefit eob = EOBLoadUtilities.getR4EOBFromFileInClassPath("eobdata/EOB-for-Carrier-R4.json");
        org.hl7.fhir.r4.model.ExplanationOfBenefit eobNew = (org.hl7.fhir.r4.model.ExplanationOfBenefit) ExplanationOfBenefitTrimmer.getBenefit((IBaseResource) eob);
        String payload = jsonParser.encodeResourceToString(eobNew) + System.lineSeparator();
        assertNotNull(payload);
    }
}
