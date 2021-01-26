package gov.cms.ab2d.fhir;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class VersionsTest {

    @Test
    void executeGetMethod() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        org.hl7.fhir.dstu3.model.ExplanationOfBenefit eob = new ExplanationOfBenefit();
        Reference patientId = new Reference("Patient/123");
        Versions.invokeSetMethod(eob, "setPatient", patientId, Reference.class);
        Object obj = Versions.invokeGetMethod(eob, "getPatient");
        assertEquals("Patient/123", ((Reference) obj).getReference());
    }

    @Test
    void executeInstantiate() throws NoSuchMethodException, IllegalAccessException, InstantiationException, VersionNotSupported, InvocationTargetException, ClassNotFoundException {
        Object obj1 = Versions.instantiateClass(Versions.FhirVersions.R4, "ExplanationOfBenefit");
        assertEquals(org.hl7.fhir.r4.model.ExplanationOfBenefit.class, obj1.getClass());
        Object obj2 = Versions.instantiateClass(Versions.FhirVersions.R3, "ExplanationOfBenefit");
        assertEquals(org.hl7.fhir.dstu3.model.ExplanationOfBenefit.class, obj2.getClass());
        assertThrows(RuntimeException.class, () -> Versions.instantiateClass(Versions.FhirVersions.R4, "EOB"));
        Object obj3 = Versions.instantiateClass(Versions.FhirVersions.R3, "OperationOutcome", "OperationOutcomeIssueComponent");
        assertEquals(org.hl7.fhir.dstu3.model.OperationOutcome.OperationOutcomeIssueComponent.class, obj3.getClass());
    }

    @Test
    void executeInstantiateEnum() throws Exception {
        Object obj = Versions.instantiateEnum(Versions.FhirVersions.R4, "OperationOutcome", "IssueSeverity", "ERROR");
        assertEquals(OperationOutcome.IssueSeverity.ERROR, obj);
    }

    @Test
    void executeSubObject() throws NoSuchMethodException, IllegalAccessException, InstantiationException, VersionNotSupported, InvocationTargetException, ClassNotFoundException {
        Object obj = Versions.instantiateClass(Versions.FhirVersions.R4, "OperationOutcome", "OperationOutcomeIssueComponent");
        assertEquals(OperationOutcome.OperationOutcomeIssueComponent.class, obj.getClass());
    }

    @Test
    void testInvalidVersion() {
        assertThrows(RuntimeException.class, () -> Versions.instantiateClass(Versions.FhirVersions.R3, "Bogus"));
        assertThrows(RuntimeException.class, () -> Versions.getClassName(null, "Bogus"));
        assertThrows(RuntimeException.class, () -> Versions.instantiateClassWithParam(Versions.FhirVersions.R3, "Bogus", "dumb", String.class));
        assertThrows(ClassNotFoundException.class, () -> Versions.instantiateEnum(Versions.FhirVersions.R3, "Bogus", "dumb", "ONE"));
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
    }

    @Test
    void testGetVersion() {
        assertEquals(Versions.FhirVersions.R3, Versions.getVersion(FhirContext.forDstu3()));
        assertEquals(Versions.FhirVersions.R4, Versions.getVersion(FhirContext.forR4()));
        assertThrows(RuntimeException.class, () -> Versions.getVersion(FhirContext.forR5()));
        assertThrows(RuntimeException.class, () -> Versions.getVersion(FhirContext.forDstu2()));
        assertThrows(RuntimeException.class, () -> Versions.getVersion(null));
    }
}