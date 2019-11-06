package gov.cms.ab2d.worker;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.repository.SponsorRepository;
import gov.cms.ab2d.common.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.util.Random;

import static gov.cms.ab2d.common.model.JobStatus.SUCCESSFUL;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@Slf4j
@SpringBootTest
public class WorkerServiceIT {
    @Autowired private JobRepository jobRepository;
    @Autowired private SponsorRepository sponsorRepository;
    @Autowired private UserRepository userRepository;

    @Autowired private WorkerService cut;


    @Test
    @DisplayName("When multiple jobs are submitted into the job table, they are processed in parallel by the worker")
    void whenTwoJobsSubmittedWorkerProcessesBothInParallel() throws InterruptedException {

        final User user = createUser();
        Job submittedJob3 = createJob(3, user);
        Job submittedJob4 = createJob(4, user);

        Thread.sleep(7500L);

        final Job processedJob3 = jobRepository.findByJobId(submittedJob3.getJobId());
        final Job processedJob4 = jobRepository.findByJobId(submittedJob4.getJobId());

        verifyJobResult(processedJob3);
        verifyJobResult(processedJob4);
    }



    private Job createJob(long id, final User user) {
        Job job = new Job();
        job.setId(id);
        job.setJobId(String.valueOf(id));
        job.setStatus(JobStatus.SUBMITTED);
        job.setStatusMessage("0%");
        job.setResourceTypes("ExplanationOfBenefits");
        job.setCreatedAt(OffsetDateTime.now());
        job.setUser(user);
        return jobRepository.save(job);
    }

    private User createUser() {
        final User user = new User();
        user.setUserName("testuser" + new Random().nextInt());
        user.setFirstName("test");
        user.setSponsor(createSponsor());
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private Sponsor createSponsor() {
        final Sponsor sponsor = new Sponsor();
        sponsor.setHpmsId(new Random().nextInt());
        sponsor.setOrgName("BCBS");
        return sponsorRepository.save(sponsor);
    }


    private void verifyJobResult(Job processedJob) {
        assertThat(processedJob.getStatus(), equalTo(SUCCESSFUL));
        assertThat(processedJob.getStatusMessage(), equalTo("100%"));
    }

}