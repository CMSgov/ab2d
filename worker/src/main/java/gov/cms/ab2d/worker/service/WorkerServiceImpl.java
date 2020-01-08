package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.worker.processor.JobPreProcessor;
import gov.cms.ab2d.worker.processor.JobProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class is responsible for actually processing the job and preparing bulk downloads for users.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerServiceImpl implements WorkerService {

    private final JobPreProcessor jobPreprocessor;
    private final JobProcessor jobProcessor;
    private final ShutDownService shutDownService;

    private static List<String> activeJobs = Collections.synchronizedList(new ArrayList<>());


    @Override
    public void process(String jobUuid) {

        activeJobs.add(jobUuid);

        jobPreprocessor.preprocess(jobUuid);
        log.info("Job was put in progress");

        jobProcessor.process(jobUuid);
        log.info("Job was processed");

        activeJobs.remove(jobUuid);
    }



    @PreDestroy
    public void resetInProgressJobs() {
        log.info("Shutdown in progress ... Do house cleaning ...");

        if (!activeJobs.isEmpty()) {
            shutDownService.resetInProgressJobs(activeJobs);
        }

        log.info("House cleaning done - Shutting down");
    }


}
