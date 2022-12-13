package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.repository.PdpClientRepository;
import gov.cms.ab2d.common.service.FeatureEngagement;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.AB2DSQSMockConfig;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobStatus;
import gov.cms.ab2d.job.repository.JobRepository;
import gov.cms.ab2d.job.service.JobCleanup;
import gov.cms.ab2d.job.service.JobService;
import gov.cms.ab2d.properties.service.PropertiesAPIService;
import gov.cms.ab2d.worker.config.JobHandler;
import java.time.OffsetDateTime;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import static gov.cms.ab2d.common.util.Constants.NDJSON_FIRE_CONTENT_TYPE;
import static gov.cms.ab2d.common.util.PropertyConstants.WORKER_ENGAGEMENT;
import static gov.cms.ab2d.fhir.BundleUtils.EOB;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Generic Tests to make sure that the worker gets triggered upon submitting a job into the Job table.
 */
@SpringBootTest
@Testcontainers
@Import(AB2DSQSMockConfig.class)
class WorkerServiceDisengagementTest extends JobCleanup {
    private final Random random = new Random();

    @Autowired private DataSetup dataSetup;
    @Autowired private JobRepository jobRepository;
    @Autowired private PdpClientRepository pdpClientRepository;
    @Autowired private PropertiesAPIService propertiesApiService;
    @Autowired private JobService jobService;

    @Autowired private WorkerServiceImpl workerServiceImpl;
    @Autowired private JobHandler jobHandler;

    private WorkerServiceStub workerServiceStub;

    @SuppressWarnings("rawtypes")
    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @BeforeEach
    public void init() {
        workerServiceStub = new WorkerServiceStub(jobService, propertiesApiService);

        ReflectionTestUtils.setField(jobHandler, "workerService", workerServiceStub);
    }

    @AfterEach
    public void jobCleanup() {
        ReflectionTestUtils.setField(jobHandler, "workerService", workerServiceImpl);
        setEngagement(FeatureEngagement.IN_GEAR);

        dataSetup.cleanup();
    }

    private void setEngagement(FeatureEngagement drive) {
        try {
            String engagement = propertiesApiService.getProperty(WORKER_ENGAGEMENT);
        } catch (Exception ex) {
            throw new IllegalStateException(WORKER_ENGAGEMENT + " must be set.");
        }

        propertiesApiService.updateProperty(WORKER_ENGAGEMENT, drive.getSerialValue());
    }

    @Test
    @DisplayName("Disengagement in database is checked by worker service impl")
    void checkEngagementUsed() {
        setEngagement(FeatureEngagement.NEUTRAL);

        FeatureEngagement shouldBeNeutral = workerServiceImpl.getEngagement();
        assertEquals(FeatureEngagement.NEUTRAL, shouldBeNeutral);
    }

    @Test
    @DisplayName("When a job is submitted into the job table, a disengaged worker never processes it")
    void whenJobSubmittedWorkerGetsTriggered() throws InterruptedException {

        setEngagement(FeatureEngagement.NEUTRAL);

        Contract contract = dataSetup.setupContract("TST-12");
        final PdpClient pdpClient = createClient(contract);
        createJob(pdpClient, contract);

        Thread.sleep(6000L);

        assertEquals(0, workerServiceStub.processingCalls);

        // Now confirm that switching workers back on ... works!
        setEngagement(FeatureEngagement.IN_GEAR);

        Thread.sleep(6000L);
        assertEquals(1, workerServiceStub.processingCalls);
    }

    @Test
    @DisplayName("When multiple jobs are submitted into the job table, they are processed in parallel by the workers")
    void whenTwoJobsSubmittedWorkerGetsTriggeredProcessesBothInParallel() throws InterruptedException {

        setEngagement(FeatureEngagement.NEUTRAL);

        Contract contract = dataSetup.setupContract("TST-12");
        Contract contract1 = dataSetup.setupContract("TST-34");

        createJob(createClient(contract), contract);
        createJob(createClient2(contract1), contract1);

        // There is a 5 second sleep in the WorkerService.
        // So if the result for two jobs comes before 10 seconds, it implies they were not processed sequentially
        Thread.sleep(10000L);

        assertEquals(0, workerServiceStub.processingCalls);

        // Now confirm that switching workers back on ... works!
        setEngagement(FeatureEngagement.IN_GEAR);

        Thread.sleep(10000L);

        assertEquals(2, workerServiceStub.processingCalls);
    }

    private Job createJob(final PdpClient pdpClient, Contract contract) {
        Job job = new Job();
        job.setId((long) getIntRandom());
        job.setJobUuid(UUID.randomUUID().toString());
        job.setStatus(JobStatus.SUBMITTED);
        job.setStatusMessage("0%");
        job.setResourceTypes(EOB);
        job.setCreatedAt(OffsetDateTime.now());
        job.setOrganization(pdpClient.getOrganization());
        job.setOutputFormat(NDJSON_FIRE_CONTENT_TYPE);
        job.setContractNumber(contract.getContractNumber());
        job.setFhirVersion(STU3);

        job = jobRepository.save(job);
        addJobForCleanup(job);
        return job;
    }

    private PdpClient createClient(Contract contract) {
        PdpClient pdpClient = new PdpClient();
        int clientNum = getIntRandom();
        pdpClient.setId((long) clientNum);
        pdpClient.setClientId("testclient" + clientNum);
        pdpClient.setOrganization("testclient" + clientNum);
        pdpClient.setEnabled(true);
        pdpClient.setContractId(contract.getId());

        pdpClient = pdpClientRepository.save(pdpClient);
        dataSetup.queueForCleanup(pdpClient);
        return pdpClient;
    }

    private PdpClient createClient2(Contract contract) {
        PdpClient pdpClient = new PdpClient();
        int clientNum = getIntRandom();
        pdpClient.setId((long) clientNum);
        pdpClient.setClientId("testclient2" + clientNum);
        pdpClient.setOrganization("testclient2" + clientNum);
        pdpClient.setEnabled(true);
        pdpClient.setContractId(contract.getId());

        pdpClient =  pdpClientRepository.save(pdpClient);
        dataSetup.queueForCleanup(pdpClient);
        return pdpClient;
    }

    private int getIntRandom() {
        return random.nextInt(100);
    }
}