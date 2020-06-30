package gov.cms.ab2d.worker.adapter.bluebutton;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.eventlogger.LogManager;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleLinkComponent;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Instant;
import java.time.Month;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import static gov.cms.ab2d.worker.processor.BundleUtils.BENEFICIARY_ID;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractBeneSearchTest {

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
        cut = new ContractBeneSearchImpl(client, eventLogger, taskExecutor);

        bundle = createBundle();
        lenient().when(client.requestPartDEnrolleesFromServer(anyString(), anyInt())).thenReturn(bundle);

        Contract contract = new Contract();
        contract.setId(Long.valueOf(Instant.now().getNano()));
        contract.setContractNumber(contractNumber);
    }

    @Test
    @DisplayName("given contractNumber, get patients from BFD API")
    void GivenContractNumber_ShouldReturnPatients() throws ExecutionException, InterruptedException {
        var response = cut.getPatients(contractNumber, currentMonth);

        assertThat(response, notNullValue());
        assertThat(response.getContractNumber(), is(contractNumber));

        var patients = response.getPatients();
        assertThat(patients.size(), is(1));
        verify(client, times(3)).requestPartDEnrolleesFromServer(anyString(), anyInt());
        verify(client, never()).requestNextBundleFromServer(Mockito.any(Bundle.class));
    }

    @Test
    void GivenPatientActiveInJanuary_ShouldReturnOneRowInDateRangesUnderContract() throws ExecutionException, InterruptedException {
        var response = cut.getPatients(contractNumber, Month.JANUARY.getValue());

        var patients = response.getPatients();
        assertThat(patients.size(), is(1));

        var patient0 = patients.get(0);
        assertThat(patient0.getPatientId(), is("ccw_patient_000"));
        assertThat(patient0.getDateRangesUnderContract().size(), is(1));
        verify(client).requestPartDEnrolleesFromServer(anyString(), anyInt());
        verify(client, never()).requestNextBundleFromServer(Mockito.any(Bundle.class));
    }

    @Test
    void GivenPatientActiveInJanAndFeb_ShouldReturnTwoRowsInDateRangesUnderContract() throws ExecutionException, InterruptedException {
        var response = cut.getPatients(contractNumber, Month.FEBRUARY.getValue());

        var patient0 = response.getPatients().get(0);
        assertThat(patient0.getPatientId(), is("ccw_patient_000"));
        assertThat(patient0.getDateRangesUnderContract().size(), is(2));
        verify(client, times(2)).requestPartDEnrolleesFromServer(anyString(), anyInt());
        verify(client, never()).requestNextBundleFromServer(Mockito.any(Bundle.class));
    }

    @Test
    void GivenPatientActiveInMonth1And3ButNot2_ShouldReturnTwoRowsInDateRangesUnderContract() throws ExecutionException, InterruptedException {
        var bundle1 = bundle.copy();
        // add 2nd patient
        var entries = bundle1.getEntry();
        entries.add(createBundleEntry("ccw_patient_001"));

        when(client.requestPartDEnrolleesFromServer("S0000", 1))
                .thenReturn(bundle1);    // January - patient1 is active
        when(client.requestPartDEnrolleesFromServer("S0000", 2))
                .thenReturn(bundle);    // January - patient1 is active
        when(client.requestPartDEnrolleesFromServer("S0000", 3))
                .thenReturn(bundle1);    // January - patient1 is active

        var response = cut.getPatients(contractNumber, Month.MARCH.getValue());

        //expect patient0 to be active in all 3 months
        var patient0 = response.getPatients().get(0);
        assertThat(patient0.getPatientId(), is("ccw_patient_000"));
        assertThat(patient0.getDateRangesUnderContract().size(), is(3));

        //expect patient1 to be active in only 2 months
        var patient1 = response.getPatients().get(1);
        assertThat(patient1.getPatientId(), is("ccw_patient_001"));
        assertThat(patient1.getDateRangesUnderContract().size(), is(2));

        var dateRangesUnderContract = patient1.getDateRangesUnderContract();

        //month is January
        assertThat(dateRangesUnderContract.get(0).getStart().getMonth(), is(Calendar.JANUARY));
        assertThat(dateRangesUnderContract.get(0).getEnd().getMonth(), is(Calendar.JANUARY));

        //month is March
        assertThat(dateRangesUnderContract.get(1).getStart().getMonth(), is(Calendar.MARCH));
        assertThat(dateRangesUnderContract.get(1).getEnd().getMonth(), is(Calendar.MARCH));

        verify(client, times(3)).requestPartDEnrolleesFromServer(anyString(), anyInt());
        verify(client, never()).requestNextBundleFromServer(Mockito.any(Bundle.class));
    }

    @Test
    void GivenSecondPatientJoinsInFeb_ShouldReturnOnlyOneRowsForThatPatientInDateRangesUnderContract() throws ExecutionException, InterruptedException {
        var bundle1 = bundle.copy();

        // add 2nd patient
        var entries = bundle1.getEntry();
        entries.add(createBundleEntry("ccw_patient_001"));

        when(client.requestPartDEnrolleesFromServer(anyString(), anyInt()))
                .thenReturn(bundle, bundle1);

        var response = cut.getPatients(contractNumber, Month.FEBRUARY.getValue());

        var patients = response.getPatients();
        assertThat(patients.size(), is(2));

        //1st patient has 2 rows in date ranges under contract
        var patient0 = patients.get(0);
        assertThat(patient0.getPatientId(), is("ccw_patient_000"));
        assertThat(patient0.getDateRangesUnderContract().size(), is(2));

        //2nd patient has 1 row in date ranges under contract
        var patient1 = patients.get(1);
        assertThat(patient1.getPatientId(), is("ccw_patient_001"));
        assertThat(patient1.getDateRangesUnderContract().size(), is(1));

        verify(client, times(2)).requestPartDEnrolleesFromServer(anyString(), anyInt());
        verify(client, never()).requestNextBundleFromServer(Mockito.any(Bundle.class));
    }

    @Test
    void GivenAPatientLeavesInFeb_ShouldReturnOnlyOneRowsForThatPatientInDateRangesUnderContract() throws ExecutionException, InterruptedException {
        var bundle1 = bundle.copy();

        //bundle1 has 2 patients in January
        var entries = bundle1.getEntry();
        entries.add(createBundleEntry("ccw_patient_001"));

        //bundle2  has 1 patient in February, coz patient_001 left and is no longer active in contract
        var bundle2 = bundle.copy();

        when(client.requestPartDEnrolleesFromServer(anyString(), anyInt()))
                .thenReturn(bundle1, bundle2);

        var response = cut.getPatients(contractNumber, Month.FEBRUARY.getValue());

        var patients = response.getPatients();
        assertThat(patients.size(), is(2));

        var patient0 = patients.get(0);
        assertThat(patient0.getPatientId(), is("ccw_patient_000"));
        assertThat(patient0.getDateRangesUnderContract().size(), is(2));

        var patient1 = patients.get(1);
        assertThat(patient1.getPatientId(), is("ccw_patient_001"));
        assertThat(patient1.getDateRangesUnderContract().size(), is(1));

        verify(client, times(2)).requestPartDEnrolleesFromServer(anyString(), anyInt());
        verify(client, never()).requestNextBundleFromServer(Mockito.any(Bundle.class));
    }

    @Test
    void GivenTwoPatientsActiveInJanAndFeb_ShouldReturnTwoPatientRowsEachWithTwoRowsInDateRangesUnderContract() throws ExecutionException, InterruptedException {
        var entries = bundle.getEntry();
        entries.add(createBundleEntry("ccw_patient_001"));

        var response = cut.getPatients(contractNumber, Month.FEBRUARY.getValue());

        var patients = response.getPatients();
        assertThat(patients.size(), is(2));

        var patient0 = patients.get(0);
        assertThat(patient0.getPatientId(), is("ccw_patient_000"));
        assertThat(patient0.getDateRangesUnderContract().size(), is(2));

        var patient1 = patients.get(1);
        assertThat(patient1.getPatientId(), is("ccw_patient_001"));
        assertThat(patient1.getDateRangesUnderContract().size(), is(2));

        verify(client, times(2)).requestPartDEnrolleesFromServer(anyString(), anyInt());
        verify(client, never()).requestNextBundleFromServer(Mockito.any(Bundle.class));
    }

    @Test
    void GivenMultiplePages_ShouldProcessAllPages() throws ExecutionException, InterruptedException {
        var bundle1 = bundle.copy();

        var entries = bundle1.getEntry();
        entries.add(createBundleEntry("ccw_patient_001"));
        bundle1.addLink(addNextLink());

        var bundle2 = createBundle("ccw_patient_002");

        when(client.requestPartDEnrolleesFromServer(anyString(), anyInt())).thenReturn(bundle1);
        when(client.requestNextBundleFromServer(Mockito.any(Bundle.class))).thenReturn(bundle2);

        var response = cut.getPatients(contractNumber, Month.JANUARY.getValue());

        var patients = response.getPatients();
        assertThat(patients.size(), is(3));

        var patient0 = patients.get(0);
        assertThat(patient0.getPatientId(), is("ccw_patient_000"));
        assertThat(patient0.getDateRangesUnderContract().size(), is(1));

        var patient1 = patients.get(1);
        assertThat(patient1.getPatientId(), is("ccw_patient_001"));
        assertThat(patient1.getDateRangesUnderContract().size(), is(1));

        var patient2 = patients.get(2);
        assertThat(patient2.getPatientId(), is("ccw_patient_002"));
        assertThat(patient2.getDateRangesUnderContract().size(), is(1));

        verify(client, times(1)).requestPartDEnrolleesFromServer(anyString(), anyInt());
        verify(client).requestNextBundleFromServer(Mockito.any(Bundle.class));
    }

    @Test
    void GivenDuplicatePatientRowsFromBFD_ShouldEliminateDuplicates() throws ExecutionException, InterruptedException {
        var entries = bundle.getEntry();
        entries.add(createBundleEntry("ccw_patient_001"));
        //simulate duplicate patient record coming from BDF
        entries.add(createBundleEntry("ccw_patient_001"));

        when(client.requestPartDEnrolleesFromServer(anyString(), anyInt())).thenReturn(bundle);

        var response = cut.getPatients(contractNumber, Month.JANUARY.getValue());

        var patients = response.getPatients();
        assertThat(patients.size(), is(2));

        var patient0 = patients.get(0);
        assertThat(patient0.getPatientId(), is("ccw_patient_000"));
        assertThat(patient0.getDateRangesUnderContract().size(), is(1));

        var patient1 = patients.get(1);
        assertThat(patient1.getPatientId(), is("ccw_patient_001"));
        assertThat(patient1.getDateRangesUnderContract().size(), is(1));

        verify(client, times(1)).requestPartDEnrolleesFromServer(anyString(), anyInt());
        verify(client, never()).requestNextBundleFromServer(Mockito.any(Bundle.class));
    }

    @Test
    @DisplayName("when call to BFD API throws Invalid Request exception, throws Exception")
    void whenBfdCallThrowsInvalidRequestException_ShouldThrowRuntimeException() {
        when(client.requestPartDEnrolleesFromServer(anyString(), anyInt()))
                .thenThrow(new InvalidRequestException("Request is invalid"));

        var exceptionThrown = assertThrows(ExecutionException.class,
                () -> cut.getPatients(contractNumber, currentMonth));

        assertThat(exceptionThrown.getMessage(), endsWith("Request is invalid"));
    }

    private Bundle createBundle() {
        return createBundle("ccw_patient_000");
    }

    private Bundle createBundle(final String patientId) {
        var bundle = new Bundle();
        var entries = bundle.getEntry();
        entries.add(createBundleEntry(patientId));
        return bundle;
    }

    private BundleEntryComponent createBundleEntry(String patientId) {
        var component = new BundleEntryComponent();
        component.setResource(createPatient(patientId));
        return component;
    }

    private Patient createPatient(String patientId) {
        var patient = new Patient();
        patient.getIdentifier().add(createIdentifier(patientId));
        return patient;
    }

    private Identifier createIdentifier(String patientId) {
        var identifier = new Identifier();
        identifier.setSystem(BENEFICIARY_ID);
        identifier.setValue(patientId);
        return identifier;
    }

    private BundleLinkComponent addNextLink() {
        var linkComponent = new BundleLinkComponent();
        linkComponent.setRelation(Bundle.LINK_NEXT);
        return linkComponent;
    }
}