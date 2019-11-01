package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.SpringBootApp;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobOutput;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.repository.JobRepository;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.TransactionSystemException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;

import static gov.cms.ab2d.common.service.JobServiceImpl.INITIAL_JOB_STATUS_MESSAGE;
import static gov.cms.ab2d.common.util.Constants.OPERATION_OUTCOME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringBootApp.class)
@TestPropertySource(locations = "/common-it.properties")
public class JobServiceTest {

    @Autowired
    JobService jobService;

    @Autowired
    JobRepository jobRepository;

    @Value("${efs.mount}")
    private String tmpJobLocation;

    // Be safe and make sure nothing from another test will impact current test
    @Before
    public void clearJobs() {
        jobRepository.deleteAll();
    }

    @Test
    public void createJob() {
        Job job = jobService.createJob("ExplanationOfBenefits", "http://localhost:8080");
        assertThat(job).isNotNull();
        assertThat(job.getId()).isNotNull();
        assertThat(job.getJobId()).isNotNull();
        assertEquals(job.getProgress(), Integer.valueOf(0));
        assertEquals(job.getUser(), null); // null for now since no authentication
        assertEquals(job.getResourceTypes(), "ExplanationOfBenefits");
        assertEquals(job.getRequestUrl(), "http://localhost:8080");
        assertEquals(job.getStatusMessage(), INITIAL_JOB_STATUS_MESSAGE);
        assertEquals(job.getStatus(), JobStatus.SUBMITTED);
        assertEquals(job.getJobOutput().size(), 0);
        assertEquals(job.getLastPollTime(), null);
        assertEquals(job.getExpiresAt(), null);
        assertThat(job.getJobId()).matches("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");

        // Verify it actually got persisted in the DB
        assertThat(jobRepository.findById(job.getId())).get().isEqualTo(job);
    }

    @Test(expected = TransactionSystemException.class)
    public void failedValidation() {
        jobService.createJob("Patient,ExplanationOfBenefits,Coverage", "http://localhost:8080");
    }

    @Test
    public void cancelJob() {
        Job job = jobService.createJob("ExplanationOfBenefits", "http://localhost:8080");

        jobService.cancelJob(job.getJobId());

        // Verify that it has the correct status
        Job cancelledJob = jobRepository.findByJobId(job.getJobId());

        assertEquals(JobStatus.CANCELLED, cancelledJob.getStatus());
    }

    @Test(expected = ResourceNotFoundException.class)
    public void cancelNonExistingJob() {
        jobService.cancelJob("NonExistingJob");
    }

    @Test
    public void getJob() {
        Job job = jobService.createJob("ExplanationOfBenefits", "http://localhost:8080");

        Job retrievedJob = jobService.getJobByJobId(job.getJobId());

        assertEquals(job, retrievedJob);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void getNonExistentJob() {
        jobService.getJobByJobId("NonExistent");
    }

    @Test(expected = InvalidJobStateTransition.class)
    public void testJobInSuccessfulState() {
        Job job = jobService.createJob("ExplanationOfBenefits", "http://localhost:8080");

        job.setStatus(JobStatus.SUCCESSFUL);
        jobRepository.saveAndFlush(job);

        jobService.cancelJob(job.getJobId());
    }

    @Test(expected = InvalidJobStateTransition.class)
    public void testJobInCancelledState() {
        Job job = jobService.createJob("ExplanationOfBenefits", "http://localhost:8080");

        job.setStatus(JobStatus.CANCELLED);
        jobRepository.saveAndFlush(job);

        jobService.cancelJob(job.getJobId());
    }

    @Test(expected = InvalidJobStateTransition.class)
    public void testJobInFailedState() {
        Job job = jobService.createJob("ExplanationOfBenefits", "http://localhost:8080");

        job.setStatus(JobStatus.FAILED);
        jobRepository.saveAndFlush(job);

        jobService.cancelJob(job.getJobId());
    }

    @Test
    public void updateJob() {
        Job job = jobService.createJob("ExplanationOfBenefits", "http://localhost:8080");
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime localDateTime = OffsetDateTime.now();
        job.setProgress(100);
        job.setLastPollTime(now);
        job.setStatus(JobStatus.IN_PROGRESS);
        job.setCreatedAt(localDateTime);
        job.setCompletedAt(localDateTime);
        job.setJobId("abc");
        job.setResourceTypes("ExplanationOfBenefits");
        job.setRequestUrl("http://localhost");
        job.setStatusMessage("Pending");
        job.setExpiresAt(now);

        JobOutput jobOutput = new JobOutput();
        jobOutput.setError(false);
        jobOutput.setFhirResourceType("ExplanationOfBenefits");
        jobOutput.setFilePath("file.ndjson");
        jobOutput.setJob(job);

        JobOutput errorJobOutput = new JobOutput();
        errorJobOutput.setError(true);
        errorJobOutput.setFhirResourceType(OPERATION_OUTCOME);
        errorJobOutput.setFilePath("error.ndjson");
        errorJobOutput.setJob(job);

        List output = List.of(jobOutput, errorJobOutput);
        job.setJobOutput(output);

        Job updatedJob = jobService.updateJob(job);
        Assert.assertEquals(Integer.valueOf(100), updatedJob.getProgress());
        Assert.assertEquals(now, updatedJob.getLastPollTime());
        Assert.assertEquals(JobStatus.IN_PROGRESS, updatedJob.getStatus());
        Assert.assertEquals(localDateTime, updatedJob.getCreatedAt());
        Assert.assertEquals(localDateTime, updatedJob.getCompletedAt());
        Assert.assertEquals("abc", updatedJob.getJobId());
        Assert.assertEquals("ExplanationOfBenefits", updatedJob.getResourceTypes());
        Assert.assertEquals("http://localhost", updatedJob.getRequestUrl());
        Assert.assertEquals("Pending", updatedJob.getStatusMessage());
        Assert.assertEquals(now, updatedJob.getExpiresAt());

        JobOutput updatedOutput = updatedJob.getJobOutput().get(0);
        Assert.assertEquals(false, updatedOutput.isError());
        Assert.assertEquals("ExplanationOfBenefits", updatedOutput.getFhirResourceType());
        Assert.assertEquals("file.ndjson", updatedOutput.getFilePath());

        JobOutput updatedErrorOutput = updatedJob.getJobOutput().get(1);
        Assert.assertEquals(true, updatedErrorOutput.isError());
        Assert.assertEquals(OPERATION_OUTCOME, updatedErrorOutput.getFhirResourceType());
        Assert.assertEquals("error.ndjson", updatedErrorOutput.getFilePath());
    }

    @Test
    public void getFileDownloadUrl() throws IOException {
        String testFile = "test.ndjson";
        String errorFile = "error.ndjson";
        Job job = createJobForFileDownloads(testFile, errorFile);

        String destinationStr = tmpJobLocation + job.getJobId();
        Path destination = Paths.get(destinationStr);
        Files.createDirectories(destination);

        createNDJSONFile(testFile, destinationStr);
        createNDJSONFile(errorFile, destinationStr);

        Resource resource = jobService.getResourceForJob(job.getJobId(), testFile);
        Assert.assertEquals(testFile, resource.getFilename());

        Resource errorResource = jobService.getResourceForJob(job.getJobId(), errorFile);
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
        Job job = jobService.createJob("ExplanationOfBenefits", "http://localhost:8080");
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime localDateTime = OffsetDateTime.now();
        job.setProgress(100);
        job.setLastPollTime(now);
        job.setStatus(JobStatus.IN_PROGRESS);
        job.setCreatedAt(localDateTime);
        job.setCompletedAt(localDateTime);
        job.setResourceTypes("ExplanationOfBenefits");
        job.setRequestUrl("http://localhost");
        job.setStatusMessage("Pending");
        job.setExpiresAt(now);

        JobOutput jobOutput = new JobOutput();
        jobOutput.setError(false);
        jobOutput.setFhirResourceType("ExplanationOfBenefits");
        jobOutput.setFilePath(fileName);
        jobOutput.setJob(job);

        JobOutput errorJobOutput = new JobOutput();
        errorJobOutput.setError(true);
        errorJobOutput.setFhirResourceType(OPERATION_OUTCOME);
        errorJobOutput.setFilePath(errorFileName);
        errorJobOutput.setJob(job);

        List output = List.of(jobOutput, errorJobOutput);
        job.setJobOutput(output);

        return jobService.updateJob(job);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void getFileDownloadUrlWithWrongFilename() throws IOException {
        String testFile = "test.ndjson";
        String errorFile = "error.ndjson";
        Job job = createJobForFileDownloads(testFile, errorFile);

        jobService.getResourceForJob(job.getJobId(), "filenamewrong.ndjson");
    }

    @Test(expected = JobOutputMissingException.class)
    public void getFileDownloadUrlWitMissingOutput() throws IOException {
        String testFile = "outputmissing.ndjson";
        String errorFile = "error.ndjson";
        Job job = createJobForFileDownloads(testFile, errorFile);

        jobService.getResourceForJob(job.getJobId(), "outputmissing.ndjson");
    }
}
