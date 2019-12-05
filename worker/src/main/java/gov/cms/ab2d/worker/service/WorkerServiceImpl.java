package gov.cms.ab2d.worker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * This class is responsible for actually processing the job and preparing bulk downloads for users.
 */
@Slf4j
@Service
public class WorkerServiceImpl implements WorkerService {

    private final JobProcessingService jobService;

    public WorkerServiceImpl(JobProcessingService jobService) {
        this.jobService = jobService;
    }

    @Override
    public void process(String jobId) {
        jobService.putJobInProgress(jobId);
        log.info("Job was put in progress");
        jobService.processJob(jobId);
        log.info("Job was processed");
    }



}
