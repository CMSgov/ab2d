package gov.cms.ab2d.worker;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.repository.JobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

/**
 * This class is responsible for actually processing requests and preparing bulk downloads for users.
 */
@Slf4j
@Service
@Transactional
public class WorkerService {

    private final JobRepository jobRepository;

    public WorkerService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public void process(String jobId) throws IOException {
        log.info("inside process Request ...");

        final Job job = getJob(jobId);
        putJobInProgress(job);
        doLongRunningWork();
        completeJob(job);

    }

    private void putJobInProgress(Job job) {
        job.setStatus(JobStatus.IN_PROGRESS);
        log.info("Job [{}] is IN_PROGRESS status", job.getId());
        jobRepository.saveAndFlush(job);
    }

    private void doLongRunningWork() {
        log.info("Sleeping for 5 seconds...");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("Awake from sleep");
    }

    private void completeJob(Job job) {
        job.setStatus(JobStatus.SUCCESSFUL);

        job.setStatusMessage("100%");
        job.setExpiresAt(job.getCreatedAt().plusDays(1));
        job.setResourceTypes("ExplanationOfBenefits");

        log.info("Update current job as done : {} ", job.getId());
        jobRepository.save(job);

        log.info("DONE. Request Processed Virtually. STOP.");
    }

    private Job getJob(String jobId) {
        return jobRepository.findByJobId(jobId);
    }

}
