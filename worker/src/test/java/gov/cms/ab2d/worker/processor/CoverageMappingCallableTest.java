package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.worker.processor.domainmodel.ContractMapping;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static gov.cms.ab2d.worker.processor.BundleUtils.createPatient;
import static gov.cms.ab2d.worker.processor.CoverageMappingCallable.BENEFICIARY_ID;
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

        Bundle bundle1 = buildBundle(0, 10, 2020);
        bundle1.setLink(Collections.singletonList(new Bundle.BundleLinkComponent().setRelation(Bundle.LINK_NEXT)));

        Bundle bundle2 = buildBundle(10, 20, 2020);

        when(bfdClient.requestPartDEnrolleesFromServer(anyString(), anyInt())).thenReturn(bundle1);
        when(bfdClient.requestNextBundleFromServer(any(Bundle.class))).thenReturn(bundle2);

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
        CoverageMappingCallable callable = new CoverageMappingCallable(mapping, bfdClient, false);

        assertFalse(callable.isCompleted());

        CoverageMapping results = callable.call();
        assertEquals(mapping, results);

        assertTrue(callable.isCompleted());
        assertTrue(mapping.isSuccessful());
        assertEquals(20, results.getBeneficiaryIds().size());
    }

    @DisplayName("Filter out years that do not match the provided year")
    @Test
    void filterYear() {

        Bundle bundle1 = buildBundle(0, 10, 2020);
        bundle1.setLink(singletonList(new Bundle.BundleLinkComponent().setRelation(Bundle.LINK_NEXT)));

        Bundle bundle2 = buildBundle(10, 20, 2019);

        when(bfdClient.requestPartDEnrolleesFromServer(anyString(), anyInt())).thenReturn(bundle1);
        when(bfdClient.requestNextBundleFromServer(any(Bundle.class))).thenReturn(bundle2);

        Contract contract = new Contract();
        contract.setContractNumber("TESTING");
        contract.setContractName("TESTING");

        PatientContractCallable patientContractCallable = new PatientContractCallable("TESTING", 1, 2020, bfdClient, false);

        try {
            ContractMapping mapping = patientContractCallable.call();

            assertEquals(10, mapping.getPatients().size());

            int pastYear = (int) ReflectionTestUtils.getField(patientContractCallable, "pastYear");

            assertEquals(10, pastYear);
        } catch (Exception exception) {
            fail("could not execute basic job with mock client", exception);
        }

    }

    @DisplayName("Filter out patients without identifiers")
    @Test
    void filterMissingIdentifier() {

        Bundle bundle1 = buildBundle(0, 10, 2020);
        bundle1.setLink(singletonList(new Bundle.BundleLinkComponent().setRelation(Bundle.LINK_NEXT)));

        Bundle bundle2 = buildBundle(10, 20, 2020);
        bundle2.getEntry().forEach(ec -> {
            Patient patient = (Patient) ec.getResource();
            patient.setIdentifier(emptyList());
        });

        when(bfdClient.requestPartDEnrolleesFromServer(anyString(), anyInt())).thenReturn(bundle1);
        when(bfdClient.requestNextBundleFromServer(any(Bundle.class))).thenReturn(bundle2);

        Contract contract = new Contract();
        contract.setContractNumber("TESTING");
        contract.setContractName("TESTING");

        PatientContractCallable patientContractCallable = new PatientContractCallable("TESTING", 1, 2020, bfdClient, false);

        try {
            ContractMapping mapping = patientContractCallable.call();

            assertEquals(10, mapping.getPatients().size());

            int missingIdentifier = (int) ReflectionTestUtils.getField(patientContractCallable, "missingIdentifier");

            assertEquals(10, missingIdentifier);
        } catch (Exception exception) {
            fail("could not execute basic job with mock client", exception);
        }

    }

    @DisplayName("Exceptional behavior leads to failure")
    @Test
    void exceptionCaught() {

        when(bfdClient.requestPartDEnrolleesFromServer(anyString(), anyInt())).thenThrow(new RuntimeException("exception"));

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
        CoverageMappingCallable callable = new CoverageMappingCallable(mapping, bfdClient, false);

        try {
            callable.call();
        } catch (Exception exception) {
            // ignore exception for sake of test
        }

        assertFalse(mapping.isSuccessful());
        assertTrue(callable.isCompleted());
    }

    private Bundle buildBundle(int startIndex, int endIndex, int year) {
        Bundle bundle1 = new Bundle();
        for (int i = startIndex; i < endIndex; i++) {
            Bundle.BundleEntryComponent component = new Bundle.BundleEntryComponent();
            Patient patient = createPatient("test-" + i, year);
            component.setResource(patient);
            bundle1.addEntry(component);
        }
        return bundle1;
    }
}
