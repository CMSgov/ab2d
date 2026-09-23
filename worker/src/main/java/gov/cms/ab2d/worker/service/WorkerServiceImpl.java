package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.coverage.service.v3.CoverageV3Service;
import gov.cms.ab2d.fhir.FhirVersion;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobStatus;
import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.common.service.FeatureEngagement;
import gov.cms.ab2d.worker.processor.JobPreProcessor;
import gov.cms.ab2d.worker.processor.JobProcessor;
import gov.cms.ab2d.worker.processor.prototype.PrototypeJobProcessor;
import gov.cms.ab2d.worker.processor.prototype.PrototypeProperties;
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

import static gov.cms.ab2d.common.util.PropertyConstants.PAUSE_RESUME_PROTOTYPE_ENABLED;
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
    private final CoverageV3Service coverageV3Service;
    private final PrototypeJobProcessor prototypeJobProcessor;
    private final PrototypeProperties prototypeProperties;

    private final List<String> activeJobs = Collections.synchronizedList(new ArrayList<>());

    // The subset of activeJobs that are prototype jobs
    private final Set<String> prototypeJobs = ConcurrentHashMap.newKeySet();

    @Override
    public Job process(String jobUuid) {

        activeJobs.add(jobUuid);
        try {
            Job job = jobPreprocessor.preprocess(jobUuid);

            if (job.getStatus() == JobStatus.IN_PROGRESS) {
                log.info("{} has been started", jobUuid);

                // The pause/resume prototype handles v3 jobs when the feature flag is on
                if (propertiesService.isToggleOn(PAUSE_RESUME_PROTOTYPE_ENABLED, false)
                        && job.getFhirVersion() == FhirVersion.R4V3
                        && job.isPauseEligible()) {
                    log.info("{} routed to pause/resume prototype processor", jobUuid);
                    prototypeJobs.add(jobUuid);
                    try {
                        job = prototypeJobProcessor.process(jobUuid);
                    } finally {
                        prototypeJobs.remove(jobUuid);
                    }
                } else {
                    // yield the prototype work if we get a real job
                    yieldPrototypeToRealWork();
                    if (job.getFhirVersion() == FhirVersion.R4V3) {
                        coverageV3Service.createAggregatedAttributionTable(job.getContractNumber());
                    }
                    job = jobProcessor.process(jobUuid);
                }
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
        return FeatureEngagement.fromString(propertiesService.getProperty(WORKER_ENGAGEMENT, FeatureEngagement.IN_GEAR.getSerialValue()));
    }

    @Override
    public boolean isPrototypeAdmissible() {
        if (!yieldingUnderLoad()) {
            return true;
        }
        return realJobCount() <= prototypeProperties.getRealJobTolerance();
    }

    /**
     * If the worker is busy, pause the prototype jobs
     */
    private void yieldPrototypeToRealWork() {
        if (!yieldingUnderLoad()) {
            return;
        }
        int realJobs = realJobCount();
        if (realJobs <= prototypeProperties.getRealJobTolerance()) {
            return;
        }
        log.info("All prototype jobs will be paused");
        prototypeJobProcessor.stopRunning();
    }

    /**
     * Check the property that decides if we yield prototype jobs at all
     */
    private boolean yieldingUnderLoad() {
        return propertiesService.isToggleOn(PAUSE_RESUME_PROTOTYPE_ENABLED, false)
                && prototypeProperties.isPauseUnderLoad();
    }

    /**
     * How many real jobs this worker is running
     */
    private int realJobCount() {
        return Math.max(0, activeJobs.size() - prototypeJobs.size());
    }

    /**
     * Signal any running prototype batch executions to stop and drain before Spring
     * tears down context
     */
    @EventListener(ContextClosedEvent.class)
    public void stopPrototypeJobsBeforeClose() {
        prototypeJobProcessor.stopForShutdown();
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
