package gov.cms.ab2d.worker.adapter.bluebutton;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.eventloggers.kinesis.KinesisEventLogger;
import gov.cms.ab2d.eventlogger.eventloggers.sql.SqlEventLogger;
import gov.cms.ab2d.eventlogger.events.*;
import gov.cms.ab2d.eventlogger.reports.sql.DoAll;
import gov.cms.ab2d.eventlogger.utils.UtilMethods;
import gov.cms.ab2d.worker.SpringBootApp;
import gov.cms.ab2d.worker.processor.domainmodel.ProgressTracker;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static gov.cms.ab2d.worker.processor.BundleUtils.BENEFICIARY_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@SpringBootTest(classes = SpringBootApp.class)
@Testcontainers
 class LoadPatientsByContractLoggingTest {
    private ProgressTracker tracker;

    @Mock
    private BFDClient bfdClient;

    @Autowired
    private DoAll doAll;

    @Autowired
    private SqlEventLogger sqlEventLogger;

    @Mock
    private KinesisEventLogger kinesisEventLogger;

    private LogManager logManager;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @BeforeEach
    void setUp() {
        logManager = new LogManager(sqlEventLogger, kinesisEventLogger);
        tracker = ProgressTracker.builder()
                .jobUuid("JOBID")
                .numContracts(1)
                .failureThreshold(1)
                .build();
    }

    @Test
    public void testLogging() throws ExecutionException, InterruptedException {
        ThreadPoolTaskExecutor patientContractThreadPool = new ThreadPoolTaskExecutor();
        patientContractThreadPool.setCorePoolSize(6);
        patientContractThreadPool.setMaxPoolSize(12);
        patientContractThreadPool.setThreadNamePrefix("contractp-");
        patientContractThreadPool.initialize();
        ContractBeneSearchImpl cai = new ContractBeneSearchImpl(bfdClient, logManager, patientContractThreadPool);

        String contractId = "C1234";
        Bundle bundle = createBundle();
        lenient().when(bfdClient.requestPartDEnrolleesFromServer(anyString(), anyInt())).thenReturn(bundle);
        Contract contract = new Contract();
        contract.setContractNumber(contractId);
        contract.setAttestedOn(OffsetDateTime.MIN);
        contract.setContractName("1234");
        contract.setId(1L);
        contract.setCoveragePeriods(new HashSet<>());
        tracker.setCurrentMonth(1);
        cai.getPatients(contractId, 1, tracker);

        List<LoggableEvent> reloadEvents = doAll.load(ReloadEvent.class);
        assertEquals(1, reloadEvents.size());
        ReloadEvent reloadEvent = (ReloadEvent) reloadEvents.get(0);
        assertEquals(ReloadEvent.FileType.CONTRACT_MAPPING, reloadEvent.getFileType());
        assertEquals(1, reloadEvent.getNumberLoaded());

        assertTrue(UtilMethods.allEmpty(
                doAll.load(ApiRequestEvent.class),
                doAll.load(ApiResponseEvent.class),
                doAll.load(ContractBeneSearchEvent.class),
                doAll.load(ErrorEvent.class),
                doAll.load(FileEvent.class),
                doAll.load(JobStatusChangeEvent.class)
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
