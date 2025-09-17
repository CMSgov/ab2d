package gov.cms.ab2d.fhir;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Enumerations;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;

class FhirVersionTest {

  @Test
  void testSupportDefaultSince() {
    assertFalse(FhirVersion.STU3.supportDefaultSince());
    assertTrue(FhirVersion.R4.supportDefaultSince());
  }

  @Test
  void testGetJsonParser() {
    assertNotNull(FhirVersion.R4.getJsonParser());
  }

  @Test
  void testFrom() {
    assertEquals(
      FhirVersion.STU3,
      FhirVersion.from(FhirVersionEnum.DSTU3)
    );
    assertEquals(
      FhirVersion.R4,
      FhirVersion.from(FhirVersionEnum.R4)
    );
    assertNotEquals(
      FhirVersion.STU3,
      FhirVersion.from(FhirVersionEnum.R4)
    );
  }

  @Test
  void testGetContext() {
    // It's not possible to assert that the instances are equal,
    // so we assert that the classes are equal instead.
    assertEquals(
      FhirVersion.R4.getContext().getClass(),
      FhirContext.forR4().getClass()
    );
  }

  @Test
  void testFromAB2DUrl() {
    String url1 = "http://localhost:8080/v1/fhir/$export";
    String url2 = "http://localhost:8080/v2/fhir/$export";
    String url3 = "http://localhost:8080/fhir/$export";
    assertEquals(FhirVersion.STU3, FhirVersion.fromAB2DUrl(url1));
    assertEquals(FhirVersion.R4, FhirVersion.fromAB2DUrl(url2));
    assertNull(FhirVersion.fromAB2DUrl(url3));
  }

  @Test
  void testGetBfdVersionString() {
    assertEquals(
      "/v1/",
      FhirVersion.STU3.getBfdVersionString()
    );
    assertEquals(
      "/v2/",
      FhirVersion.R4.getBfdVersionString()
    );
  }

  @Test
  void testGetClassName() {
    assertEquals(
      "org.hl7.fhir.dstu3.model.Patient",
      FhirVersion.STU3.getClassName("Patient")
    );
    assertEquals(
      "org.hl7.fhir.r4.model.Patient",
      FhirVersion.R4.getClassName("Patient")
    );

    // Set classLocation to null to test the null handling code path
    String classLocation = (String) ReflectionTestUtils.getField(FhirVersion.R4, "classLocation");
    ReflectionTestUtils.setField(FhirVersion.R4, "classLocation", null);
    assertNull(FhirVersion.R4.getClassName("Patient"));
    ReflectionTestUtils.setField(FhirVersion.R4, "classLocation", classLocation);
  }

  @Test
  void testGetClassFromName() {
    assertEquals(
      org.hl7.fhir.dstu3.model.Patient.class,
      FhirVersion.STU3.getClassFromName("Patient")
    );
    assertEquals(
      org.hl7.fhir.r4.model.Patient.class,
      FhirVersion.R4.getClassFromName("Patient")
    );

    assertDoesNotThrow(() -> {
      FhirVersion.R4.getClassFromName("doesNotExist");
    });
    assertNull(FhirVersion.R4.getClassFromName("doesNotExist"));
    assertDoesNotThrow(() -> {
      FhirVersion.STU3.getClassFromName("doesNotExist");
    });
    assertNull(FhirVersion.STU3.getClassFromName("doesNotExist"));
  }

  @Test
  void testGetBundleClass() {
    assertEquals(org.hl7.fhir.dstu3.model.Bundle.class, FhirVersion.STU3.getBundleClass());
    assertEquals(org.hl7.fhir.r4.model.Bundle.class, FhirVersion.R4.getBundleClass());

    // Set classLocation to null to test the null handling code path
    String classLocation = (String) ReflectionTestUtils.getField(FhirVersion.R4, "classLocation");
    ReflectionTestUtils.setField(FhirVersion.R4, "classLocation", null);
    assertNull(FhirVersion.R4.getBundleClass());
    ReflectionTestUtils.setField(FhirVersion.R4, "classLocation", classLocation);
  }

  @Test
  void testGetPatientClass() {
    assertEquals(org.hl7.fhir.dstu3.model.Patient.class, FhirVersion.STU3.getPatientClass());
    assertEquals(org.hl7.fhir.r4.model.Patient.class, FhirVersion.R4.getPatientClass());

    // Set classLocation to null to test the null handling code path
    String classLocation = (String) ReflectionTestUtils.getField(FhirVersion.R4, "classLocation");
    ReflectionTestUtils.setField(FhirVersion.R4, "classLocation", null);
    assertNull(FhirVersion.R4.getPatientClass());
    ReflectionTestUtils.setField(FhirVersion.R4, "classLocation", classLocation);
  }

  @Test
  void testGetFhirTime() {
    OffsetDateTime now = OffsetDateTime.now();
    final org.hl7.fhir.dstu3.model.DateTimeType jobStartedAt = new org.hl7.fhir.dstu3.model.DateTimeType(now.toString());
    String val2 = jobStartedAt.toHumanDisplay();
    String val = FhirVersion.STU3.getFhirTime(now);
    assertEquals(val, val2);
  }

  @Test
  void testMetaDataValid() {
    CapabilityStatement statement = new CapabilityStatement();
    assertFalse(FhirVersion.R4.metaDataValid(statement));
    statement.setStatus(Enumerations.PublicationStatus.ACTIVE);
    assertTrue(FhirVersion.R4.metaDataValid(statement));
    statement.setStatus(Enumerations.PublicationStatus.UNKNOWN);
    assertFalse(FhirVersion.R4.metaDataValid(statement));
    assertFalse(FhirVersion.R4.metaDataValid(null));
  }

  @Test
  void testGetCapabilityClass() {
    assertEquals(CapabilityStatement.class, FhirVersion.R4.getCapabilityClass());

    // Set classLocation to null to test the null handling code path
    String classLocation = (String) ReflectionTestUtils.getField(FhirVersion.R4, "classLocation");
    ReflectionTestUtils.setField(FhirVersion.R4, "classLocation", null);
    assertNull(FhirVersion.R4.getCapabilityClass());
    ReflectionTestUtils.setField(FhirVersion.R4, "classLocation", classLocation);
  }

  @Test
  void testGetErrorOutcome() {
    final String errText = "SOMETHING BROKE";
    final IBaseResource o = FhirVersion.R4.getErrorOutcome(errText);
    org.hl7.fhir.r4.model.OperationOutcome oo = (org.hl7.fhir.r4.model.OperationOutcome) o;
    assertTrue(oo instanceof  org.hl7.fhir.r4.model.OperationOutcome);
    assertEquals(org.hl7.fhir.r4.model.ResourceType.OperationOutcome, oo.getResourceType());
    assertEquals(1, oo.getIssue().size());
    assertEquals(errText, oo.getIssue().get(0).getDetails().getText());
  }

  @Test
  void testOutcomePrettyToJSON() {
    final String errText = "SOMETHING BROKE";
    final IBaseResource oo = FhirVersion.STU3.getErrorOutcome(errText);
    final String payload = FhirVersion.STU3.outcomePrettyToJSON(oo);
    assertNotNull(payload);
  }

  @Test
  void testGetPatientEnum() {
    assertEquals(org.hl7.fhir.dstu3.model.ResourceType.Patient, FhirVersion.STU3.getPatientEnum());
    assertEquals(org.hl7.fhir.r4.model.ResourceType.Patient, FhirVersion.R4.getPatientEnum());
  }
}
