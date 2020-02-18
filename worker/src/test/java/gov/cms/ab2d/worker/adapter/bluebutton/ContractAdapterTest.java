package gov.cms.ab2d.worker.adapter.bluebutton;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import gov.cms.ab2d.bfd.client.BFDClient;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class ContractAdapterTest {

    private static final String BENEFICIARY_ID = "https://bluebutton.cms.gov/resources/variables/bene_id";

    @Mock
    private BFDClient client;

    private ContractAdapter cut;
    private String contractNumber = "S0000";
    private int currentMonth;


    @BeforeEach
    void setUp() {
        cut = new ContractAdapterImpl(client);

        currentMonth = LocalDate.now().getMonthValue();
        var bundle = new Bundle();
        var entries = bundle.getEntry();
        entries.add(createBundleEntry("ccw_patient_000"));
        entries.add(createBundleEntry("ccw_patient_001"));

        when(client.requestPartDEnrolleesFromServer(anyString(), anyInt())).thenReturn(bundle);
    }



    @Test
    @DisplayName("given contractNumber, get patients from BFD API")
    void GivenContractNumber_ShouldReturnPatients() {
        var response = cut.getPatients(contractNumber, currentMonth);

        Assert.assertThat(response, notNullValue());
        Assert.assertThat(response.getContractNumber(), is(contractNumber));
    }


    @Test
    @DisplayName("when call to BFD API throws Invalid Request exception, throws Exception")
    void whenBfdCallThrowsInvalidRequestException_ShouldThrowRuntimeException() {
        when(client.requestPartDEnrolleesFromServer(anyString(), anyInt()))
                .thenThrow(new InvalidRequestException("Request is invalid"));

        var exceptionThrown = assertThrows(RuntimeException.class,
                () -> cut.getPatients(contractNumber, currentMonth));

        assertThat(exceptionThrown.getMessage(), endsWith("Request is invalid"));
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

}