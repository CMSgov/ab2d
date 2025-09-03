package gov.cms.ab2d.fhir;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Period;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static gov.cms.ab2d.fhir.EobUtils.EOB_TYPE_CODE_SYS;
import static gov.cms.ab2d.fhir.EobUtils.EOB_TYPE_PART_D_CODE_VAL;
import static org.junit.jupiter.api.Assertions.*;

class EobUtilsTest {

    @Test
    void testIt() {
        org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent component = new org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent();
        org.hl7.fhir.dstu3.model.Patient patient = new org.hl7.fhir.dstu3.model.Patient();
        org.hl7.fhir.dstu3.model.ExplanationOfBenefit eob = new org.hl7.fhir.dstu3.model.ExplanationOfBenefit();
        assertTrue(BundleUtils.isExplanationOfBenefitResource(eob));
        assertNull(EobUtils.getPatientId(eob));
        eob.setPatient(new org.hl7.fhir.dstu3.model.Reference().setReference("Patient/1"));
        Date d1 = new Date();
        Date d2 = new Date();
        Period period = new Period().setStart(d1).setEnd(d2);
        eob.setBillablePeriod(period);
        CodeableConcept concept = new CodeableConcept().addCoding(new Coding().setSystem("http://www.bluebutton.com/" + EOB_TYPE_CODE_SYS).setCode(EOB_TYPE_PART_D_CODE_VAL));
        eob.setType(concept);

        assertEquals(1, EobUtils.getPatientId(eob));
        assertNull(EobUtils.getPatientId(null));
        assertNull(EobUtils.getBillablePeriod(null));
        Period obj = (Period) EobUtils.getBillablePeriod(eob);
        assertNotNull(obj);
        assertEquals(d1, period.getStart());
        assertEquals(d2, period.getEnd());

        assertNull(EobUtils.getStartDate(null));
        assertNull(EobUtils.getEndDate(null));
        assertEquals(d1, EobUtils.getStartDate(eob));
        assertEquals(d2, EobUtils.getEndDate(eob));

        assertTrue(EobUtils.isPartD(eob));
        assertFalse(EobUtils.isPartD(null));

        assertFalse(EobUtils.isPartD(patient));
    }
}