package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobStatus;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.service.FeatureEngagement;
import gov.cms.ab2d.common.util.Constants;
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
 * This class is responsible for actually processing the job and preparing bulk downloads for clients.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerServiceImpl implements WorkerService {

    private final JobPreProcessor jobPreprocessor;
    private final JobProcessor jobProcessor;
    private final ShutDownService shutDownService;
    private final PropertiesService propertiesService;

    private final List<String> activeJobs = Collections.synchronizedList(new ArrayList<>());

    @Override
    public Job process(String jobUuid) {

        activeJobs.add(jobUuid);
        try {
            Job job = jobPreprocessor.preprocess(jobUuid);

            if (job.getStatus() == JobStatus.IN_PROGRESS) {
                log.info("{} has been started", jobUuid);

                job = jobProcessor.process(jobUuid);
                log.info("Job was processed");
            } else if (job.getStatus() == JobStatus.SUBMITTED) {
                log.info("{} job is waiting for enrollment information", jobUuid);
            } else if (job.getStatus() == JobStatus.CANCELLED) {
                log.warn("{} job has been cancelled", jobUuid);
            } else if (job.getStatus() == JobStatus.FAILED) {
                log.warn("{} job has failed to start", jobUuid);
            }

            // Check that job hasn't been cancelled by processor and that we actually changed
            // the state of the job
            return job;

        } finally {
            activeJobs.remove(jobUuid);
        }
    }

    @Override
    public FeatureEngagement getEngagement() {
        return FeatureEngagement.fromString(propertiesService.getPropertiesByKey(Constants.WORKER_ENGAGEMENT).getValue());
    }

    @PreDestroy
    public void resetInProgressJobs() {
        log.info("Shutdown in progress ... Do house keeping ...");

        if (!activeJobs.isEmpty()) {
            shutDownService.resetInProgressJobs(activeJobs);
        }

        log.info("House keeping done - Shutting down");
    }
}
