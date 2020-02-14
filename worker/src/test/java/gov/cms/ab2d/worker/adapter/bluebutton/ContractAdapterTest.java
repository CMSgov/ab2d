package gov.cms.ab2d.worker.adapter.bluebutton;

import gov.cms.ab2d.bfd.client.BFDClient;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class ContractAdapterTest {

    @Mock
    private BFDClient client;

    private ContractAdapter cut;


    @BeforeEach
    void setUp() {
        cut = new ContractAdapterImpl(client);

        Bundle fakeBundle = new Bundle();
        Patient patient = new Patient();
        Bundle.BundleEntryComponent component = new Bundle.BundleEntryComponent();
        component.setResource(patient);
        // Creates new list;
        List<Bundle.BundleEntryComponent> entry = fakeBundle.getEntry();
        entry.add(component);

        when(client.requestPartDEnrolleesFromServer(anyString(), anyInt()))
                .thenReturn(fakeBundle);
    }

    @Test
    @DisplayName("given contractNumber, get patients from BFD API")
    void GivenContractNumber_ShouldReturnPatients() {
        final String contractNumber = "S0000";
        final GetPatientsByContractResponse response = cut.getPatients(contractNumber);

        Assert.assertThat(response, notNullValue());
        Assert.assertThat(response.getContractNumber(), is(contractNumber));
    }


}