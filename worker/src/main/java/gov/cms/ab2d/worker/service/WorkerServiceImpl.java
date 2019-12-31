package gov.cms.ab2d.worker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * This class is responsible for actually processing the job and preparing bulk downloads for users.
 */
@Slf4j
@Service
public class WorkerServiceImpl implements WorkerService {

    private final JobProcessingService jobService;

    @Value("${delay-processing}")
    private String delayProcessing;

    public WorkerServiceImpl(JobProcessingService jobService) {
        this.jobService = jobService;
    }

    @Override
    public void process(String jobId) {
        // This exists for the e2e tests so that when a status request comes in after the export, there is enough time
        // so that the job doesn't get processed and finishes. This way incomplete statuses can be tested. This should never
        // run in any other environment
        if (delayProcessing.equals("true")) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                log.error("Thread was interrupted while sleeping for e2e tests", e);
            }
        }

        jobService.putJobInProgress(jobId);
        log.info("Job was put in progress");
        jobService.processJob(jobId);
        log.info("Job was processed");
    }



}
