package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.fhir.FhirVersion;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobStatus;
import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.common.service.FeatureEngagement;
import gov.cms.ab2d.worker.processor.JobPreProcessor;
import gov.cms.ab2d.worker.processor.JobProcessor;
import gov.cms.ab2d.worker.processor.pauseresume.PauseResumeJobProcessor;
import gov.cms.ab2d.worker.processor.pauseresume.PauseResumeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static gov.cms.ab2d.common.util.PropertyConstants.PAUSE_RESUME_ENABLED;
import static gov.cms.ab2d.common.util.PropertyConstants.WORKER_ENGAGEMENT;

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
    private final PauseResumeJobProcessor pauseResumeJobProcessor;
    private final PauseResumeProperties pauseResumeProperties;

    private final List<String> activeJobs = Collections.synchronizedList(new ArrayList<>());

    // The subset of activeJobs that are pause/resume jobs
    private final Set<String> pauseResumeJobs = ConcurrentHashMap.newKeySet();

    @Override
    public Job process(String jobUuid) {

        activeJobs.add(jobUuid);
        try {
            Job job = jobPreprocessor.preprocess(jobUuid);

            if (job.getStatus() == JobStatus.IN_PROGRESS) {
                log.info("{} has been started", jobUuid);

                // The pause/resume handles v3 jobs when the feature flag is on
                if (propertiesService.isToggleOn(PAUSE_RESUME_ENABLED, false)
                        && job.getFhirVersion() == FhirVersion.R4V3
                        && job.isPauseEligible()) {
                    log.info("{} routed to pause/resume processor", jobUuid);
                    pauseResumeJobs.add(jobUuid);
                    try {
                        job = pauseResumeJobProcessor.process(jobUuid);
                    } finally {
                        pauseResumeJobs.remove(jobUuid);
                    }
                } else {
                    // no-op unless pause-resume.pause-under-load is on
                    yieldPauseResumeToRealWork();
                    job = jobProcessor.process(jobUuid);
                }
                log.info("Job was processed");
            } else if (job.getStatus() == JobStatus.SUBMITTED) {
                log.debug("{} job is waiting for enrollment information", jobUuid);
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
        return FeatureEngagement.fromString(propertiesService.getProperty(WORKER_ENGAGEMENT, FeatureEngagement.IN_GEAR.getSerialValue()));
    }

    /**
     * @deprecated unused, but potentially useful for self-pausing test jobs
     */
    @Deprecated
    @Override
    public boolean isPauseResumeAdmissible() {
        if (!yieldingUnderLoad()) {
            return true;
        }
        return realJobCount() <= pauseResumeProperties.getRealJobTolerance();
    }

    /**
     * If the worker is busy, pause the running pause/resume jobs
     *
     * @deprecated unused, but potentially useful for self-pausing test jobs
     */
    @Deprecated
    private void yieldPauseResumeToRealWork() {
        if (!yieldingUnderLoad()) {
            return;
        }
        int realJobs = realJobCount();
        if (realJobs <= pauseResumeProperties.getRealJobTolerance()) {
            return;
        }
        log.info("All pause/resume jobs will be paused");
        pauseResumeJobProcessor.stopRunning(pauseResumeJobs);
    }

    /**
     * Check the property that decides if we yield pause/resume jobs at all
     */
    private boolean yieldingUnderLoad() {
        return propertiesService.isToggleOn(PAUSE_RESUME_ENABLED, false)
                && pauseResumeProperties.isPauseUnderLoad();
    }

    /**
     * How many real jobs this worker is running
     */
    private int realJobCount() {
        return Math.max(0, activeJobs.size() - pauseResumeJobs.size());
    }

    /**
     * Signal any running pause/resume batch executions to stop and drain before Spring
     * tears down context
     */
    @EventListener(ContextClosedEvent.class)
    public void stopPauseResumeJobsBeforeClose() {
        pauseResumeJobProcessor.stopForShutdown(pauseResumeJobs);
    }

    @PreDestroy
    public void resetInProgressJobs() {
        log.info("Shutdown in progress ... Do house keeping ...");

        // this is a stopgap to prevent jobs from being forced to hard-recovery
        // pause/resume jobs handle their own shutdown behavior.
        List<String> jobsToReset = new ArrayList<>(activeJobs);
        jobsToReset.removeAll(pauseResumeJobs);

        if (!jobsToReset.isEmpty()) {
            shutDownService.resetInProgressJobs(jobsToReset);
        }

        log.info("House keeping done - Shutting down");
    }
}
