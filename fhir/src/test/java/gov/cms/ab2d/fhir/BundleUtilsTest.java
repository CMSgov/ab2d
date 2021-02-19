package gov.cms.ab2d.fhir;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static gov.cms.ab2d.fhir.Versions.FhirVersions.STU3;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

class BundleUtilsTest {
    @Test
    void testPatientBundle() {
        Bundle bundle = new Bundle();
        org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent component = new org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent();
        org.hl7.fhir.dstu3.model.Patient patient = new org.hl7.fhir.dstu3.model.Patient();
        assertFalse(BundleUtils.isExplanationOfBenefitResource(patient));
        org.hl7.fhir.dstu3.model.Identifier identifier = new org.hl7.fhir.dstu3.model.Identifier();
        identifier.setSystem("https://bluebutton.cms.gov/resources/variables/bene_id");
        identifier.setValue("test-1");
        patient.setIdentifier(singletonList(identifier));
        component.setResource(patient);
        bundle.addEntry(component);
        bundle.setLink(singletonList(new org.hl7.fhir.dstu3.model.Bundle.BundleLinkComponent()
                .setRelation(org.hl7.fhir.dstu3.model.Bundle.LINK_NEXT)
                .setUrl("http://google.com")));
        var nextLink = BundleUtils.getNextLink(bundle);
        assertNotNull(nextLink);
        assertNull(BundleUtils.getNextLink(null));
        assertEquals("next", BundleUtils.getAvailableLinks(bundle));
        assertNull(BundleUtils.getAvailableLinks(null));
        assertNull(BundleUtils.getAvailableLinksPretty(null));
        assertEquals("next -> http://google.com", BundleUtils.getAvailableLinksPretty(bundle));
        assertNull(BundleUtils.getAvailableLinks(null));

        Stream<IDomainResource> patientStream = BundleUtils.getPatientStream(bundle, STU3);
        assertNull(BundleUtils.getPatientStream(null, STU3));
        List<IDomainResource> patients = patientStream.collect(Collectors.toList());
        assertNotNull(patients);
        assertEquals(1, patients.size());
    }

    @Test
    void testErrors() {
        assertNull(BundleUtils.getEntries(null));
        assertEquals(0, BundleUtils.getTotal(null));
    }

    @Test
    void testEobBundles() {
        Bundle bundle = new Bundle();
        bundle.setTotal(1);
        org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent component = new org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent();
        org.hl7.fhir.dstu3.model.Patient patient = new org.hl7.fhir.dstu3.model.Patient();
        org.hl7.fhir.dstu3.model.ExplanationOfBenefit eob = new org.hl7.fhir.dstu3.model.ExplanationOfBenefit();
        assertTrue(BundleUtils.isExplanationOfBenefitResource(eob));
        eob.setPatient(new org.hl7.fhir.dstu3.model.Reference().setReference("Patient/bene-id"));
        component.setResource(eob);
        bundle.addEntry(component);
        assertEquals(1, BundleUtils.getTotal(bundle));
        assertEquals(0, BundleUtils.getTotal(null));

        List<IBaseResource> resources = BundleUtils.getEobResources(BundleUtils.getEntries(bundle));
        assertNotNull(resources);
        assertEquals(1, resources.size());
        assertNull(BundleUtils.getEobResources(null));
    }
}