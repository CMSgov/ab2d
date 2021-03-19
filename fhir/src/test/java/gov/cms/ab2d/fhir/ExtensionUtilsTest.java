package gov.cms.ab2d.fhir;

import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.instance.model.api.IBase;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static gov.cms.ab2d.fhir.ExtensionUtils.*;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static org.junit.jupiter.api.Assertions.*;

class ExtensionUtilsTest {
    @Test
    void testExtensions() {
        String mbiIdCurrent = "MBI1";
        String mbiIdPrevious = "MBI1";
        org.hl7.fhir.dstu3.model.ExplanationOfBenefit eob = new org.hl7.fhir.dstu3.model.ExplanationOfBenefit();
        IBase extension = ExtensionUtils.createMbiExtension(mbiIdCurrent, true, STU3);
        ExtensionUtils.addExtension(eob, extension, STU3);

        List<Extension> extensions = eob.getExtension();
        assertNotNull(extensions);
        assertEquals(1, extensions.size());
        Extension ex = extensions.get(0);
        assertEquals(ID_EXT, ex.getUrl());
        Identifier id = (Identifier) ex.getValue();
        assertNotNull(id);
        assertEquals(MBI_ID, id.getSystem());
        assertEquals(mbiIdCurrent, id.getValue());
        List<Extension> extensions2 = id.getExtension();
        Extension ex2 = extensions2.get(0);
        assertNotNull(extensions2);
        assertEquals(CURRENCY_IDENTIFIER, ex2.getUrl());
        Coding c = (Coding) ex2.getValue();
        assertEquals(CURRENT_MBI, c.getCode());
    }

    @Test
    void testPatientExt() {
        org.hl7.fhir.dstu3.model.Patient patient = new org.hl7.fhir.dstu3.model.Patient();
        int referenceYear = ExtensionUtils.getReferenceYear(patient);
        assertEquals(-1, referenceYear);
        Date now = new Date();
        Extension ext = new Extension().setUrl(REF_YEAR_EXT).setValue(new DateTimeType().setValue(now));
        patient.addExtension(ext);

        referenceYear = ExtensionUtils.getReferenceYear(patient);
        assertEquals(Calendar.getInstance().get(Calendar.YEAR), referenceYear);
    }
}