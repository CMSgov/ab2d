package gov.cms.ab2d.worker;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.repository.JobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * This class is responsible for actually processing the job and preparing bulk downloads for users.
 */
@Slf4j
@Service
public class WorkerService {

    private final JobRepository jobRepository;

    public WorkerService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public void process(String jobId) throws IOException {

        final Job job = jobRepository.findByJobId(jobId);

        putJobInProgress(job);
        doLongRunningWork();
        completeJob(job);
    }

    private void putJobInProgress(Job job) {
        job.setStatus(JobStatus.IN_PROGRESS);
        log.info("Job [{}] is IN_PROGRESS", job.getId());
        jobRepository.save(job);
    }

    private void doLongRunningWork() {
        log.info("Sleeping for 5 seconds...");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void completeJob(Job job) {
        job.setStatus(JobStatus.SUCCESSFUL);
        job.setStatusMessage("100%");
        job.setExpiresAt(job.getCreatedAt().plusDays(1));

        jobRepository.save(job);
        log.info("Job: [{}] is DONE", job.getId());
    }


}
