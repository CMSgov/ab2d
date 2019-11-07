package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.common.model.Job;
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
        final Job job = jobService.putJobInProgress(jobId);
        doLongRunningWork();
        jobService.completeJob(job);
    }



    private void doLongRunningWork() {
        log.info("Sleeping for 5 seconds...");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }




}
