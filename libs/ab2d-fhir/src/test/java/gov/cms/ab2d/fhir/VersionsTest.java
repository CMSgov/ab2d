package gov.cms.ab2d.fhir;

import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Reference;
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
    void executeGetMethodWithBadSetArgs() {
        ExplanationOfBenefit eob = new ExplanationOfBenefit();
        Versions.invokeSetMethod(eob, "setPatient", "broken", Reference.class);
        Object obj = Versions.invokeGetMethod(eob, "getPatient");
        assertEquals(null, ((Reference) obj).getReference());
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
    void testInvalidClass() {
        assertNull(Versions.instantiateClass(STU3, "does-not-exist", "does-not-exist"));
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

        obj = Versions.instantiateEnum(R4, "Enumerations", "FAKE", "PATIENT");
        assertNull(obj);
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
}
