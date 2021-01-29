package gov.cms.ab2d.fhir;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.r4.model.Enumerations;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

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
    void executeInstantiate() {
        Object obj1 = Versions.getObject(Versions.FhirVersions.R4, "ExplanationOfBenefit");
        assertEquals(org.hl7.fhir.r4.model.ExplanationOfBenefit.class, obj1.getClass());
        Object obj2 = Versions.getObject(Versions.FhirVersions.R3, "ExplanationOfBenefit");
        assertEquals(ExplanationOfBenefit.class, obj2.getClass());
        assertNull(Versions.getObject(Versions.FhirVersions.R4, "EOB"));
        Object obj3 = Versions.instantiateClass(Versions.FhirVersions.R3, "OperationOutcome", "OperationOutcomeIssueComponent");
        assertEquals(org.hl7.fhir.dstu3.model.OperationOutcome.OperationOutcomeIssueComponent.class, obj3.getClass());
    }

    @Test
    void executeInstantiateEnum() {
        Object obj = Versions.instantiateEnum(Versions.FhirVersions.R4, "OperationOutcome", "IssueSeverity", "ERROR");
        assertEquals(org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity.ERROR, obj);
    }

    @Test
    void executeSubObject() {
        Object obj = Versions.instantiateClass(Versions.FhirVersions.R4, "OperationOutcome", "OperationOutcomeIssueComponent");
        assertEquals(org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent.class, obj.getClass());
    }

    @Test
    void testInvalidVersion() {
        assertNull(Versions.getObject(Versions.FhirVersions.R3, "Bogus"));
        assertThrows(RuntimeException.class, () -> Versions.getClassName(null, "Bogus"));
        assertNull(Versions.getObject(Versions.FhirVersions.R3, "Bogus", "dumb", String.class));
        assertNull(Versions.instantiateEnum(Versions.FhirVersions.R3, "Bogus", "dumb", "ONE"));
    }

    @Test
    void executeInstantiateSimpleEnum() throws Exception {
        Object obj = Versions.instantiateEnum(Versions.FhirVersions.R4, "Enumerations", "ResourceType", "PATIENT");
        assertEquals(Enumerations.ResourceType.PATIENT, obj);
    }

    @Test
    void getVersionFromURL() {
        String url1 = "http://localhost:8080/v1/fhir/$export";
        String url2 = "http://localhost:8080/v2/fhir/$export";
        String url3 = "http://localhost:8080/fhir/$export";
        assertEquals(Versions.FhirVersions.R3, Versions.getVersionFromUrl(url1));
        assertEquals(Versions.FhirVersions.R4, Versions.getVersionFromUrl(url2));
        assertEquals(Versions.FhirVersions.R3, Versions.getVersionFromUrl(url3));
    }

    @Test
    void invokeWithArg() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String val = "Hello World";
        String ret = (String) Versions.invokeGetMethodWithArg(val, "substring", 6, int.class);
        assertEquals("World", ret);
        assertNull(Versions.invokeGetMethodWithArg(val, "substring", "bad", int.class));
        assertNull(Versions.invokeGetMethod(val, "bad"));
        Versions.invokeSetMethod(val, "bad", "bad", String.class);
        assertNull(Versions.instantiateEnum(Versions.FhirVersions.R3, "bad", "word"));
    }

    @Test
    void testGetVersion() {
        assertEquals(Versions.FhirVersions.R3, Versions.getVersion(FhirContext.forDstu3()));
        assertEquals(Versions.FhirVersions.R4, Versions.getVersion(FhirContext.forR4()));
        assertThrows(RuntimeException.class, () -> Versions.getVersion(FhirContext.forR5()));
        assertThrows(RuntimeException.class, () -> Versions.getVersion(FhirContext.forDstu2()));
        assertThrows(RuntimeException.class, () -> Versions.getVersion(null));
        assertNull(Versions.getContextFromVersion(null));
    }

    @Test
    void testGetObjectWithParam() {

        OffsetDateTime d1 = OffsetDateTime.now();
        DateTimeType dt1 = (DateTimeType) Versions.getObject(Versions.FhirVersions.R3, "DateTimeType", d1.toString(), String.class);
        DateTimeType dt11 = (DateTimeType) Versions.getObject(Versions.FhirVersions.R3, "DateTimeType", d1.toString(), String.class);
        assertEquals(dt1.toHumanDisplay(), dt11.toHumanDisplay());
        OffsetDateTime d2 = OffsetDateTime.now().minus(10, ChronoUnit.DAYS);
        DateTimeType dt2 = (DateTimeType) Versions.getObject(Versions.FhirVersions.R3, "DateTimeType", d2.toString(), String.class);
        assertNotEquals(dt1.toHumanDisplay(), dt2.toHumanDisplay());
    }
}