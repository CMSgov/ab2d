package gov.cms.ab2d.fhir;

import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Enumerations;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static gov.cms.ab2d.fhir.FhirVersion.R4;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static org.junit.jupiter.api.Assertions.*;

class VersionsTest {

    @Test
    void executeGetMethod() {
        ExplanationOfBenefit eob = new ExplanationOfBenefit();
        Reference patientId = new Reference("Patient/123");
        Versions.invokeSetMethod(eob, "setPatient", patientId, Reference.class);
        Object obj = Versions.invokeGetMethod(eob, "getPatient");
        assertEquals("Patient/123", ((Reference) obj).getReference());
    }

    @Test
    void methodNotAvailableWhenInvokeGet() {
        ExplanationOfBenefit eob = new ExplanationOfBenefit();
        assertThrows(AssertionError.class, () -> {
            Versions.invokeSetMethod(eob, "notReal", null, Object.class);
        });
        Object obj = Versions.invokeGetMethod(eob, "getPatient");
        assertNull(((Reference) obj).getReference());
        assertNull(((Reference) obj).getId());

    }

    @Test
    void executeInstantiate() {
        Object obj1 = Versions.getObject(R4, "ExplanationOfBenefit");
        assertEquals(org.hl7.fhir.r4.model.ExplanationOfBenefit.class, obj1.getClass());
        Object obj2 = Versions.getObject(STU3, "ExplanationOfBenefit");
        assertEquals(ExplanationOfBenefit.class, obj2.getClass());
        assertNull(Versions.getObject(R4, "EOB"));
        Object obj3 = Versions.instantiateClass(STU3, "OperationOutcome", "OperationOutcomeIssueComponent");
        assertEquals(org.hl7.fhir.dstu3.model.OperationOutcome.OperationOutcomeIssueComponent.class, obj3.getClass());
    }

    @Test
    void executeInstantiateEnum() {
        Object obj = Versions.instantiateEnum(R4, "OperationOutcome", "IssueSeverity", "ERROR");
        assertEquals(org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity.ERROR, obj);
    }

    @Test
    void executeSubObject() {
        Object obj = Versions.instantiateClass(R4, "OperationOutcome", "OperationOutcomeIssueComponent");
        assertEquals(org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent.class, obj.getClass());
    }

    @Test
    void testInvalidVersion() {
        assertNull(Versions.getObject(STU3, "Bogus"));
        assertNull(Versions.getObject(STU3, "Bogus", "dumb", String.class));
        assertNull(Versions.instantiateEnum(STU3, "Bogus", "dumb", "ONE"));
    }

    @Test
    void executeInstantiateSimpleEnum() {
        Object obj = Versions.instantiateEnum(R4, "Enumerations", "ResourceType", "PATIENT");
        assertEquals(Enumerations.ResourceType.PATIENT, obj);
    }

    @Test
    void getVersionFromURL() {
        String url1 = "http://localhost:8080/v1/fhir/$export";
        String url2 = "http://localhost:8080/v2/fhir/$export";
        String url3 = "http://localhost:8080/fhir/$export";
        assertEquals(STU3, FhirVersion.fromAB2DUrl(url1));
        assertEquals(R4, FhirVersion.fromAB2DUrl(url2));
        assertNull(FhirVersion.fromAB2DUrl(url3));
    }

    @Test
    void invokeWithArg() {
        String val = "Hello World";
        String ret = (String) Versions.invokeGetMethodWithArg(val, "substring", 6, int.class);
        assertEquals("World", ret);
        assertNull(Versions.invokeGetMethodWithArg(val, "substring", "bad", int.class));
        assertNull(Versions.invokeGetMethod(val, "bad"));
        assertThrows(AssertionError.class, () -> {
            Versions.invokeSetMethod(val, "bad", "bad", String.class);
        });
        assertNull(Versions.instantiateEnum(STU3, "bad", "word"));
    }

    @Test
    void testGetObjectWithParam() {

        OffsetDateTime d1 = OffsetDateTime.now();
        DateTimeType dt1 = (DateTimeType) Versions.getObject(STU3, "DateTimeType", d1.toString(), String.class);
        DateTimeType dt11 = (DateTimeType) Versions.getObject(STU3, "DateTimeType", d1.toString(), String.class);
        assertEquals(dt1.toHumanDisplay(), dt11.toHumanDisplay());
        OffsetDateTime d2 = OffsetDateTime.now().minus(10, ChronoUnit.DAYS);
        DateTimeType dt2 = (DateTimeType) Versions.getObject(STU3, "DateTimeType", d2.toString(), String.class);
        assertNotEquals(dt1.toHumanDisplay(), dt2.toHumanDisplay());
    }

    @Test
    void testGetPatientClass() {
        assertEquals(org.hl7.fhir.dstu3.model.Patient.class, STU3.getPatientClass());
        assertEquals(org.hl7.fhir.r4.model.Patient.class, R4.getPatientClass());
    }

    @Test
    void testGetBundleClass() {
        assertEquals(org.hl7.fhir.dstu3.model.Bundle.class, STU3.getBundleClass());
        assertEquals(org.hl7.fhir.r4.model.Bundle.class, R4.getBundleClass());
    }

    @Test
    void testTimeUtils() {
        OffsetDateTime now = OffsetDateTime.now();
        final org.hl7.fhir.dstu3.model.DateTimeType jobStartedAt = new org.hl7.fhir.dstu3.model.DateTimeType(now.toString());
        String val2 = jobStartedAt.toHumanDisplay();
        String val = STU3.getFhirTime(now);
        assertEquals(val, val2);
    }

    @Test
    void testValid() {
        CapabilityStatement statement = new CapabilityStatement();
        assertFalse(R4.metaDataValid(statement));
        statement.setStatus(Enumerations.PublicationStatus.ACTIVE);
        assertTrue(R4.metaDataValid(statement));
        statement.setStatus(Enumerations.PublicationStatus.UNKNOWN);
        assertFalse(R4.metaDataValid(statement));
        assertFalse(R4.metaDataValid(null));
    }

    @Test
    void testClass() {
        assertEquals(CapabilityStatement.class, R4.getCapabilityClass());
    }

    @Test
    void testGetErrorOutcome() {
        final String errText = "SOMETHING BROKE";
        final IBaseResource o = R4.getErrorOutcome(errText);
        org.hl7.fhir.r4.model.OperationOutcome oo = (org.hl7.fhir.r4.model.OperationOutcome) o;
        assertTrue(oo instanceof  org.hl7.fhir.r4.model.OperationOutcome);
        assertEquals(org.hl7.fhir.r4.model.ResourceType.OperationOutcome, oo.getResourceType());
        assertEquals(1, oo.getIssue().size());
        assertEquals(errText, oo.getIssue().get(0).getDetails().getText());
    }

    @Test
    void testOutcomeToJSON() {
        final String errText = "SOMETHING BROKE";
        final IBaseResource oo = STU3.getErrorOutcome(errText);
        final String payload = STU3.outcomePrettyToJSON(oo);
        assertNotNull(payload);
    }
}