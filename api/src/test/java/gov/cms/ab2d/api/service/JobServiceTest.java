package gov.cms.ab2d.api.service;

import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.api.repository.JobRepository;
import gov.cms.ab2d.domain.Job;
import gov.cms.ab2d.domain.JobStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.TransactionSystemException;

import static gov.cms.ab2d.api.service.JobServiceImpl.INITIAL_JOB_STATUS_MESSAGE;
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
        assertThat(job.getJobID()).matches("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");

        // Verify it actually got persisted in the DB
        assertThat(jobRepository.findById(job.getId())).get().isEqualTo(job);
    }

    @Test(expected = TransactionSystemException.class)
    public void failedValidation() {
        jobService.createJob("Patient,ExplanationOfBenefits,Coverage", "http://localhost:8080");
    }
}
