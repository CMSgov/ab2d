package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.worker.processor.JobPreProcessor;
import gov.cms.ab2d.worker.processor.JobProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * This class is responsible for actually processing the job and preparing bulk downloads for users.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerServiceImpl implements WorkerService {

    private final JobPreProcessor jobPreprocessor;
    private final JobProcessor jobProcessor;

    @Value("${delay-processing}")
    private String delayProcessing;

    @Override
    public void process(String jobUuid) {
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

        jobPreprocessor.preprocess(jobUuid);
        log.info("Job was put in progress");

        jobProcessor.process(jobUuid);
        log.info("Job was processed");
    }
}
