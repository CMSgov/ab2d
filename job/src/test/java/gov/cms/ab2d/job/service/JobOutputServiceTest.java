package gov.cms.ab2d.job.service;

import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.service.ResourceNotFoundException;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.job.JobTestSpringBootApp;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobOutput;
import gov.cms.ab2d.job.model.JobStatus;
import gov.cms.ab2d.job.repository.JobOutputRepository;
import gov.cms.ab2d.job.repository.JobRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static gov.cms.ab2d.common.util.DataSetup.TEST_PDP_CLIENT;
import static gov.cms.ab2d.fhir.BundleUtils.EOB;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = JobTestSpringBootApp.class)
@Testcontainers
class JobOutputServiceTest extends JobCleanup {

    @Autowired
    JobService jobService;

    @Autowired
    JobRepository jobRepository;

    @Autowired
    ContractRepository contractRepository;

    @Autowired
    JobOutputRepository jobOutputRepository;

    @Autowired
    JobOutputService jobOutputService;

    @Autowired
    DataSetup dataSetup;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    private static final String JOB_OUTPUT_CONTRACT_NUMBER = "JJ112233";

    // Be safe and make sure nothing from another test will impact current test
    @BeforeEach
    public void setup() {
        dataSetup.setupPdpClient(List.of());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new org.springframework.security.core.userdetails.User(TEST_PDP_CLIENT,
                                "test", new ArrayList<>()), "pass"));
    }

    @AfterEach
    public void tearDown() {
        jobCleanup();
        dataSetup.cleanup();
    }

    @Test
    void testJobOutputUpdate() {
        Job job = new Job();
        job.setJobUuid("uuid");
        job.setOrganization(TEST_PDP_CLIENT);
        job.setStatus(JobStatus.FAILED);
        job.setCreatedAt(OffsetDateTime.now());
        job.setFhirVersion(STU3);
        job.setContractNumber(JOB_OUTPUT_CONTRACT_NUMBER);
        Job savedJob = jobRepository.save(job);
        addJobForCleanup(savedJob);

        JobOutput jobOutput = new JobOutput();
        jobOutput.setDownloaded(0);
        jobOutput.setError(true);
        jobOutput.setFhirResourceType(EOB);
        jobOutput.setFilePath("file.ndjson");
        jobOutput.setJob(savedJob);
        jobOutput.setChecksum("testoutput");
        jobOutput.setFileLength(20L);
        JobOutput savedJobOutput = jobOutputRepository.save(jobOutput);
        savedJobOutput.setDownloaded(0);
        savedJobOutput.setError(false);
        savedJobOutput.setFilePath("newpath.ndjson");
        savedJobOutput.setFhirResourceType("newtype");
        JobOutput updatedJobOutput = jobOutputService.updateJobOutput(savedJobOutput);
        assertEquals(updatedJobOutput.getFilePath(), savedJobOutput.getFilePath());
        assertEquals(updatedJobOutput.getDownloaded(), savedJobOutput.getDownloaded());
        assertEquals(updatedJobOutput.getError(), savedJobOutput.getError());
        assertEquals(updatedJobOutput.getFhirResourceType(), savedJobOutput.getFhirResourceType());
    }

    @Test
    void testJobOutputRetrieval() {
        Job job = new Job();
        job.setJobUuid("uuid");
        job.setOrganization(TEST_PDP_CLIENT);
        job.setStatus(JobStatus.FAILED);
        job.setCreatedAt(OffsetDateTime.now());
        job.setFhirVersion(STU3);
        job.setContractNumber(JOB_OUTPUT_CONTRACT_NUMBER);
        Job savedJob = jobRepository.save(job);
        addJobForCleanup(savedJob);

        JobOutput jobOutput = new JobOutput();
        jobOutput.setDownloaded(0);
        jobOutput.setError(true);
        jobOutput.setFhirResourceType(EOB);
        jobOutput.setChecksum("testoutput");
        jobOutput.setFileLength(20L);
        jobOutput.setFilePath("file.ndjson");
        jobOutput.setJob(savedJob);
        JobOutput savedJobOutput = jobOutputRepository.save(jobOutput);

        JobOutput retrievedJobOutput = jobOutputService.findByFilePathAndJob(jobOutput.getFilePath(), job);

        assertEquals(savedJobOutput, retrievedJobOutput);
    }

    @Test
    void testJobOutputRetrievalNotFound() {
        Job job = new Job();
        job.setJobUuid("uuid");
        job.setOrganization(TEST_PDP_CLIENT);
        job.setStatus(JobStatus.FAILED);
        job.setCreatedAt(OffsetDateTime.now());
        job.setFhirVersion(STU3);
        job.setContractNumber(JOB_OUTPUT_CONTRACT_NUMBER);
        Job savedJob = jobRepository.save(job);
        addJobForCleanup(savedJob);

        JobOutput jobOutput = new JobOutput();
        jobOutput.setDownloaded(0);
        jobOutput.setError(true);
        jobOutput.setFhirResourceType(EOB);
        jobOutput.setFilePath("file.ndjson");
        jobOutput.setChecksum("testoutput");
        jobOutput.setFileLength(20L);
        jobOutput.setJob(savedJob);
        jobOutputRepository.save(jobOutput);

        var exception = Assertions.assertThrows(ResourceNotFoundException.class, () ->
                jobOutputService.findByFilePathAndJob("", job));
        assertEquals("JobOutput with fileName  was not able to be found" +
                " for job " + job.getJobUuid(), exception.getMessage());
    }

    @Test
    void testJobOutputDownloaded() {
        int maxInterval = 30;
        Job job = new Job();
        job.setJobUuid(UUID.randomUUID().toString());
        job.setOrganization(RandomStringUtils.randomAlphabetic(10));
        job.setStatus(JobStatus.SUCCESSFUL);
        job.setCreatedAt(OffsetDateTime.now());
        job.setExpiresAt(OffsetDateTime.now().plusMinutes(maxInterval));
        job.setFhirVersion(STU3);
        job.setContractNumber(RandomStringUtils.randomAlphabetic(10));
        Job savedJob = jobRepository.save(job);

        JobOutput jobOutput = new JobOutput();
        jobOutput.setDownloaded(10);
        jobOutput.setError(true);
        jobOutput.setFhirResourceType(EOB);
        jobOutput.setFilePath(RandomStringUtils.randomAlphabetic(10) + ".ndjson");
        jobOutput.setChecksum(RandomStringUtils.randomAlphabetic(10));
        jobOutput.setFileLength(20L);
        jobOutput.setJob(savedJob);
        jobOutput.setLastDownloadAt(OffsetDateTime.now().minusMinutes(maxInterval + 1));
        jobOutputRepository.save(jobOutput);
        assertFalse(jobOutputService.expiredDownloadableFiles(maxInterval).isEmpty());
    }

    @Test
    void testJobOutputDownloadedNone() {
        int maxInterval = 30;
        Job job = new Job();
        job.setJobUuid(UUID.randomUUID().toString());
        job.setOrganization(RandomStringUtils.randomAlphabetic(10));
        job.setStatus(JobStatus.SUCCESSFUL);
        job.setCreatedAt(OffsetDateTime.now());
        job.setExpiresAt(OffsetDateTime.now().plusMinutes(maxInterval));
        job.setFhirVersion(STU3);
        job.setContractNumber(RandomStringUtils.randomAlphabetic(10));
        Job savedJob = jobRepository.save(job);

        JobOutput jobOutput = new JobOutput();
        jobOutput.setDownloaded(10);
        jobOutput.setError(true);
        jobOutput.setFhirResourceType(EOB);
        jobOutput.setFilePath(RandomStringUtils.randomAlphabetic(10) + ".ndjson");
        jobOutput.setChecksum(RandomStringUtils.randomAlphabetic(10));
        jobOutput.setFileLength(20L);
        jobOutput.setJob(savedJob);
        jobOutput.setLastDownloadAt(OffsetDateTime.now().minusMinutes(maxInterval - 1));
        jobOutputRepository.save(jobOutput);
        assertTrue(jobOutputService.expiredDownloadableFiles(maxInterval).isEmpty());
    }
}
