package gov.cms.ab2d.fhir;

import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Extension;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static gov.cms.ab2d.fhir.ExtensionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

class IdentifierUtilsTest {
    @Test
    void testGetMbis() {
        org.hl7.fhir.dstu3.model.Patient patient = new org.hl7.fhir.dstu3.model.Patient();
        assertFalse(BundleUtils.isExplanationOfBenefitResource(patient));
        org.hl7.fhir.dstu3.model.Identifier identifier = new org.hl7.fhir.dstu3.model.Identifier();
        identifier.setSystem("https://bluebutton.cms.gov/resources/variables/bene_id");
        identifier.setValue("test-1");
        var identifier2 = new org.hl7.fhir.dstu3.model.Identifier();
        identifier2.setSystem(MBI_ID);
        identifier2.setValue("mbi-1");
        Extension extension = new Extension().setUrl(CURRENCY_IDENTIFIER).setValue(new Coding().setCode(CURRENT_MBI));
        identifier2.addExtension(extension);

        var identifier3 = new org.hl7.fhir.dstu3.model.Identifier();
        identifier3.setSystem(MBI_ID);
        identifier3.setValue("mbi-2");
        Extension extension2 = new Extension().setUrl(CURRENCY_IDENTIFIER).setValue(new Coding().setCode(HISTORIC_MBI));
        identifier3.addExtension(extension2);
        patient.setIdentifier(List.of(identifier, identifier2, identifier3));

        assertEquals("mbi-1", IdentifierUtils.getCurrentMbi(patient));
        Set<String> historical = IdentifierUtils.getHistoricMbi(patient);
        String h = (String) historical.toArray()[0];
        assertEquals("mbi-2", h);

        assertEquals("test-1", IdentifierUtils.getBeneId(patient));
    }
}