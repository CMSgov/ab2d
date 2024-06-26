package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.repository.PdpClientRepository;
import gov.cms.ab2d.common.service.ContractServiceStub;
import gov.cms.ab2d.common.util.AB2DLocalstackContainer;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.contracts.model.ContractDTO;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.eventclient.events.JobStatusChangeEvent;
import gov.cms.ab2d.eventclient.events.LoggableEvent;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobStatus;
import gov.cms.ab2d.job.repository.JobRepository;
import gov.cms.ab2d.job.service.JobCleanup;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriver;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriverException;
import gov.cms.ab2d.worker.service.ContractWorkerClient;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import static gov.cms.ab2d.common.model.SinceSource.AB2D;
import static gov.cms.ab2d.common.model.SinceSource.FIRST_RUN;
import static gov.cms.ab2d.common.model.SinceSource.USER;
import static gov.cms.ab2d.common.util.Constants.FHIR_NDJSON_CONTENT_TYPE;
import static gov.cms.ab2d.fhir.FhirVersion.R4;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static gov.cms.ab2d.job.model.JobStartedBy.DEVELOPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@Profile("test")
class JobPreProcessorIntegrationTest extends JobCleanup {

    private JobPreProcessor cut;

    @Autowired
    private ContractServiceStub contractServiceStub;

    @Autowired
    private ContractWorkerClient contractWorkerClient;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PdpClientRepository pdpClientRepository;

    @Mock
    private SQSEventClient sqsEventClient;

    @Autowired
    private DataSetup dataSetup;

    @Mock
    private CoverageDriver coverageDriver;

    @Captor
    private ArgumentCaptor<LoggableEvent> captor;

    private PdpClient pdpClient;
    private Job job;
    private Contract contract;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @Container
    private static final AB2DLocalstackContainer localstackContainer = new AB2DLocalstackContainer();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        cut = new JobPreProcessorImpl(contractWorkerClient, jobRepository, sqsEventClient, coverageDriver);

        contract = new Contract();
        contract.setContractNumber(UUID.randomUUID().toString());
        contract.setContractName(UUID.randomUUID().toString());
        contractServiceStub.updateContract(contract);
        pdpClient = createClient(contract);
        job = createJob(pdpClient, contract.getContractNumber());
    }

    @AfterEach
    void clear() {
        jobCleanup();
        dataSetup.cleanup();
    }

    @Test
    @DisplayName("When a job is in submitted status, it can be put into progress upon starting processing")
    void whenJobIsInSubmittedStatus_ThenJobShouldBePutInProgress() throws InterruptedException {
        when(coverageDriver.isCoverageAvailable(any(Job.class), any(ContractDTO.class))).thenReturn(true);

        var processedJob = cut.preprocess(job.getJobUuid());
        assertEquals(JobStatus.IN_PROGRESS, processedJob.getStatus());

        verify(sqsEventClient, times(1)).logAndAlert(captor.capture(), any());
        List<LoggableEvent> jobStatusChange = captor.getAllValues();

        assertEquals(1, jobStatusChange.size());
        JobStatusChangeEvent event = (JobStatusChangeEvent) jobStatusChange.get(0);
        assertEquals("SUBMITTED", event.getOldStatus());
        assertEquals("IN_PROGRESS", event.getNewStatus());
    }

    @Test
    @DisplayName("When a job is not already in a submitted status, it cannot be put into progress")
    void whenJobIsNotInSubmittedStatus_ThenJobShouldNotBePutInProgress() throws InterruptedException {
        when(coverageDriver.isCoverageAvailable(any(Job.class), any(ContractDTO.class))).thenReturn(true);

        job.setStatus(JobStatus.IN_PROGRESS);

        Job inProgress = jobRepository.save(job);

        var exceptionThrown = assertThrows(
                IllegalArgumentException.class,
                () -> cut.preprocess(inProgress.getJobUuid()));

        assertEquals("Job S0000 is not in SUBMITTED status", exceptionThrown.getMessage());
    }

    @Test
    @DisplayName("When coverage fails, a job should fail")
    void whenCoverageFails_ThenJobShouldFail() throws InterruptedException {
        when(coverageDriver.isCoverageAvailable(any(Job.class), any(ContractDTO.class))).thenThrow(new CoverageDriverException("test"));

        var processedJob = cut.preprocess(job.getJobUuid());
        assertEquals(JobStatus.FAILED, processedJob.getStatus());

        verify(sqsEventClient, times(1)).logAndAlert(captor.capture(), any());
        List<LoggableEvent> jobStatusChange = captor.getAllValues();

        assertEquals(1, jobStatusChange.size());
        JobStatusChangeEvent event = (JobStatusChangeEvent) jobStatusChange.get(0);
        assertEquals("SUBMITTED", event.getOldStatus());
        assertEquals("FAILED", event.getNewStatus());

    }

    @Test
    @DisplayName("We're R4, there have been successful jobs so set default since")
    void testDefaultSince() {
        // This is the last successful job run
        Job oldJob = new Job();
        oldJob.setJobUuid("AA-BB");
        oldJob.setStatus(JobStatus.SUCCESSFUL);
        oldJob.setStatusMessage("100%");
        oldJob.setOrganization(pdpClient.getOrganization());
        oldJob.setOutputFormat(FHIR_NDJSON_CONTENT_TYPE);
        OffsetDateTime oldJobTime = OffsetDateTime.parse("2021-01-01T00:00:00.000-05:00", DateTimeFormatter.ISO_DATE_TIME);
        oldJob.setCreatedAt(oldJobTime);
        oldJob.setFhirVersion(STU3);
        oldJob.setContractNumber(contract.getContractNumber());
        oldJob = jobRepository.save(oldJob);

        // This is an even early job (want to make sure it picks the correct old job)
        Job reallyOldJob = new Job();
        reallyOldJob.setJobUuid("CC-DD");
        reallyOldJob.setStatus(JobStatus.SUCCESSFUL);
        reallyOldJob.setStatusMessage("100%");
        reallyOldJob.setOrganization(pdpClient.getOrganization());
        reallyOldJob.setStartedBy(DEVELOPER);
        reallyOldJob.setOutputFormat(FHIR_NDJSON_CONTENT_TYPE);
        OffsetDateTime reallyOldldJobTime = OffsetDateTime.parse("2020-12-01T00:00:00.000-05:00", DateTimeFormatter.ISO_DATE_TIME);
        reallyOldJob.setCreatedAt(reallyOldldJobTime);
        reallyOldJob.setFhirVersion(R4);
        reallyOldJob.setContractNumber(contract.getContractNumber());
        reallyOldJob = jobRepository.save(reallyOldJob);

        Job newJob = new Job();
        newJob.setJobUuid("YY-ZZ");
        newJob.setStatus(JobStatus.SUBMITTED);
        newJob.setStatusMessage("0%");
        newJob.setOrganization(pdpClient.getOrganization());
        newJob.setOutputFormat(FHIR_NDJSON_CONTENT_TYPE);
        newJob.setCreatedAt(OffsetDateTime.now());
        newJob.setFhirVersion(R4);
        newJob.setContractNumber(contract.getContractNumber());
        newJob = jobRepository.save(newJob);

        Job processedJob = cut.preprocess(newJob.getJobUuid());
        assertEquals(oldJobTime.getNano(), processedJob.getSince().getNano());
        assertEquals(AB2D, processedJob.getSinceSource());

        addJobForCleanup(oldJob);
        addJobForCleanup(reallyOldJob);
        addJobForCleanup(newJob);
    }

    @Test
    @DisplayName("We're R4, there has been a failed job so don't set default since")
    void testDefaultSinceFailed() {
        Job oldJob = new Job();
        oldJob.setJobUuid("AA-BB");
        oldJob.setStatus(JobStatus.FAILED);
        oldJob.setStatusMessage("100%");
        oldJob.setOrganization(pdpClient.getOrganization());
        oldJob.setOutputFormat(FHIR_NDJSON_CONTENT_TYPE);
        OffsetDateTime oldJobTime = OffsetDateTime.parse("2021-01-01T00:00:00.000-05:00", DateTimeFormatter.ISO_DATE_TIME);
        oldJob.setCreatedAt(oldJobTime);
        oldJob.setFhirVersion(STU3);
        oldJob.setContractNumber(contract.getContractNumber());
        oldJob = jobRepository.save(oldJob);

        Job newJob = new Job();
        newJob.setJobUuid("YY-ZZ");
        newJob.setStatus(JobStatus.SUBMITTED);
        newJob.setStatusMessage("0%");
        newJob.setOrganization(pdpClient.getOrganization());
        newJob.setOutputFormat(FHIR_NDJSON_CONTENT_TYPE);
        OffsetDateTime newJobTime = OffsetDateTime.parse("2021-02-01T00:00:00.000-05:00", DateTimeFormatter.ISO_DATE_TIME);
        newJob.setCreatedAt(newJobTime);
        newJob.setFhirVersion(R4);
        newJob.setContractNumber(contract.getContractNumber());
        newJob = jobRepository.save(newJob);

        Job processedJob = cut.preprocess(newJob.getJobUuid());
        assertNull(processedJob.getSince());
        assertEquals(FIRST_RUN, processedJob.getSinceSource());

        addJobForCleanup(oldJob);
        addJobForCleanup(newJob);
    }

    @Test
    @DisplayName("We're R4, there has been a successful job run but it was run by the developers so don't set default since")
    void testDefaultSinceAB2DRun() {
        Job oldJob = new Job();
        oldJob.setJobUuid("AA-BB");
        oldJob.setStatus(JobStatus.SUCCESSFUL);
        oldJob.setStatusMessage("100%");
        oldJob.setOrganization(pdpClient.getOrganization());
        oldJob.setStartedBy(DEVELOPER);
        oldJob.setOutputFormat(FHIR_NDJSON_CONTENT_TYPE);
        OffsetDateTime oldJobTime = OffsetDateTime.parse("2021-01-01T00:00:00.000-05:00", DateTimeFormatter.ISO_DATE_TIME);
        oldJob.setCreatedAt(oldJobTime);
        oldJob.setFhirVersion(STU3);
        oldJob.setContractNumber(contract.getContractNumber());
        oldJob = jobRepository.save(oldJob);

        Job newJob = new Job();
        newJob.setJobUuid("YY-ZZ");
        newJob.setStatus(JobStatus.SUBMITTED);
        newJob.setStatusMessage("0%");
        newJob.setOrganization(pdpClient.getOrganization());
        newJob.setOutputFormat(FHIR_NDJSON_CONTENT_TYPE);
        newJob.setCreatedAt(OffsetDateTime.now());
        newJob.setFhirVersion(R4);
        newJob.setContractNumber(contract.getContractNumber());

        newJob = jobRepository.save(newJob);

        Job processedJob = cut.preprocess(newJob.getJobUuid());
        assertNull(processedJob.getSince());
        assertEquals(FIRST_RUN, processedJob.getSinceSource());

        addJobForCleanup(oldJob);
        addJobForCleanup(newJob);
    }

    @Test
    @DisplayName("We're R4, there has been a successful job run but we've specified a since date so don't set default since")
    void testDefaultSinceNotNeeded() {
        Job oldJob = new Job();
        oldJob.setJobUuid("AA-BB");
        oldJob.setStatus(JobStatus.SUCCESSFUL);
        oldJob.setStatusMessage("100%");
        oldJob.setOrganization(pdpClient.getOrganization());
        oldJob.setOutputFormat(FHIR_NDJSON_CONTENT_TYPE);
        OffsetDateTime oldJobTime = OffsetDateTime.parse("2021-01-01T00:00:00.000-05:00", DateTimeFormatter.ISO_DATE_TIME);
        oldJob.setCreatedAt(oldJobTime);
        oldJob.setFhirVersion(STU3);
        oldJob.setContractNumber(contract.getContractNumber());
        oldJob = jobRepository.save(oldJob);

        Job newJob = new Job();
        newJob.setJobUuid("YY-ZZ");
        newJob.setStatus(JobStatus.SUBMITTED);
        newJob.setStatusMessage("0%");
        newJob.setOrganization(pdpClient.getOrganization());
        newJob.setOutputFormat(FHIR_NDJSON_CONTENT_TYPE);
        OffsetDateTime suppliedSince = OffsetDateTime.parse("2021-02-01T00:00:00.000-05:00", DateTimeFormatter.ISO_DATE_TIME);
        newJob.setSince(suppliedSince);
        newJob.setCreatedAt(OffsetDateTime.now());
        newJob.setFhirVersion(R4);
        newJob.setSince(suppliedSince);
        newJob.setContractNumber(contract.getContractNumber());
        newJob = jobRepository.save(newJob);

        Job processedJob = cut.preprocess(newJob.getJobUuid());
        assertEquals(suppliedSince.getNano(), processedJob.getSince().getNano());
        assertEquals(USER, processedJob.getSinceSource());

        addJobForCleanup(oldJob);
        addJobForCleanup(newJob);
    }

    private PdpClient createClient(Contract contract) {
        PdpClient pdpClient = new PdpClient();
        pdpClient.setClientId("Harry_Potter");
        pdpClient.setOrganization("Harry_Potter");
        pdpClient.setEnabled(true);
        pdpClient.setContractId(contract.getId());

        pdpClient = pdpClientRepository.save(pdpClient);
        dataSetup.queueForCleanup(pdpClient);
        return pdpClient;
    }

    private Job createJob(PdpClient pdpClient, String contractNumber) {
        Job job = new Job();
        job.setJobUuid("S0000");
        job.setStatus(JobStatus.SUBMITTED);
        job.setStatusMessage("0%");
        job.setOrganization(pdpClient.getOrganization());
        job.setOutputFormat(FHIR_NDJSON_CONTENT_TYPE);
        job.setCreatedAt(OffsetDateTime.now());
        job.setFhirVersion(STU3);
        job.setContractNumber(contractNumber);

        job = jobRepository.save(job);
        addJobForCleanup(job);
        return job;
    }
}