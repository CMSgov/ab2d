package gov.cms.ab2d.worker.processor.eob;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriver;
import gov.cms.ab2d.worker.service.FileService;
import org.hl7.fhir.dstu3.model.Bundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.concurrent.ExecutionException;

import static gov.cms.ab2d.common.util.Constants.NDJSON_FIRE_CONTENT_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@Transactional
public class ProgressTrackerIntegrationTest {

    private JobProcessorImpl cut;
    @Autowired
    private FileService fileService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ContractProcessor contractProcessor;

    @Autowired
    private CoverageDriver coverageDriver;

    @Autowired
    private JobOutputRepository jobOutputRepository;

    @Value("${patient.contract.year}")
    private int year;

    @Mock
    private LogManager eventLogger;

    @Mock
    BFDClient bfdClient;

    ProgressTracker progressTracker;

    @Autowired
    @Qualifier("patientContractThreadPool")
    private ThreadPoolTaskExecutor patientContractThreadPool;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @BeforeEach
    void init() {
        patientContractThreadPool = new ThreadPoolTaskExecutor();
        patientContractThreadPool.setCorePoolSize(6);
        patientContractThreadPool.setMaxPoolSize(12);
        patientContractThreadPool.setThreadNamePrefix("contractp-");
        patientContractThreadPool.initialize();
        cut = new JobProcessorImpl(fileService, jobRepository, jobOutputRepository,
                contractProcessor, coverageDriver, eventLogger);
    }

    @Test
    void testIt() throws ExecutionException, InterruptedException {
        int month = 2;
        String contractId = "C0001";
        Contract contract = new Contract();
        contract.setContractNumber(contractId);

        Job job = createJob(null);
        job.setContract(contract);

        progressTracker = ProgressTracker.builder()
                .failureThreshold(2)
                .jobUuid(job.getJobUuid())
                .numContracts(1)
                .currentMonth(2)
                .build();

        Bundle.BundleEntryComponent entry1 = BundleUtils.createBundleEntry("P1", "mbi1", year);
        Bundle.BundleEntryComponent entry2 = BundleUtils.createBundleEntry("P2", "mbi2", year);
        Bundle.BundleEntryComponent entry3 = BundleUtils.createBundleEntry("P3", "mbi3", year);
        Bundle.BundleEntryComponent entry4 = BundleUtils.createBundleEntry("P4", "mbi4", year);

        Bundle bundleA = BundleUtils.createBundle(entry1, entry2, entry3);
        Bundle bundleB = BundleUtils.createBundle(entry1, entry2, entry3, entry4);

        when(bfdClient.requestPartDEnrolleesFromServer(contractId, 1)).thenReturn(bundleA);
        when(bfdClient.requestPartDEnrolleesFromServer(contractId, 2)).thenReturn(bundleB);

        cut.processContractBenes(job, month, progressTracker);

        Job loadedVal = jobRepository.findById(job.getId()).get();
        assertEquals(30, loadedVal.getProgress());
    }

    private Job createJob(User user) {
        Job job = new Job();
        job.setJobUuid("S0000");
        job.setStatus(JobStatus.SUBMITTED);
        job.setStatusMessage("0%");
        job.setUser(user);
        job.setOutputFormat(NDJSON_FIRE_CONTENT_TYPE);
        job.setCreatedAt(OffsetDateTime.now());
        return jobRepository.save(job);
    }
}
