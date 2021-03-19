package gov.cms.ab2d.worker.processor.coverage;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.model.Contract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static gov.cms.ab2d.worker.processor.BundleUtils.createPatient;
import static gov.cms.ab2d.worker.processor.BundleUtils.createPatientWithMultipleMbis;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class CoverageMappingCallableTest {

    private BFDClient bfdClient;

    @BeforeEach
    public void before() {

        bfdClient = Mockito.mock(BFDClient.class);

    }

    @DisplayName("Successfully completing marks as done and transfers results")
    @Test
    void callableFunctions() {

        org.hl7.fhir.dstu3.model.Bundle bundle1 = buildBundle(0, 10, 2020);
        bundle1.setLink(singletonList(new org.hl7.fhir.dstu3.model.Bundle.BundleLinkComponent().setRelation(org.hl7.fhir.dstu3.model.Bundle.LINK_NEXT)));

        org.hl7.fhir.dstu3.model.Bundle bundle2 = buildBundle(10, 20, 2020);

        when(bfdClient.requestPartDEnrolleesFromServer(eq(STU3), anyString(), anyInt(), anyInt())).thenReturn(bundle1);
        when(bfdClient.requestNextBundleFromServer(eq(STU3), any(org.hl7.fhir.dstu3.model.Bundle.class))).thenReturn(bundle2);

        Contract contract = new Contract();
        contract.setContractNumber("TESTING");
        contract.setContractName("TESTING");

        CoveragePeriod period = new CoveragePeriod();
        period.setContract(contract);
        period.setYear(2020);
        period.setMonth(1);

        CoverageSearchEvent cse = new CoverageSearchEvent();
        cse.setCoveragePeriod(period);

        CoverageSearch search = new CoverageSearch();
        search.setPeriod(period);

        CoverageMapping mapping = new CoverageMapping(cse, search);
        CoverageMappingCallable callable = new CoverageMappingCallable(STU3, mapping, bfdClient, false);

        assertFalse(callable.isCompleted());

        CoverageMapping results = callable.call();
        assertEquals(mapping, results);

        assertTrue(callable.isCompleted());
        assertTrue(mapping.isSuccessful());
        assertEquals(20, results.getBeneficiaryIds().size());
    }

    @DisplayName("Successfully completing with skip billable period marks as done and complete")
    @Test
    void callableFunctionsWithSkipBillablePeriod() {

        org.hl7.fhir.dstu3.model.Bundle bundle1 = buildBundle(0, 10, 2020);
        bundle1.setLink(singletonList(new org.hl7.fhir.dstu3.model.Bundle.BundleLinkComponent().setRelation(org.hl7.fhir.dstu3.model.Bundle.LINK_NEXT)));

        org.hl7.fhir.dstu3.model.Bundle bundle2 = buildBundle(10, 20, 2020);

        when(bfdClient.requestPartDEnrolleesFromServer(anyString(), anyInt(), anyInt())).thenReturn(bundle1);
        when(bfdClient.requestNextBundleFromServer(any(org.hl7.fhir.dstu3.model.Bundle.class))).thenReturn(bundle2);
        when(bfdClient.getVersion()).thenReturn(Versions.FhirVersions.STU3);

        Contract contract = new Contract();
        contract.setContractNumber("TESTING");
        contract.setContractName("TESTING");

        CoveragePeriod period = new CoveragePeriod();
        period.setContract(contract);
        period.setYear(2020);
        period.setMonth(1);

        CoverageSearchEvent cse = new CoverageSearchEvent();
        cse.setCoveragePeriod(period);

        CoverageSearch search = new CoverageSearch();
        search.setPeriod(period);

        CoverageMapping mapping = new CoverageMapping(cse, search);
        CoverageMappingCallable callable = new CoverageMappingCallable(mapping, bfdClient, true);

        assertFalse(callable.isCompleted());

        CoverageMapping results = callable.call();
        assertEquals(mapping, results);

        assertTrue(callable.isCompleted());
        assertTrue(mapping.isSuccessful());
        assertEquals(20, results.getBeneficiaryIds().size());
    }

    @DisplayName("Multiple mbis captured")
    @Test
    void multipleMbis() {

        org.hl7.fhir.dstu3.model.Bundle bundle1 = buildBundle(0, 10, 3,2020);
        bundle1.setLink(singletonList(new org.hl7.fhir.dstu3.model.Bundle.BundleLinkComponent().setRelation(org.hl7.fhir.dstu3.model.Bundle.LINK_NEXT)));

        org.hl7.fhir.dstu3.model.Bundle bundle2 = buildBundle(10, 20, 3,2020);

        when(bfdClient.requestPartDEnrolleesFromServer(eq(STU3), anyString(), anyInt(), anyInt())).thenReturn(bundle1);
        when(bfdClient.requestNextBundleFromServer(eq(STU3), any(org.hl7.fhir.dstu3.model.Bundle.class))).thenReturn(bundle2);

        Contract contract = new Contract();
        contract.setContractNumber("TESTING");
        contract.setContractName("TESTING");

        CoveragePeriod period = new CoveragePeriod();
        period.setContract(contract);
        period.setYear(2020);
        period.setMonth(1);

        CoverageSearchEvent cse = new CoverageSearchEvent();
        cse.setCoveragePeriod(period);

        CoverageSearch search = new CoverageSearch();
        search.setPeriod(period);

        CoverageMapping mapping = new CoverageMapping(cse, search);
        CoverageMappingCallable callable = new CoverageMappingCallable(STU3, mapping, bfdClient, false);

        try {
            callable.call();

            for (Identifiers patient : mapping.getBeneficiaryIds()) {
                assertNotNull(patient.getBeneficiaryId());
                assertTrue(patient.getBeneficiaryId().contains("test-"));

                assertNotNull(patient.getCurrentMbi());
                assertEquals(2, patient.getHistoricMbis().size());
            }

            assertEquals(20, mapping.getBeneficiaryIds().size());
        } catch (Exception exception) {
            fail("could not execute basic job with mock client", exception);
        }

    }

    @DisplayName("Current and historic mbis always captured")
    @Test
    void currentMibAppearsFirst() {

        org.hl7.fhir.dstu3.model.Bundle bundle1 = buildBundle(0, 10, 3,2020);
        bundle1.setLink(singletonList(new org.hl7.fhir.dstu3.model.Bundle.BundleLinkComponent().setRelation(org.hl7.fhir.dstu3.model.Bundle.LINK_NEXT)));

        bundle1.getEntry().forEach(bec -> {
            org.hl7.fhir.dstu3.model.Patient patient = (org.hl7.fhir.dstu3.model.Patient) bec.getResource();
            Collections.reverse(patient.getIdentifier());
        });

        org.hl7.fhir.dstu3.model.Bundle bundle2 = buildBundle(10, 20, 3,2020);

        when(bfdClient.requestPartDEnrolleesFromServer(eq(STU3), anyString(), anyInt(), anyInt())).thenReturn(bundle1);
        when(bfdClient.requestNextBundleFromServer(eq(STU3), any(org.hl7.fhir.dstu3.model.Bundle.class))).thenReturn(bundle2);

        Contract contract = new Contract();
        contract.setContractNumber("TESTING");
        contract.setContractName("TESTING");

        CoveragePeriod period = new CoveragePeriod();
        period.setContract(contract);
        period.setYear(2020);
        period.setMonth(1);

        CoverageSearchEvent cse = new CoverageSearchEvent();
        cse.setCoveragePeriod(period);

        CoverageSearch search = new CoverageSearch();
        search.setPeriod(period);

        CoverageMapping mapping = new CoverageMapping(cse, search);
        CoverageMappingCallable callable = new CoverageMappingCallable(STU3, mapping, bfdClient, false);

        try {
            callable.call();

            for (Identifiers patient : mapping.getBeneficiaryIds()) {
                assertNotNull(patient.getBeneficiaryId());
                assertTrue(patient.getBeneficiaryId().contains("test-"));

                assertNotNull(patient.getCurrentMbi());
                assertTrue(patient.getCurrentMbi().endsWith("mbi-0"));
                assertEquals(2, patient.getHistoricMbis().size());
            }

            assertEquals(20, mapping.getBeneficiaryIds().size());
        } catch (Exception exception) {
            fail("could not execute basic job with mock client", exception);
        }

    }


    @DisplayName("Filter out years that do not match the provided year")
    @Test
    void filterYear() {

        org.hl7.fhir.dstu3.model.Bundle bundle1 = buildBundle(0, 10, 2020);
        bundle1.setLink(singletonList(new org.hl7.fhir.dstu3.model.Bundle.BundleLinkComponent().setRelation(org.hl7.fhir.dstu3.model.Bundle.LINK_NEXT)));

        org.hl7.fhir.dstu3.model.Bundle bundle2 = buildBundle(10, 20, 2019);

        when(bfdClient.requestPartDEnrolleesFromServer(eq(STU3), anyString(), anyInt(), anyInt())).thenReturn(bundle1);
        when(bfdClient.requestNextBundleFromServer(eq(STU3), any(org.hl7.fhir.dstu3.model.Bundle.class))).thenReturn(bundle2);

        Contract contract = new Contract();
        contract.setContractNumber("TESTING");
        contract.setContractName("TESTING");

        CoveragePeriod period = new CoveragePeriod();
        period.setContract(contract);
        period.setYear(2020);
        period.setMonth(1);

        CoverageSearchEvent cse = new CoverageSearchEvent();
        cse.setCoveragePeriod(period);

        CoverageSearch search = new CoverageSearch();
        search.setPeriod(period);

        CoverageMapping mapping = new CoverageMapping(cse, search);

        CoverageMappingCallable coverageCallable =
                new CoverageMappingCallable(STU3, mapping, bfdClient, false);

        try {
            mapping = coverageCallable.call();

            assertEquals(10, mapping.getBeneficiaryIds().size());

            int pastYear = (int) ReflectionTestUtils.getField(coverageCallable, "filteredByYear");

            assertEquals(10, pastYear);
        } catch (Exception exception) {
            fail("could not execute basic job with mock client", exception);
        }

    }

    @DisplayName("Filter out patients without identifiers")
    @Test
    void filterMissingIdentifier() {

        org.hl7.fhir.dstu3.model.Bundle bundle1 = buildBundle(0, 10, 2020);
        bundle1.setLink(singletonList(new org.hl7.fhir.dstu3.model.Bundle.BundleLinkComponent().setRelation(org.hl7.fhir.dstu3.model.Bundle.LINK_NEXT)));

        org.hl7.fhir.dstu3.model.Bundle bundle2 = buildBundle(10, 20, 2020);
        bundle2.getEntry().forEach(ec -> {
            org.hl7.fhir.dstu3.model.Patient patient = (org.hl7.fhir.dstu3.model.Patient) ec.getResource();
            patient.setIdentifier(emptyList());
        });

        when(bfdClient.requestPartDEnrolleesFromServer(eq(STU3), anyString(), anyInt(), anyInt())).thenReturn(bundle1);
        when(bfdClient.requestNextBundleFromServer(eq(STU3), any(org.hl7.fhir.dstu3.model.Bundle.class))).thenReturn(bundle2);

        Contract contract = new Contract();
        contract.setContractNumber("TESTING");
        contract.setContractName("TESTING");

        CoveragePeriod period = new CoveragePeriod();
        period.setContract(contract);
        period.setYear(2020);
        period.setMonth(1);

        CoverageSearchEvent cse = new CoverageSearchEvent();
        cse.setCoveragePeriod(period);

        CoverageSearch search = new CoverageSearch();
        search.setPeriod(period);

        CoverageMapping mapping = new CoverageMapping(cse, search);

        CoverageMappingCallable coverageCallable =
                new CoverageMappingCallable(STU3, mapping, bfdClient, false);

        try {
            mapping = coverageCallable.call();

            assertEquals(10, mapping.getBeneficiaryIds().size());

            int missingIdentifier = (int) ReflectionTestUtils.getField(coverageCallable, "missingBeneId");

            assertEquals(10, missingIdentifier);
        } catch (Exception exception) {
            fail("could not execute basic job with mock client", exception);
        }

    }

    @DisplayName("Exceptional behavior leads to failure")
    @Test
    void exceptionCaught() {

        when(bfdClient.requestPartDEnrolleesFromServer(eq(STU3), anyString(), anyInt())).thenThrow(new RuntimeException("exception"));

        Contract contract = new Contract();
        contract.setContractNumber("TESTING");
        contract.setContractName("TESTING");

        CoveragePeriod period = new CoveragePeriod();
        period.setContract(contract);
        period.setYear(2020);
        period.setMonth(1);

        CoverageSearchEvent cse = new CoverageSearchEvent();
        cse.setCoveragePeriod(period);

        CoverageSearch search = new CoverageSearch();
        search.setPeriod(period);

        CoverageMapping mapping = new CoverageMapping(cse, search);
        CoverageMappingCallable callable = new CoverageMappingCallable(STU3, mapping, bfdClient, false);

        try {
            callable.call();
        } catch (Exception exception) {
            // ignore exception for sake of test
        }

        assertFalse(mapping.isSuccessful());
        assertTrue(callable.isCompleted());
    }

    private org.hl7.fhir.dstu3.model.Bundle buildBundle(int startIndex, int endIndex, int year) {
        org.hl7.fhir.dstu3.model.Bundle bundle1 = new org.hl7.fhir.dstu3.model.Bundle();
        for (int i = startIndex; i < endIndex; i++) {
            org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent component = new org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent();
            org.hl7.fhir.dstu3.model.Patient patient = createPatient("test-" + i, "mbi-" + i, year);
            component.setResource(patient);
            bundle1.addEntry(component);
        }
        return bundle1;
    }

    private org.hl7.fhir.dstu3.model.Bundle buildBundle(int startIndex, int endIndex, int numMbis, int year) {
        org.hl7.fhir.dstu3.model.Bundle bundle1 = new org.hl7.fhir.dstu3.model.Bundle();

        for (int i = startIndex; i < endIndex; i++) {
            org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent component = new org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent();
            org.hl7.fhir.dstu3.model.Patient patient = createPatientWithMultipleMbis("test-" + i, numMbis, year);
            component.setResource(patient);
            bundle1.addEntry(component);
        }
        return bundle1;
    }
}
