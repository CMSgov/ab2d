package gov.cms.ab2d.worker.adapter.bluebutton;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.eventloggers.kinesis.KinesisEventLogger;
import gov.cms.ab2d.eventlogger.eventloggers.sql.SqlEventLogger;
import gov.cms.ab2d.eventlogger.events.*;
import gov.cms.ab2d.eventlogger.reports.sql.DoAll;
import gov.cms.ab2d.eventlogger.utils.UtilMethods;
import gov.cms.ab2d.worker.SpringBootApp;
import gov.cms.ab2d.worker.processor.eob.ProgressTracker;
import org.hl7.fhir.dstu3.model.Bundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static gov.cms.ab2d.worker.processor.eob.BundleUtils.createBundleEntry;
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

    @Value("${patient.contract.year}")
    private int year;

    @Mock
    private KinesisEventLogger kinesisEventLogger;

    private LogManager logManager;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

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
        ContractBeneSearchImpl cai = new ContractBeneSearchImpl(bfdClient, logManager, patientContractThreadPool, false);

        String contractId = "C1234";
        Bundle bundle = createBundle();
        lenient().when(bfdClient.requestPartDEnrolleesFromServer(anyString(), anyInt())).thenReturn(bundle);
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
        var bundle = new Bundle();
        var entries = bundle.getEntry();
        entries.add(createBundleEntry("ccw_patient_000", "mbi0", year));
        return bundle;
    }
}
