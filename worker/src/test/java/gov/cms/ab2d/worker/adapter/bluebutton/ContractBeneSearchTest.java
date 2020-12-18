package gov.cms.ab2d.worker.adapter.bluebutton;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.common.util.FilterOutByDate;
import gov.cms.ab2d.worker.processor.eob.BundleUtils;
import gov.cms.ab2d.worker.processor.eob.ProgressTracker;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleLinkComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Month;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import static gov.cms.ab2d.worker.processor.eob.BundleUtils.createBundleEntry;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractBeneSearchTest {

    private static final int YEAR = 2020;

    private ProgressTracker tracker;

    @Mock BFDClient client;
    @Mock LogManager eventLogger;

    private ContractBeneSearch cut;
    private String contractNumber = "S0000";
    private int currentMonth = Month.MARCH.getValue();
    private Bundle bundle;

    @BeforeEach
    void setUp() {
        final ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(6);
        taskExecutor.setMaxPoolSize(12);
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        taskExecutor.initialize();
        cut = new ContractBeneSearchImpl(client, eventLogger, taskExecutor, false);

        bundle = createBundle();
        lenient().when(client.requestPartDEnrolleesFromServer(anyString(), anyInt())).thenReturn(bundle);

        tracker = ProgressTracker.builder()
                .jobUuid("JOBID")
                .numContracts(1)
                .failureThreshold(1)
                .currentMonth(currentMonth)
                .build();
    }

    @Test
    @DisplayName("given contractNumber, get patients from BFD API")
    void GivenContractNumber_ShouldReturnPatients() throws ExecutionException, InterruptedException {
        ContractBeneficiaries response = cut.getPatients(contractNumber, currentMonth, tracker);

        assertThat(response, notNullValue());
        assertThat(response.getContractNumber(), is(contractNumber));

        Map<String, ContractBeneficiaries.PatientDTO> patients = response.getPatients();
        assertThat(patients.size(), is(1));
        verify(client, times(3)).requestPartDEnrolleesFromServer(anyString(), anyInt());
        verify(client, never()).requestNextBundleFromServer(Mockito.any(Bundle.class));
    }

    @Test
    void GivenPatientActiveInJanuary_ShouldReturnOneRowInDateRangesUnderContract() throws ExecutionException, InterruptedException {
        ContractBeneficiaries response = cut.getPatients(contractNumber, Month.JANUARY.getValue(), tracker);

        Map<String, ContractBeneficiaries.PatientDTO> patients = response.getPatients();
        assertThat(patients.size(), is(1));

        ContractBeneficiaries.PatientDTO patient0 = patients.values().stream()
                .filter(c -> c.getBeneficiaryId().equalsIgnoreCase("ccw_patient_000")).findFirst().get();

        assertThat(patient0.getBeneficiaryId(), is("ccw_patient_000"));
        assertTrue(patient0.getIdentifiers().getCurrentMbi().contains("mbi0"));
        assertThat(patient0.getDateRangesUnderContract().size(), is(1));

        verify(client).requestPartDEnrolleesFromServer(anyString(), anyInt());
        verify(client, never()).requestNextBundleFromServer(Mockito.any(Bundle.class));
    }

    @Test
    void GivenPatientActiveInJanAndFeb_ShouldReturnTwoRowsInDateRangesUnderContract() throws ExecutionException, InterruptedException {
        ContractBeneficiaries response = cut.getPatients(contractNumber, Month.FEBRUARY.getValue(), tracker);
        Collection <ContractBeneficiaries.PatientDTO> patients = response.getPatients().values();
        ContractBeneficiaries.PatientDTO patient0 = patients.stream().filter(c -> c.getBeneficiaryId().equalsIgnoreCase("ccw_patient_000")).findFirst().get();

        assertThat(patient0.getBeneficiaryId(), is("ccw_patient_000"));
        assertThat(patient0.getDateRangesUnderContract().size(), is(2));

        verify(client, times(2)).requestPartDEnrolleesFromServer(anyString(), anyInt());
        verify(client, never()).requestNextBundleFromServer(Mockito.any(Bundle.class));
    }

    @Test
    void GivenPatientActiveInMonth1And3ButNot2_ShouldReturnTwoRowsInDateRangesUnderContract() throws ExecutionException, InterruptedException {
        Bundle bundle1 = bundle.copy();
        // add 2nd patient
        List<BundleEntryComponent> entries = bundle1.getEntry();
        entries.add(createBundleEntry("ccw_patient_001", "mbi1", YEAR));

        Date startJanuary = FilterOutByDate.getStartOfMonth(1, YEAR);
        Date endJanuary = FilterOutByDate.getEndOfMonth(1, YEAR);

        Date startMarch = FilterOutByDate.getStartOfMonth(3, YEAR);
        Date endMarch = FilterOutByDate.getEndOfMonth(3, YEAR);

        when(client.requestPartDEnrolleesFromServer("S0000", 1))
                .thenReturn(bundle1);    // January - patient1 is active
        when(client.requestPartDEnrolleesFromServer("S0000", 2))
                .thenReturn(bundle);    // January - patient1 is active
        when(client.requestPartDEnrolleesFromServer("S0000", 3))
                .thenReturn(bundle1);    // January - patient1 is active

        ContractBeneficiaries response = cut.getPatients(contractNumber, Month.MARCH.getValue(), tracker);

        for (ContractBeneficiaries.PatientDTO patient : response.getPatients().values()) {
            if (patient.getBeneficiaryId().equalsIgnoreCase("ccw_patient_000")) {
                //expect patient0 to be active in all 3 months
                assertThat(patient.getDateRangesUnderContract().size(), is(3));
            } else if (patient.getBeneficiaryId().equalsIgnoreCase("ccw_patient_001")) {
                //expect patient1 to be active in only 2 months
                assertThat(patient.getDateRangesUnderContract().size(), is(2));
                List<FilterOutByDate.DateRange> dateRangesUnderContract = patient.getDateRangesUnderContract();
                // month is January
                assertThat(dateRangesUnderContract.get(0).getStart().getTime(), is(startJanuary.getTime()));
                assertThat(dateRangesUnderContract.get(0).getEnd().getTime(), is(endJanuary.getTime()));

                //month is March
                assertThat(dateRangesUnderContract.get(1).getStart().getTime(), is(startMarch.getTime()));
                assertThat(dateRangesUnderContract.get(1).getEnd().getTime(), is(endMarch.getTime()));
            }
        }

        verify(client, times(3)).requestPartDEnrolleesFromServer(anyString(), anyInt());
        verify(client, never()).requestNextBundleFromServer(Mockito.any(Bundle.class));
    }

    @Test
    void GivenSecondPatientJoinsInFeb_ShouldReturnOnlyOneRowsForThatPatientInDateRangesUnderContract() throws ExecutionException, InterruptedException {
        Bundle bundle1 = bundle.copy();

        // add 2nd patient
        List<BundleEntryComponent> entries = bundle1.getEntry();
        entries.add(createBundleEntry("ccw_patient_001", "mbi1", YEAR));

        when(client.requestPartDEnrolleesFromServer(anyString(), anyInt()))
                .thenReturn(bundle, bundle1);

        ContractBeneficiaries response = cut.getPatients(contractNumber, Month.FEBRUARY.getValue(), tracker);

        Map<String, ContractBeneficiaries.PatientDTO> patients = response.getPatients();
        assertThat(patients.size(), is(2));

        // 1st patient has 2 rows in date ranges under contract
        ContractBeneficiaries.PatientDTO patient0 = patients.entrySet().stream()
                .map(Map.Entry::getValue)
                .filter(c -> c.getBeneficiaryId().equalsIgnoreCase("ccw_patient_000"))
                .findFirst().get();
        assertThat(patient0.getDateRangesUnderContract().size(), is(2));

        // 2nd patient has 1 row in date ranges under contract
        ContractBeneficiaries.PatientDTO patient1 = patients.entrySet().stream()
                .map(Map.Entry::getValue)
                .filter(c -> c.getBeneficiaryId().equalsIgnoreCase("ccw_patient_001"))
                .findFirst().get();
        assertThat(patient1.getDateRangesUnderContract().size(), is(1));

        verify(client, times(2)).requestPartDEnrolleesFromServer(anyString(), anyInt());
        verify(client, never()).requestNextBundleFromServer(Mockito.any(Bundle.class));
    }

    @Test
    void GivenAPatientLeavesInFeb_ShouldReturnOnlyOneRowsForThatPatientInDateRangesUnderContract() throws ExecutionException, InterruptedException {
        Bundle bundle1 = bundle.copy();

        // bundle1 has 2 patients in January
        List<BundleEntryComponent> entries = bundle1.getEntry();
        entries.add(createBundleEntry("ccw_patient_001", "mbi1", YEAR));

        // bundle2  has 1 patient in February, coz patient_001 left and is no longer active in contract
        Bundle bundle2 = bundle.copy();

        when(client.requestPartDEnrolleesFromServer(anyString(), anyInt()))
                .thenReturn(bundle1, bundle2);

        ContractBeneficiaries response = cut.getPatients(contractNumber, Month.FEBRUARY.getValue(), tracker);
        assertEquals(30, tracker.getPercentageCompleted());

        Collection<ContractBeneficiaries.PatientDTO> patients = response.getPatients().values();
        assertThat(patients.size(), is(2));

        for (ContractBeneficiaries.PatientDTO patient : patients) {
            if (patient.getBeneficiaryId().equalsIgnoreCase("ccw_patient_000")) {
                assertThat(patient.getDateRangesUnderContract().size(), is(2));
            } else if (patient.getBeneficiaryId().equalsIgnoreCase("ccw_patient_001")) {
                assertThat(patient.getDateRangesUnderContract().size(), is(1));
            } else {
                fail("Invalid patient ID: " + patient.getBeneficiaryId());
            }
        }
        verify(client, times(2)).requestPartDEnrolleesFromServer(anyString(), anyInt());
        verify(client, never()).requestNextBundleFromServer(Mockito.any(Bundle.class));
    }

    @Test
    void GivenTwoPatientsActiveInJanAndFeb_ShouldReturnTwoPatientRowsEachWithTwoRowsInDateRangesUnderContract() throws ExecutionException, InterruptedException {
        List<BundleEntryComponent> entries = bundle.getEntry();
        entries.add(createBundleEntry("ccw_patient_001", "mbi1", YEAR));

        ContractBeneficiaries response = cut.getPatients(contractNumber, Month.FEBRUARY.getValue(), tracker);

        Collection<ContractBeneficiaries.PatientDTO> patients = response.getPatients().values();
        assertThat(patients.size(), is(2));

        ContractBeneficiaries.PatientDTO patient0 = patients.stream()
                .filter(c -> c.getBeneficiaryId().equalsIgnoreCase("ccw_patient_000")).findFirst().get();
        assertThat(patient0.getDateRangesUnderContract().size(), is(2));

        ContractBeneficiaries.PatientDTO patient1 = patients.stream()
                .filter(c -> c.getBeneficiaryId().equalsIgnoreCase("ccw_patient_001")).findFirst().get();
        assertThat(patient1.getDateRangesUnderContract().size(), is(2));

        verify(client, times(2)).requestPartDEnrolleesFromServer(anyString(), anyInt());
        verify(client, never()).requestNextBundleFromServer(Mockito.any(Bundle.class));
    }

    @Test
    void GivenMultiplePages_ShouldProcessAllPages() throws ExecutionException, InterruptedException {
        Bundle bundle1 = bundle.copy();

        List<BundleEntryComponent> entries = bundle1.getEntry();
        entries.add(createBundleEntry("ccw_patient_001", "mbi1", YEAR));
        bundle1.addLink(addNextLink());

        Bundle bundle2 = createBundle("ccw_patient_002", "mbi2", YEAR);

        when(client.requestPartDEnrolleesFromServer(anyString(), anyInt())).thenReturn(bundle1);
        when(client.requestNextBundleFromServer(Mockito.any(Bundle.class))).thenReturn(bundle2);

        Map<String, ContractBeneficiaries.PatientDTO> map = cut.getPatients(contractNumber, Month.JANUARY.getValue(), tracker).getPatients();
        assertEquals(30, tracker.getPercentageCompleted());

        Collection<ContractBeneficiaries.PatientDTO> patients = map.values();
        assertThat(patients.size(), is(3));

        for (ContractBeneficiaries.PatientDTO patient : patients) {
            if (patient.getBeneficiaryId().equalsIgnoreCase("ccw_patient_000")) {
                assertThat(patient.getDateRangesUnderContract().size(), is(1));
            } else if (patient.getBeneficiaryId().equalsIgnoreCase("ccw_patient_001")) {
                assertThat(patient.getDateRangesUnderContract().size(), is(1));
            } else if (patient.getBeneficiaryId().equalsIgnoreCase("ccw_patient_002")) {
                assertThat(patient.getDateRangesUnderContract().size(), is(1));
            } else {
                fail("Invalid patient ID: " + patient.getBeneficiaryId());
            }
        }

        verify(client, times(1)).requestPartDEnrolleesFromServer(anyString(), anyInt());
        verify(client).requestNextBundleFromServer(Mockito.any(Bundle.class));
    }

    @Test
    void GivenDuplicatePatientRowsFromBFD_ShouldEliminateDuplicates() throws ExecutionException, InterruptedException {
        List<BundleEntryComponent> entries = bundle.getEntry();
        entries.add(createBundleEntry("ccw_patient_001", "mbi1", YEAR));
        //simulate duplicate patient record coming from BDF
        entries.add(createBundleEntry("ccw_patient_001", "mbi1", YEAR));

        when(client.requestPartDEnrolleesFromServer(anyString(), anyInt())).thenReturn(bundle);

        Collection<ContractBeneficiaries.PatientDTO> patients = cut.getPatients(contractNumber, Month.JANUARY.getValue(), tracker).getPatients().values();
        assertThat(patients.size(), is(2));

        for (ContractBeneficiaries.PatientDTO patient : patients) {
            if (patient.getBeneficiaryId().equalsIgnoreCase("ccw_patient_000")) {
                assertThat(patient.getDateRangesUnderContract().size(), is(1));
            } else if (patient.getBeneficiaryId().equalsIgnoreCase("ccw_patient_001")) {
                assertThat(patient.getDateRangesUnderContract().size(), is(1));
            } else {
                fail("Invalid patient ID: " + patient.getBeneficiaryId());
            }
        }

        verify(client, times(1)).requestPartDEnrolleesFromServer(anyString(), anyInt());
        verify(client, never()).requestNextBundleFromServer(Mockito.any(Bundle.class));
    }

    @Test
    @DisplayName("when call to BFD API throws Invalid Request exception, throws Exception")
    void whenBfdCallThrowsInvalidRequestException_ShouldThrowRuntimeException() {
        when(client.requestPartDEnrolleesFromServer(anyString(), anyInt()))
                .thenThrow(new InvalidRequestException("Request is invalid"));

        var exceptionThrown = assertThrows(ExecutionException.class,
                () -> cut.getPatients(contractNumber, currentMonth, tracker));

        assertThat(exceptionThrown.getMessage(), endsWith("Request is invalid"));
    }

    private Bundle createBundle() {
        BundleEntryComponent component = createBundleEntry("ccw_patient_000", "mbi0", YEAR);
        return BundleUtils.createBundle(component);
    }

    private Bundle createBundle(String id, String mbi, int year) {
        BundleEntryComponent component = createBundleEntry(id, mbi, year);
        return BundleUtils.createBundle(component);
    }

    private BundleLinkComponent addNextLink() {
        BundleLinkComponent linkComponent = new BundleLinkComponent();
        linkComponent.setRelation(Bundle.LINK_NEXT);
        return linkComponent;
    }
}