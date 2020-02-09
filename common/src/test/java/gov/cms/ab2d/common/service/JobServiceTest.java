package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.SpringBootApp;
import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.repository.SponsorRepository;
import gov.cms.ab2d.common.repository.UserRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.TransactionSystemException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static gov.cms.ab2d.common.service.JobServiceImpl.INITIAL_JOB_STATUS_MESSAGE;
import static gov.cms.ab2d.common.service.JobServiceImpl.ZIPFORMAT;
import static gov.cms.ab2d.common.util.Constants.*;
import static gov.cms.ab2d.common.util.DataSetup.TEST_USER;
import static gov.cms.ab2d.common.util.DataSetup.VALID_CONTRACT_NUMBER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = SpringBootApp.class)
@TestPropertySource(locations = "/application.common.properties")
@Testcontainers
public class JobServiceTest {

    @Autowired
    JobService jobService;

    @Autowired
    JobRepository jobRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    SponsorRepository sponsorRepository;

    @Autowired
    ContractRepository contractRepository;

    @Autowired
    DataSetup dataSetup;

    @Value("${efs.mount}")
    private String tmpJobLocation;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    // Be safe and make sure nothing from another test will impact current test
    @BeforeEach
    public void setup() {
        contractRepository.deleteAll();
        jobRepository.deleteAll();
        userRepository.deleteAll();
        sponsorRepository.deleteAll();

        dataSetup.setupUser(List.of());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new org.springframework.security.core.userdetails.User(TEST_USER,
                                "test", new ArrayList<>()), "pass"));
    }

    @Test
    public void createJob() {
        Job job = jobService.createJob(EOB, "http://localhost:8080", ZIPFORMAT);
        assertThat(job).isNotNull();
        assertThat(job.getId()).isNotNull();
        assertThat(job.getJobUuid()).isNotNull();
        assertThat(job.getOutputFormat()).isNotNull();
        assertEquals(job.getOutputFormat(), ZIPFORMAT);
        assertEquals(job.getProgress(), Integer.valueOf(0));
        assertEquals(job.getUser(), userRepository.findByUsername(TEST_USER));
        assertEquals(job.getResourceTypes(), EOB);
        assertEquals(job.getRequestUrl(), "http://localhost:8080");
        assertEquals(job.getStatusMessage(), INITIAL_JOB_STATUS_MESSAGE);
        assertEquals(job.getStatus(), JobStatus.SUBMITTED);
        assertEquals(job.getJobOutputs().size(), 0);
        assertEquals(job.getLastPollTime(), null);
        assertEquals(job.getExpiresAt(), null);
        assertThat(job.getJobUuid()).matches(
                "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");

        // Verify it actually got persisted in the DB
        assertThat(jobRepository.findById(job.getId())).get().isEqualTo(job);
    }

    @Test
    public void createJobWithContract() {
        Contract contract = contractRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();

        Job job = jobService.createJob(EOB, "http://localhost:8080", contract.getContractNumber(), NDJSON_FIRE_CONTENT_TYPE);
        assertThat(job).isNotNull();
        assertThat(job.getId()).isNotNull();
        assertThat(job.getJobUuid()).isNotNull();
        assertThat(job.getOutputFormat()).isNotNull();
        assertEquals(NDJSON_FIRE_CONTENT_TYPE, job.getOutputFormat());
        assertEquals(job.getProgress(), Integer.valueOf(0));
        assertEquals(job.getUser(), userRepository.findByUsername(TEST_USER));
        assertEquals(job.getResourceTypes(), EOB);
        assertEquals(job.getRequestUrl(), "http://localhost:8080");
        assertEquals(job.getStatusMessage(), INITIAL_JOB_STATUS_MESSAGE);
        assertEquals(job.getStatus(), JobStatus.SUBMITTED);
        assertEquals(job.getJobOutputs().size(), 0);
        assertEquals(job.getLastPollTime(), null);
        assertEquals(job.getExpiresAt(), null);
        assertThat(job.getJobUuid()).matches(
                "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");
        assertEquals(job.getContract().getContractNumber(), contract.getContractNumber());

        // Verify it actually got persisted in the DB
        assertThat(jobRepository.findById(job.getId())).get().isEqualTo(job);
    }

    @Test
    public void createJobWithBadContract() {
        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            jobService.createJob(EOB, "http://localhost:8080", "BadContract", NDJSON_FIRE_CONTENT_TYPE);
        });
    }

    @Test
    public void failedValidation() {
        Assertions.assertThrows(TransactionSystemException.class, () -> {
            jobService.createJob("Patient,ExplanationOfBenefit,Coverage", "http://localhost:8080", NDJSON_FIRE_CONTENT_TYPE);
        });
    }

    @Test
    public void cancelJob() {
        Job job = jobService.createJob(EOB, "http://localhost:8080", NDJSON_FIRE_CONTENT_TYPE);

        jobService.cancelJob(job.getJobUuid());

        // Verify that it has the correct status
        Job cancelledJob = jobRepository.findByJobUuid(job.getJobUuid());

        assertEquals(JobStatus.CANCELLED, cancelledJob.getStatus());
    }

    @Test
    public void cancelNonExistingJob() {
        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            jobService.cancelJob("NonExistingJob");
        });
    }

    @Test
    public void getJob() {
        Job job = jobService.createJob(EOB, "http://localhost:8080", NDJSON_FIRE_CONTENT_TYPE);

        Job retrievedJob = jobService.getJobByJobUuid(job.getJobUuid());

        assertEquals(job, retrievedJob);
    }

    @Test
    public void getNonExistentJob() {
        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            jobService.getJobByJobUuid("NonExistent");
        });
    }

    @Test
    public void testJobInSuccessfulState() {
        Job job = jobService.createJob(EOB, "http://localhost:8080", NDJSON_FIRE_CONTENT_TYPE);

        job.setStatus(JobStatus.SUCCESSFUL);
        jobRepository.saveAndFlush(job);

        Assertions.assertThrows(InvalidJobStateTransition.class, () -> {
            jobService.cancelJob(job.getJobUuid());
        });
    }

    @Test
    public void testJobInCancelledState() {
        Job job = jobService.createJob(EOB, "http://localhost:8080", NDJSON_FIRE_CONTENT_TYPE);

        job.setStatus(JobStatus.CANCELLED);
        jobRepository.saveAndFlush(job);

        Assertions.assertThrows(InvalidJobStateTransition.class, () -> {
            jobService.cancelJob(job.getJobUuid());
        });

    }

    @Test
    public void testJobInFailedState() {
        Job job = jobService.createJob(EOB, "http://localhost:8080", NDJSON_FIRE_CONTENT_TYPE);

        job.setStatus(JobStatus.FAILED);
        jobRepository.saveAndFlush(job);

        Assertions.assertThrows(InvalidJobStateTransition.class, () -> {
            jobService.cancelJob(job.getJobUuid());
        });

    }

    @Test
    public void updateJob() {
        Job job = jobService.createJob(EOB, "http://localhost:8080", ZIPFORMAT);
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime localDateTime = OffsetDateTime.now();
        job.setProgress(100);
        job.setLastPollTime(now);
        job.setStatus(JobStatus.IN_PROGRESS);
        job.setCreatedAt(localDateTime);
        job.setCompletedAt(localDateTime);
        job.setJobUuid("abc");
        job.setResourceTypes(EOB);
        job.setRequestUrl("http://localhost");
        job.setStatusMessage("Pending");
        job.setExpiresAt(now);

        JobOutput jobOutput = new JobOutput();
        jobOutput.setError(false);
        jobOutput.setFhirResourceType(EOB);
        jobOutput.setFilePath("file.ndjson");
        jobOutput.setJob(job);

        JobOutput errorJobOutput = new JobOutput();
        errorJobOutput.setError(true);
        errorJobOutput.setFhirResourceType(OPERATION_OUTCOME);
        errorJobOutput.setFilePath("error.ndjson");
        errorJobOutput.setJob(job);

        List<JobOutput> output = List.of(jobOutput, errorJobOutput);
        job.setJobOutputs(output);

        Job updatedJob = jobService.updateJob(job);
        Assert.assertEquals(ZIPFORMAT, updatedJob.getOutputFormat());
        Assert.assertEquals(Integer.valueOf(100), updatedJob.getProgress());
        Assert.assertEquals(now, updatedJob.getLastPollTime());
        Assert.assertEquals(JobStatus.IN_PROGRESS, updatedJob.getStatus());
        Assert.assertEquals(localDateTime, updatedJob.getCreatedAt());
        Assert.assertEquals(localDateTime, updatedJob.getCompletedAt());
        Assert.assertEquals("abc", updatedJob.getJobUuid());
        Assert.assertEquals(EOB, updatedJob.getResourceTypes());
        Assert.assertEquals("http://localhost", updatedJob.getRequestUrl());
        Assert.assertEquals("Pending", updatedJob.getStatusMessage());
        Assert.assertEquals(now, updatedJob.getExpiresAt());

        JobOutput updatedOutput = updatedJob.getJobOutputs().get(0);
        Assert.assertEquals(false, updatedOutput.getError());
        Assert.assertEquals(EOB, updatedOutput.getFhirResourceType());
        Assert.assertEquals("file.ndjson", updatedOutput.getFilePath());

        JobOutput updatedErrorOutput = updatedJob.getJobOutputs().get(1);
        Assert.assertEquals(true, updatedErrorOutput.getError());
        Assert.assertEquals(OPERATION_OUTCOME, updatedErrorOutput.getFhirResourceType());
        Assert.assertEquals("error.ndjson", updatedErrorOutput.getFilePath());
    }

    @Test
    public void getFileDownloadUrl() throws IOException {
        String testFile = "test.ndjson";
        String errorFile = "error.ndjson";
        Job job = createJobForFileDownloads(testFile, errorFile);

        Path destination = Paths.get(tmpJobLocation, job.getJobUuid());
        String destinationStr = destination.toString();
        Files.createDirectories(destination);

        createNDJSONFile(testFile, destinationStr);
        createNDJSONFile(errorFile, destinationStr);

        Resource resource = jobService.getResourceForJob(job.getJobUuid(), testFile);
        Assert.assertEquals(testFile, resource.getFilename());

        Resource errorResource = jobService.getResourceForJob(job.getJobUuid(), errorFile);
        Assert.assertEquals(errorFile, errorResource.getFilename());
    }

    private void createNDJSONFile(String file, String destinationStr) throws IOException {
        InputStream testFileStream = this.getClass().getResourceAsStream("/" + file);
        String fileStr = IOUtils.toString(testFileStream, "UTF-8");
        try (PrintWriter out = new PrintWriter(destinationStr + File.separator + file)) {
            out.println(fileStr);
        }
    }

    private Job createJobForFileDownloads(String fileName, String errorFileName) {
        Job job = jobService.createJob(EOB, "http://localhost:8080", NDJSON_FIRE_CONTENT_TYPE);
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime localDateTime = OffsetDateTime.now();
        job.setProgress(100);
        job.setLastPollTime(now);
        job.setStatus(JobStatus.IN_PROGRESS);
        job.setCreatedAt(localDateTime);
        job.setCompletedAt(localDateTime);
        job.setResourceTypes(EOB);
        job.setRequestUrl("http://localhost");
        job.setStatusMessage("Pending");
        job.setExpiresAt(now);

        JobOutput jobOutput = new JobOutput();
        jobOutput.setError(false);
        jobOutput.setFhirResourceType(EOB);
        jobOutput.setFilePath(fileName);
        jobOutput.setJob(job);

        JobOutput errorJobOutput = new JobOutput();
        errorJobOutput.setError(true);
        errorJobOutput.setFhirResourceType(OPERATION_OUTCOME);
        errorJobOutput.setFilePath(errorFileName);
        errorJobOutput.setJob(job);

        List<JobOutput> output = List.of(jobOutput, errorJobOutput);
        job.setJobOutputs(output);

        return jobService.updateJob(job);
    }

    @Test
    public void getFileDownloadUrlWithWrongFilename() throws IOException {
        String testFile = "test.ndjson";
        String errorFile = "error.ndjson";
        Job job = createJobForFileDownloads(testFile, errorFile);

        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            jobService.getResourceForJob(job.getJobUuid(), "filenamewrong.ndjson");
        });

    }

    @Test
    public void getFileDownloadUrlWitMissingOutput() throws IOException {
        String testFile = "outputmissing.ndjson";
        String errorFile = "error.ndjson";
        Job job = createJobForFileDownloads(testFile, errorFile);

        Assertions.assertThrows(JobOutputMissingException.class, () -> {
            jobService.getResourceForJob(job.getJobUuid(), "outputmissing.ndjson");
        });
    }

    @Test
    public void checkIfUserCanAddJobTest() {
        boolean result = jobService.checkIfCurrentUserCanAddJob();
        Assert.assertTrue(result);
    }

    @Test
    public void checkIfUserCanAddJobTrueTest() {
        jobService.createJob(EOB, "http://localhost:8080", NDJSON_FIRE_CONTENT_TYPE);

        boolean result = jobService.checkIfCurrentUserCanAddJob();
        Assert.assertTrue(result);
    }

    @Test
    public void checkIfUserCanAddJobPastLimitTest() {
        jobService.createJob(EOB, "http://localhost:8080", NDJSON_FIRE_CONTENT_TYPE);
        jobService.createJob(EOB, "http://localhost:8080", NDJSON_FIRE_CONTENT_TYPE);
        jobService.createJob(EOB, "http://localhost:8080", NDJSON_FIRE_CONTENT_TYPE);

        boolean result = jobService.checkIfCurrentUserCanAddJob();
        Assert.assertFalse(result);
    }
}
