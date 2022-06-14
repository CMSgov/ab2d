package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.repository.PdpClientRepository;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.util.AB2DLocalstackContainer;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobStatus;
import gov.cms.ab2d.job.repository.JobRepository;
import gov.cms.ab2d.job.service.JobCleanup;
import gov.cms.ab2d.job.service.JobService;
import gov.cms.ab2d.worker.config.JobHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.Random;
import java.util.UUID;

import static gov.cms.ab2d.common.util.Constants.NDJSON_FIRE_CONTENT_TYPE;
import static gov.cms.ab2d.fhir.BundleUtils.EOB;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Generic Tests to make sure that the worker gets triggered upon submitting a job into the Job table.
 */
@SpringBootTest
@Testcontainers
class WorkerServiceTest extends JobCleanup {
    private final Random random = new Random();

    @Autowired private DataSetup dataSetup;
    @Autowired private JobRepository jobRepository;
    @Autowired private PdpClientRepository pdpClientRepository;
    @Autowired private JobService jobService;
    @Autowired private PropertiesService propertiesService;

    @Autowired private WorkerServiceImpl workerServiceImpl;
    @Autowired private JobHandler jobHandler;

    @Container
    private static final AB2DLocalstackContainer localstackContainer = new AB2DLocalstackContainer();

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    private WorkerServiceStub workerServiceStub;

    @BeforeEach
    public void init() {
        workerServiceStub = new WorkerServiceStub(jobService, propertiesService);

        ReflectionTestUtils.setField(jobHandler, "workerService", workerServiceStub);
    }

    @AfterEach
    public void jobCleanup() {
        ReflectionTestUtils.setField(jobHandler, "workerService", workerServiceImpl);

        dataSetup.cleanup();
    }

    @Test
    @DisplayName("When a job is submitted into the job table, a worker processes it")
    void whenJobSubmittedWorkerGetsTriggered() throws InterruptedException {

        final PdpClient pdpClient = createClient();
        createJob(pdpClient);

        Thread.sleep(6000L);

        assertEquals(1, workerServiceStub.processingCalls);
    }

    @Test
    @DisplayName("When multiple jobs are submitted into the job table, they are processed in parallel by the workers")
    void whenTwoJobsSubmittedWorkerGetsTriggeredProcessesBothInParallel() throws InterruptedException {

        createJob(createClient());
        createJob(createClient2());

        // There is a 5 second sleep in the WorkerService.
        // So if the result for two jobs comes before 10 seconds, it implies they were not processed sequentially
        Thread.sleep(10000L);

        assertEquals(2, workerServiceStub.processingCalls);
    }

    private Job createJob(final PdpClient pdpClient) {
        Job job = new Job();
        job.setId((long) getIntRandom());
        job.setJobUuid(UUID.randomUUID().toString());
        job.setStatus(JobStatus.SUBMITTED);
        job.setStatusMessage("0%");
        job.setResourceTypes(EOB);
        job.setCreatedAt(OffsetDateTime.now());
        job.setOrganization(pdpClient.getOrganization());
        job.setOutputFormat(NDJSON_FIRE_CONTENT_TYPE);
        job.setContractNumber(pdpClient.getContract().getContractNumber());
        job.setFhirVersion(STU3);

        job = jobRepository.save(job);
        addJobForCleanup(job);
        return job;
    }

    private PdpClient createClient() {
        PdpClient pdpClient = new PdpClient();
        int clientNum = getIntRandom();
        pdpClient.setId((long) clientNum);
        pdpClient.setClientId("testclient" + clientNum);
        pdpClient.setOrganization("testclient" + clientNum);
        pdpClient.setEnabled(true);
        pdpClient.setContract(dataSetup.setupContract("TST-12"));

        pdpClient = pdpClientRepository.save(pdpClient);
        dataSetup.queueForCleanup(pdpClient);
        return pdpClient;
    }

    private PdpClient createClient2() {
        PdpClient pdpClient = new PdpClient();
        int clientNum = getIntRandom();
        pdpClient.setId((long) clientNum);
        pdpClient.setClientId("testclient2" + clientNum);
        pdpClient.setOrganization("testclient2" + clientNum);
        pdpClient.setEnabled(true);
        pdpClient.setContract(dataSetup.setupContract("TST-34"));

        pdpClient = pdpClientRepository.save(pdpClient);
        dataSetup.queueForCleanup(pdpClient);
        return pdpClient;
    }

    private int getIntRandom() {
        return random.nextInt(100);
    }
}