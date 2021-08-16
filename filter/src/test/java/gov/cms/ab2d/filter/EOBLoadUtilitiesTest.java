package gov.cms.ab2d.filter;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.ab2d.fhir.EobUtils;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EOBLoadUtilitiesTest {
    static IBaseResource eobC;
    static IBaseResource eobS;
    static FhirContext context = FhirContext.forDstu3();

    static {
        eobC = ExplanationOfBenefitTrimmerSTU3.getBenefit(EOBLoadUtilities.getSTU3EOBFromFileInClassPath("eobdata/EOB-for-Carrier-Claims.json"));
        eobS = ExplanationOfBenefitTrimmerSTU3.getBenefit(EOBLoadUtilities.getSTU3EOBFromFileInClassPath("eobdata/EOB-for-SNF-Claims.json"));
    }

    @Test
    public void testConvertFromFilePatient() throws IOException {
        assertEquals(EobUtils.getPatientId(eobC), -199900000022040L);
    }

    @Test
    public void testLoadFromFilePatient() {
        assertNull(EOBLoadUtilities.getSTU3EOBFromFileInClassPath(""));
        assertNull(EOBLoadUtilities.getSTU3EOBFromFileInClassPath(null));
        org.hl7.fhir.dstu3.model.ExplanationOfBenefit eob = EOBLoadUtilities.getSTU3EOBFromFileInClassPath("eobdata/EOB-for-Carrier-Claims.json");
        assertNotNull(eob);
        assertEquals(eob.getPatient().getReference(), "Patient/-199900000022040");
    }

    @Test
    public void testType() {
        ExplanationOfBenefit eobCarrier = (ExplanationOfBenefit) eobC;
        List<org.hl7.fhir.dstu3.model.Coding> coding = eobCarrier.getType().getCoding();
        assertEquals(4, coding.size());
        org.hl7.fhir.dstu3.model.Coding cd = coding.stream().filter(c -> c.getCode().equals("professional")).findFirst().orElse(null);
        assertNotNull(cd);
        assertEquals(cd.getSystem(), "http://hl7.org/fhir/ex-claimtype");
        assertEquals(cd.getCode(), "professional");
        assertEquals(cd.getDisplay(), "Professional");
    }

    @Test
    public void testResourceType() {
        ExplanationOfBenefit eobCarrier = (ExplanationOfBenefit) eobC;
        assertEquals(eobCarrier.getResourceType(), org.hl7.fhir.dstu3.model.ResourceType.ExplanationOfBenefit);
    }

    @Test
    public void testDiagnosis() {
        ExplanationOfBenefit eobCarrier = (ExplanationOfBenefit) eobC;
        List<org.hl7.fhir.dstu3.model.ExplanationOfBenefit.DiagnosisComponent> diagnoses = eobCarrier.getDiagnosis();
        assertNotNull(diagnoses);
        assertEquals(5, diagnoses.size());
        org.hl7.fhir.dstu3.model.ExplanationOfBenefit.DiagnosisComponent comp = diagnoses.stream()
                .filter(c -> c.getSequence() == 2).findFirst().orElse(null);
        assertNotNull(comp);
        assertEquals(comp.getDiagnosisCodeableConcept().getCoding().size(), 1);
        assertEquals(comp.getDiagnosisCodeableConcept().getCoding().get(0).getCode(), "H8888");
    }

    @Test
    public void testProcedure() throws ParseException {
        ExplanationOfBenefit eobSNF = (ExplanationOfBenefit) eobS;
        List<org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ProcedureComponent> procedures = eobSNF.getProcedure();
        assertNotNull(procedures);
        assertEquals(procedures.size(), 1);
        org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ProcedureComponent comp = procedures.get(0);
        assertEquals(comp.getSequence(), 1);
        assertNotNull(comp.getDate());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        Date expectedTime = sdf.parse("2016-01-16T00:00:00-0600");
        assertEquals(expectedTime.getTime(), comp.getDate().getTime());
        assertEquals(comp.getProcedureCodeableConcept().getCoding().get(0).getSystem(), "http://hl7.org/fhir/sid/icd-9-cm");
        assertEquals(comp.getProcedureCodeableConcept().getCoding().get(0).getCode(), "0TCCCCC");
    }

    @Test
    public void testProvider() {
        ExplanationOfBenefit eobSNF = (ExplanationOfBenefit) eobS;
        org.hl7.fhir.dstu3.model.Reference ref = eobSNF.getProvider();
        assertNotNull(ref);
        assertNotNull(ref.getIdentifier());
        assertEquals(ref.getIdentifier().getSystem(), "https://bluebutton.cms.gov/resources/variables/prvdr_num");
        assertEquals(ref.getIdentifier().getValue(), "299999");
    }

    @Test
    public void testOrganization() {
        ExplanationOfBenefit eobSNF = (ExplanationOfBenefit) eobS;
        org.hl7.fhir.dstu3.model.Reference ref = eobSNF.getOrganization();
        assertNotNull(ref);
        assertNotNull(ref.getIdentifier());
        assertEquals(ref.getIdentifier().getSystem(), "http://hl7.org/fhir/sid/us-npi");
        assertEquals(ref.getIdentifier().getValue(), "1111111111");
    }

    @Test
    public void testFacility() {
        ExplanationOfBenefit eobSNF = (ExplanationOfBenefit) eobS;
        org.hl7.fhir.dstu3.model.Reference ref = eobSNF.getFacility();
        assertNotNull(ref);
        assertNotNull(ref.getIdentifier());
        assertEquals(ref.getIdentifier().getSystem(), "http://hl7.org/fhir/sid/us-npi");
        assertEquals(ref.getIdentifier().getValue(), "1111111111");
    }

    @Test
    public void testIdentifier() {
        ExplanationOfBenefit eobSNF = (ExplanationOfBenefit) eobS;
        List<org.hl7.fhir.dstu3.model.Identifier> ids = eobSNF.getIdentifier();
        assertNotNull(ids);
        assertEquals(ids.size(), 2);
        org.hl7.fhir.dstu3.model.Identifier id = ids.stream()
                .filter(c -> c.getValue().equalsIgnoreCase("900"))
                .findFirst().orElse(null);
        assertNotNull(id);
        assertEquals(id.getSystem(), "https://bluebutton.cms.gov/resources/identifier/claim-group");
    }

    @Test
    public void testCareTeam() {
        ExplanationOfBenefit eobSNF = (ExplanationOfBenefit) eobS;
        List<org.hl7.fhir.dstu3.model.ExplanationOfBenefit.CareTeamComponent> careTeamComponents = eobSNF.getCareTeam();
        assertNotNull(careTeamComponents);
        assertEquals(careTeamComponents.size(), 4);
        org.hl7.fhir.dstu3.model.ExplanationOfBenefit.CareTeamComponent comp = careTeamComponents.stream()
                .filter(c -> c.getSequence() == 2).findFirst().orElse(null);
        assertNotNull(comp);
        assertEquals(comp.getProvider().getIdentifier().getSystem(), "http://hl7.org/fhir/sid/us-npi");
        assertEquals(comp.getProvider().getIdentifier().getValue(), "3333333333");
        assertEquals(comp.getRole().getCoding().get(0).getSystem(), "http://hl7.org/fhir/claimcareteamrole");
        assertEquals(comp.getRole().getCoding().get(0).getCode(), "assist");
        assertEquals(comp.getRole().getCoding().get(0).getDisplay(), "Assisting Provider");
    }

    @Test
    public void testItems() throws ParseException {
        ExplanationOfBenefit eobCarrier = (ExplanationOfBenefit) eobC;
        List<org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent> components = eobCarrier.getItem();
        assertNotNull(components);
        assertEquals(components.size(), 1);
        assertEquals(components.get(0).getCareTeamLinkId().get(0).getValue(), 2);
        assertEquals(components.get(0).getQuantity().getValue().toString(), "1");
        assertEquals(components.get(0).getSequence(), 6);
        assertEquals(components.get(0).getService().getCoding().get(0).getSystem(), "https://bluebutton.cms.gov/resources/codesystem/hcpcs");
        assertEquals(components.get(0).getService().getCoding().get(0).getVersion(), "5");
        assertEquals(components.get(0).getService().getCoding().get(0).getCode(), "92999");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date d = sdf.parse("1999-10-27");
        org.hl7.fhir.dstu3.model.Period period = (org.hl7.fhir.dstu3.model.Period) components.get(0).getServiced();
        assertEquals(period.getStart().getTime(), d.getTime());
        assertEquals(period.getEnd().getTime(), d.getTime());

        ExplanationOfBenefit eobSNF = (ExplanationOfBenefit) eobS;
        org.hl7.fhir.dstu3.model.CodeableConcept location = (org.hl7.fhir.dstu3.model.CodeableConcept) components.get(0).getLocation();
        assertEquals(location.getCoding().get(0).getSystem(), "https://bluebutton.cms.gov/resources/variables/line_place_of_srvc_cd");
        assertEquals(location.getCoding().get(0).getCode(), "11");
        assertEquals(location.getCoding().get(0).getDisplay(), "Office. Location, other than a hospital, skilled nursing facility (SNF), military treatment facility, community health center, State or local public health clinic, or intermediate care facility (ICF), where the health professional routinely provides health examinations, diagnosis, and treatment of illness or injury on an ambulatory basis.");

        List<org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent> components2 = eobSNF.getItem();
        org.hl7.fhir.dstu3.model.Address location2 = (org.hl7.fhir.dstu3.model.Address) components2.get(0).getLocation();
        assertEquals(location2.getState(), "FL");
    }

    @Test
    void testReaderEOB() throws IOException {
        ClassLoader classLoader = EOBLoadUtilities.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("eobdata/EOB-for-Carrier-Claims.json");
        Reader reader = new java.io.InputStreamReader(inputStream);
        assertNull(EOBLoadUtilities.getEOBFromReader((Reader) null, context));
        // STU3
        org.hl7.fhir.dstu3.model.ExplanationOfBenefit benefit = (org.hl7.fhir.dstu3.model.ExplanationOfBenefit) EOBLoadUtilities.getEOBFromReader(reader, context);
        assertNotNull(benefit);
        assertEquals(benefit.getPatient().getReference(), "Patient/-199900000022040");
    }

    @Test
    void testToJson() {
        var jsonParser = context.newJsonParser();
        org.hl7.fhir.dstu3.model.ExplanationOfBenefit eob = EOBLoadUtilities.getSTU3EOBFromFileInClassPath("eobdata/EOB-for-Carrier-Claims.json");
        org.hl7.fhir.dstu3.model.ExplanationOfBenefit eobNew = (org.hl7.fhir.dstu3.model.ExplanationOfBenefit) ExplanationOfBenefitTrimmerSTU3.getBenefit((IBaseResource) eob);
        String payload = jsonParser.encodeResourceToString(eobNew) + System.lineSeparator();
        assertNotNull(payload);
    }
}