package gov.cms.ab2d.api.service;

import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.api.repository.JobRepository;
import gov.cms.ab2d.domain.Job;
import gov.cms.ab2d.domain.JobOutput;
import gov.cms.ab2d.domain.JobStatus;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.TransactionSystemException;

import javax.persistence.EntityNotFoundException;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static gov.cms.ab2d.api.service.JobServiceImpl.INITIAL_JOB_STATUS_MESSAGE;
import static gov.cms.ab2d.api.util.Constants.OPERATION_OUTCOME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringBootApp.class)
public class JobServiceTest {

    @Autowired
    JobService jobService;

    @Autowired
    JobRepository jobRepository;

    @Test
    public void createRequest() {
        Job job = jobService.createJob("ExplanationOfBenefits", "http://localhost:8080");
        assertThat(job).isNotNull();
        assertThat(job.getId()).isNotNull();
        assertThat(job.getJobID()).isNotNull();
        assertEquals(job.getProgress(), Integer.valueOf(0));
        assertEquals(job.getUser(), null); // null for now since no authentication
        assertEquals(job.getResourceTypes(), "ExplanationOfBenefits");
        assertEquals(job.getRequestURL(), "http://localhost:8080");
        assertEquals(job.getStatusMessage(), INITIAL_JOB_STATUS_MESSAGE);
        assertEquals(job.getStatus(), JobStatus.SUBMITTED);
        assertEquals(job.getJobOutput(), null);
        assertEquals(job.getLastPollTime(), null);
        assertEquals(job.getExpires(), null);
        assertThat(job.getJobID()).matches("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");

        // Verify it actually got persisted in the DB
        assertThat(jobRepository.findById(job.getId())).get().isEqualTo(job);
    }

    @Test(expected = TransactionSystemException.class)
    public void failedValidation() {
        jobService.createJob("Patient,ExplanationOfBenefits,Coverage", "http://localhost:8080");
    }

    @Test
    public void getJob() {
        Job job = jobService.createJob("ExplanationOfBenefits", "http://localhost:8080");

        Job retrievedJob = jobService.getJobByJobID(job.getJobID());

        assertEquals(job, retrievedJob);
    }

    @Test(expected = EntityNotFoundException.class)
    public void getNonExistentJob() {
        jobService.getJobByJobID("NonExistent");
    }

    @Test
    public void updateJob() {
        Job job = jobService.createJob("ExplanationOfBenefits", "http://localhost:8080");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime localDateTime = LocalDateTime.now();
        job.setProgress(100);
        job.setLastPollTime(now);
        job.setStatus(JobStatus.IN_PROGRESS);
        job.setCreatedAt(localDateTime);
        job.setCompletedAt(localDateTime);
        job.setJobID("abc");
        job.setResourceTypes("ExplanationOfBenefits");
        job.setRequestURL("http://localhost");
        job.setStatusMessage("Pending");
        job.setExpires(now);

        JobOutput jobOutput = new JobOutput();
        jobOutput.setError(false);
        jobOutput.setFhirResourceType("ExplanationOfBenefits");
        jobOutput.setFilePath("/output/file.ndjson");
        jobOutput.setJob(job);

        JobOutput errorJobOutput = new JobOutput();
        errorJobOutput.setError(true);
        errorJobOutput.setFhirResourceType(OPERATION_OUTCOME);
        errorJobOutput.setFilePath("http://localhost/path/error.ndjson");
        errorJobOutput.setJob(job);

        List output = List.of(jobOutput, errorJobOutput);
        job.setJobOutput(output);

        Job updatedJob = jobService.updateJob(job);
        Assert.assertEquals(Integer.valueOf(100), updatedJob.getProgress());
        Assert.assertEquals(now, updatedJob.getLastPollTime());
        Assert.assertEquals(JobStatus.IN_PROGRESS, updatedJob.getStatus());
        Assert.assertEquals(localDateTime, updatedJob.getCreatedAt());
        Assert.assertEquals(localDateTime, updatedJob.getCompletedAt());
        Assert.assertEquals("abc", updatedJob.getJobID());
        Assert.assertEquals("ExplanationOfBenefits", updatedJob.getResourceTypes());
        Assert.assertEquals("http://localhost", updatedJob.getRequestURL());
        Assert.assertEquals("Pending", updatedJob.getStatusMessage());
        Assert.assertEquals(now, updatedJob.getExpires());

        JobOutput updatedOutput = updatedJob.getJobOutput().get(0);
        Assert.assertEquals(false, updatedOutput.isError());
        Assert.assertEquals("ExplanationOfBenefits", updatedOutput.getFhirResourceType());
        Assert.assertEquals("/output/file.ndjson", updatedOutput.getFilePath());

        JobOutput updatedErrorOutput = updatedJob.getJobOutput().get(1);
        Assert.assertEquals(true, updatedErrorOutput.isError());
        Assert.assertEquals(OPERATION_OUTCOME, updatedErrorOutput.getFhirResourceType());
        Assert.assertEquals("http://localhost/path/error.ndjson", updatedErrorOutput.getFilePath());
    }
}
