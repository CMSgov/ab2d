package gov.cms.ab2d.worker.adapter.bluebutton;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.eventlogger.EventLogger;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.events.ReloadEvent;
import gov.cms.ab2d.eventlogger.reports.sql.LoadObjects;
import gov.cms.ab2d.eventlogger.utils.UtilMethods;
import gov.cms.ab2d.worker.SpringBootApp;
import gov.cms.ab2d.worker.service.BeneficiaryService;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@SpringBootTest(classes = SpringBootApp.class)
@Testcontainers
public class LoadPatientsByContractLoggingTest {
    private static final String BENEFICIARY_ID = "https://bluebutton.cms.gov/resources/variables/bene_id";

    @Mock
    private BFDClient bfdClient;

    @Autowired
    private LoadObjects loadObjects;

    @Mock
    private ContractRepository contractRepo;

    @Autowired
    private BeneficiaryService beneficiaryService;

    @Autowired
    private PropertiesService propertiesService;

    @Autowired
    private EventLogger eventLogger;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @Test
    public void testLogging() {
        ContractAdapterImpl cai = new ContractAdapterImpl(bfdClient, contractRepo, beneficiaryService,
                propertiesService, eventLogger);

        String contractId = "C1234";
        Bundle bundle = createBundle();
        lenient().when(bfdClient.requestPartDEnrolleesFromServer(anyString(), anyInt())).thenReturn(bundle);
        Contract contract = new Contract();
        contract.setContractNumber(contractId);
        contract.setAttestedOn(OffsetDateTime.MIN);
        contract.setContractName("1234");
        contract.setId(1L);
        contract.setCoverages(new HashSet<>());
        lenient().when(contractRepo.findContractByContractNumber(anyString())).thenReturn(java.util.Optional.of(contract));
        cai.getPatients(contractId, 1);

        List<LoggableEvent> reloadEvents = loadObjects.loadAllReloadEvent();
        assertEquals(1, reloadEvents.size());
        ReloadEvent reloadEvent = (ReloadEvent) reloadEvents.get(0);
        assertEquals(ReloadEvent.FileType.CONTRACT_MAPPING, reloadEvent.getFileType());
        assertEquals(1, reloadEvent.getNumberLoaded());

        assertTrue(UtilMethods.allEmpty(
                loadObjects.loadAllApiRequestEvent(),
                loadObjects.loadAllApiResponseEvent(),
                loadObjects.loadAllContractBeneSearchEvent(),
                loadObjects.loadAllErrorEvent(),
                loadObjects.loadAllFileEvent(),
                loadObjects.loadAllJobStatusChangeEvent()
        ));
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

    private Bundle.BundleEntryComponent createBundleEntry(String patientId) {
        var component = new Bundle.BundleEntryComponent();
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
