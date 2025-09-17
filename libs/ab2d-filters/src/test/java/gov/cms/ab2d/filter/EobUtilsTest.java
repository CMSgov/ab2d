package gov.cms.ab2d.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.Test;

class EobUtilsTest {

  class MockEOB extends ExplanationOfBenefit {
    @Override
    public String fhirType() {
      return "mock";
    }
  }

  IBaseResource eob1 = ExplanationOfBenefitTrimmerSTU3.getBenefit(EOBLoadUtilities.getSTU3EOBFromFileInClassPath("eobdata/EOB-for-Part-D-Claims.json"));
  IBaseResource eob2 = ExplanationOfBenefitTrimmerSTU3.getBenefit(EOBLoadUtilities.getSTU3EOBFromFileInClassPath("eobdata/EOB-for-Carrier-Claims.json"));
  IBaseResource eob3 = new MockEOB();

  @Test
  void testInvokeGetMethod() {
    assertNotNull(EobUtils.invokeGetMethod(eob1, "getBillablePeriod"));
    assertNotNull(EobUtils.invokeGetMethod(eob2, "getBillablePeriod"));

    assertNull(EobUtils.invokeGetMethod(eob1, "fake"));
    assertNull(EobUtils.invokeGetMethod(eob2, "fake"));
  }

  @Test
  void testGetStartDate() {
    assertNull(EobUtils.getStartDate(eob1));
    assertNotNull(EobUtils.getStartDate(eob2));
    assertNull(EobUtils.getStartDate(eob3));
    assertNull(EobUtils.getStartDate((ExplanationOfBenefit) null));
  }

  @Test
  void testGetEndDate() {
    assertNull(EobUtils.getEndDate(eob1));
    assertNotNull(EobUtils.getEndDate(eob2));
    assertNull(EobUtils.getEndDate(eob3));
    assertNull(EobUtils.getEndDate((ExplanationOfBenefit) null));
  }

  @Test
  void testIsPartD() {
    assertTrue(EobUtils.isPartD(eob1));
    assertFalse(EobUtils.isPartD(eob2));
    assertFalse(EobUtils.isPartD(eob3));
    assertFalse(EobUtils.isPartD((ExplanationOfBenefit) null));
  }
}
