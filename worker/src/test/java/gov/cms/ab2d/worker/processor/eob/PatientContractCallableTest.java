package gov.cms.ab2d.worker.processor.eob;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Identifiers;
import gov.cms.ab2d.worker.processor.coverage.ContractMapping;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static gov.cms.ab2d.worker.processor.eob.BundleUtils.createPatient;
import static gov.cms.ab2d.worker.processor.eob.BundleUtils.createPatientWithMultipleMbis;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class PatientContractCallableTest {

    public static final String TESTING_JOB_ID = "TESTING_JOB_ID";
    private BFDClient bfdClient;

    @BeforeEach
    public void before() {

        bfdClient = Mockito.mock(BFDClient.class);

    }

    @DisplayName("Successfully completing marks as done and transfers results")
    @Test
    void callableFunctions() {

        Bundle bundle1 = buildBundle(0, 10, 2020);
        bundle1.setLink(singletonList(new Bundle.BundleLinkComponent().setRelation(Bundle.LINK_NEXT)));

        Bundle bundle2 = buildBundle(10, 20, 2020);

        when(bfdClient.requestPartDEnrolleesFromServer(anyString(), anyInt())).thenReturn(bundle1);
        when(bfdClient.requestNextBundleFromServer(any(Bundle.class))).thenReturn(bundle2);

        Contract contract = new Contract();
        contract.setContractNumber("TESTING");
        contract.setContractName("TESTING");

        PatientContractCallable patientContractCallable = new PatientContractCallable("TESTING", 1,
                2020, bfdClient, false, TESTING_JOB_ID);

        try {
            ContractMapping mapping = patientContractCallable.call();

            for (Identifiers patient : mapping.getPatients()) {
                assertNotNull(patient.getBeneficiaryId());
                assertTrue(patient.getBeneficiaryId().contains("test-"));

                assertNotNull(patient.getCurrentMbi());
                assertTrue(StringUtils.isNotBlank(patient.getCurrentMbi()));
                assertTrue(patient.getHistoricMbis().isEmpty());
            }

            assertEquals(20, mapping.getPatients().size());
        } catch (Exception exception) {
            fail("could not execute basic job with mock client", exception);
        }

    }

    @DisplayName("Multiple mbis captured")
    @Test
    void multipleMbis() {

        Bundle bundle1 = buildBundle(0, 10, 3,2020);
        bundle1.setLink(singletonList(new Bundle.BundleLinkComponent().setRelation(Bundle.LINK_NEXT)));

        Bundle bundle2 = buildBundle(10, 20, 3,2020);

        when(bfdClient.requestPartDEnrolleesFromServer(anyString(), anyInt())).thenReturn(bundle1);
        when(bfdClient.requestNextBundleFromServer(any(Bundle.class))).thenReturn(bundle2);

        Contract contract = new Contract();
        contract.setContractNumber("TESTING");
        contract.setContractName("TESTING");

        PatientContractCallable patientContractCallable =
                new PatientContractCallable("TESTING", 1, 2020, bfdClient,
                        false, TESTING_JOB_ID);

        try {
            ContractMapping mapping = patientContractCallable.call();

            for (Identifiers patient : mapping.getPatients()) {
                assertNotNull(patient.getBeneficiaryId());
                assertTrue(patient.getBeneficiaryId().contains("test-"));

                assertNotNull(patient.getCurrentMbi());
                assertEquals(2, patient.getHistoricMbis().size());
            }

            assertEquals(20, mapping.getPatients().size());
        } catch (Exception exception) {
            fail("could not execute basic job with mock client", exception);
        }

    }

    @DisplayName("Current and historic mbis always captured")
    @Test
    void currentMibAppearsFirst() {

        Bundle bundle1 = buildBundle(0, 10, 3,2020);
        bundle1.setLink(singletonList(new Bundle.BundleLinkComponent().setRelation(Bundle.LINK_NEXT)));

        bundle1.getEntry().forEach(bec -> {
            Patient patient = (Patient) bec.getResource();
            Collections.reverse(patient.getIdentifier());
        });

        Bundle bundle2 = buildBundle(10, 20, 3,2020);

        when(bfdClient.requestPartDEnrolleesFromServer(anyString(), anyInt())).thenReturn(bundle1);
        when(bfdClient.requestNextBundleFromServer(any(Bundle.class))).thenReturn(bundle2);

        Contract contract = new Contract();
        contract.setContractNumber("TESTING");
        contract.setContractName("TESTING");

        PatientContractCallable patientContractCallable =
                new PatientContractCallable("TESTING", 1, 2020, bfdClient,
                        false, TESTING_JOB_ID);

        try {
            ContractMapping mapping = patientContractCallable.call();

            for (Identifiers patient : mapping.getPatients()) {
                assertNotNull(patient.getBeneficiaryId());
                assertTrue(patient.getBeneficiaryId().contains("test-"));

                assertNotNull(patient.getCurrentMbi());
                assertTrue(patient.getCurrentMbi().endsWith("mbi-0"));
                assertEquals(2, patient.getHistoricMbis().size());
            }

            assertEquals(20, mapping.getPatients().size());
        } catch (Exception exception) {
            fail("could not execute basic job with mock client", exception);
        }

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

        PatientContractCallable patientContractCallable = new PatientContractCallable("TESTING", 1,
                2020, bfdClient, false, TESTING_JOB_ID);

        try {
            ContractMapping mapping = patientContractCallable.call();

            assertEquals(10, mapping.getPatients().size());

            int pastYear = (int) ReflectionTestUtils.getField(patientContractCallable, "filteredByYear");

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

        PatientContractCallable patientContractCallable = new PatientContractCallable("TESTING", 1,
                2020, bfdClient, false, TESTING_JOB_ID);

        try {
            ContractMapping mapping = patientContractCallable.call();

            assertEquals(10, mapping.getPatients().size());

            int missingIdentifier = (int) ReflectionTestUtils.getField(patientContractCallable, "missingBeneId");

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

        PatientContractCallable callable = new PatientContractCallable("TESTING", 1,
                2020, bfdClient, false, TESTING_JOB_ID);
        assertThrows(RuntimeException.class, callable::call);
    }

    private Bundle buildBundle(int startIndex, int endIndex, int year) {
        Bundle bundle1 = new Bundle();

        for (int i = startIndex; i < endIndex; i++) {
            Bundle.BundleEntryComponent component = new Bundle.BundleEntryComponent();
            Patient patient = createPatient("test-" + i, "mbi-" + i, year);
            component.setResource(patient);
            bundle1.addEntry(component);
        }
        return bundle1;
    }

    private Bundle buildBundle(int startIndex, int endIndex, int numMbis, int year) {
        Bundle bundle1 = new Bundle();

        for (int i = startIndex; i < endIndex; i++) {
            Bundle.BundleEntryComponent component = new Bundle.BundleEntryComponent();
            Patient patient = createPatientWithMultipleMbis("test-" + i, numMbis, year);
            component.setResource(patient);
            bundle1.addEntry(component);
        }
        return bundle1;
    }
}