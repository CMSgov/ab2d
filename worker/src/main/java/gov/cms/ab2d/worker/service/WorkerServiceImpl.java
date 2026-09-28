package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.coverage.service.v3.CoverageV3Service;
import gov.cms.ab2d.coverage.service.v3.CoverageV3SyncResult;
import gov.cms.ab2d.fhir.FhirVersion;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobStatus;
import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.common.service.FeatureEngagement;
import gov.cms.ab2d.worker.processor.JobPreProcessor;
import gov.cms.ab2d.worker.processor.JobProcessor;
import gov.cms.ab2d.worker.processor.coverage.CoverageV3SyncException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static gov.cms.ab2d.common.util.PropertyConstants.WORKER_ENGAGEMENT;
import static gov.cms.ab2d.coverage.service.v3.CoverageV3SyncResult.*;
import static gov.cms.ab2d.coverage.service.v3.CoverageV3SyncSource.JOB_HANDLER;

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
    // Time to wait before retrying to sync v3 coverage if IDR importer is in progress
    private Duration waitTimeIfIdrImporterInProgress = Duration.ofMinutes(3);

    private final List<String> activeJobs = Collections.synchronizedList(new ArrayList<>());

    @Override
    public Job process(String jobUuid) {

        activeJobs.add(jobUuid);
        try {
            Job job = jobPreprocessor.preprocess(jobUuid);

            if (job.getStatus() == JobStatus.IN_PROGRESS) {
                log.info("{} has been started", jobUuid);

                if (job.getFhirVersion() == FhirVersion.R4V3) {
                    log.info("Attempting to sync v3 coverage before creating aggregated table for {}", job.getContractNumber());
                    trySyncCoverageV3(job.getContractNumber());
                    coverageV3Service.createAggregatedAttributionTable(job.getContractNumber());
                }

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
        return FeatureEngagement.fromString(propertiesService.getProperty(WORKER_ENGAGEMENT, FeatureEngagement.IN_GEAR.getSerialValue()));
    }

    @PreDestroy
    public void resetInProgressJobs() {
        log.info("Shutdown in progress ... Do house keeping ...");

        if (!activeJobs.isEmpty()) {
            shutDownService.resetInProgressJobs(activeJobs);
        }

        log.info("House keeping done - Shutting down");
    }

    void setWaitTimeIfIdrImporterInProgress(Duration waitTimeIfIdrImporterInProgress) {
        if (waitTimeIfIdrImporterInProgress != null) {
            this.waitTimeIfIdrImporterInProgress = waitTimeIfIdrImporterInProgress;
        }
    }

    private void trySyncCoverageV3(String contract) throws CoverageV3SyncException {
        log.info("Calling moveOldCoverageToHistoricalCoverage() for contract {}", contract);
        coverageV3Service.moveOldCoverageToHistoricalCoverage(contract, JOB_HANDLER);

        var attempts = 0;
        val maxAttempts = 3;
        CoverageV3SyncResult result = null;
        while (attempts < maxAttempts) {
            log.info("Calling moveFromStagingToRecentCoverage() for contract {}", contract);
            result = coverageV3Service.moveFromStagingToRecentCoverage(contract, JOB_HANDLER);
            attempts++;

            if (result == SYNC_SUCCESSFUL_FOR_CONTRACT ||
                result == NO_COVERAGE_FOUND_FOR_CONTRACT) {
                log.info("moveFromStagingToRecentCoverage() completed with {}", result);
                return;
            }
            else if (result == IDR_IMPORTER_IN_PROGRESS) {
                log.info("moveFromStagingToRecentCoverage() returned {}; Waiting {} seconds before retrying sync",
                    result,
                    waitTimeIfIdrImporterInProgress.toSeconds()
                );
                try {
                    Thread.sleep(waitTimeIfIdrImporterInProgress.toMillis());
                } catch (InterruptedException e) {
                    throw new CoverageV3SyncException("Error sleeping thread inside trySyncCoverageV3()", e);
                }
            } else {
                log.warn("moveFromStagingToRecentCoverage() returned {}; Retrying sync", result);
            }
        }

        throw new CoverageV3SyncException(
            "trySyncCoverageV3 failed with %s after %s attempts".formatted(result, maxAttempts)
        );
    }
}
