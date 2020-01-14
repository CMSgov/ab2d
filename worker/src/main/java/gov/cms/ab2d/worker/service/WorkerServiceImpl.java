package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.worker.processor.JobPreProcessor;
import gov.cms.ab2d.worker.processor.JobProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public void process(String jobUuid) {
        jobPreprocessor.preprocess(jobUuid);
        log.info("Job was put in progress");

        jobProcessor.process(jobUuid);
        log.info("Job was processed");
    }
}
