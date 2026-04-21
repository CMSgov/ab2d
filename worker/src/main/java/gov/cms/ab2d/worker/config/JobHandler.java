package gov.cms.ab2d.worker.config;

import gov.cms.ab2d.coverage.service.v3.CoverageV3LockWrapper;
import gov.cms.ab2d.coverage.service.v3.CoverageV3Service;
import gov.cms.ab2d.fhir.FhirVersion;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobStatus;
import gov.cms.ab2d.common.service.FeatureEngagement;
import gov.cms.ab2d.common.service.ResourceNotFoundException;
import gov.cms.ab2d.worker.service.WorkerService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.slf4j.MDC;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import static gov.cms.ab2d.common.util.Constants.JOB_LOG;
import static gov.cms.ab2d.coverage.service.v3.CoverageV3SyncSource.JOB_HANDLER;


/**
 * This handler gets triggered when a job is submitted into the job table.
 * Spring Integration polls the jobs table in the database
 * And when a new record is inserted into the jobs table, Spring Integration streams into the subscribable executor channel.
 * It locks the job for processing via a database-centric lock and then delegates to a service for processing.
 */
@Slf4j
@Component
public class JobHandler implements MessageHandler {

    /**
     * Export requests must be locked globally to avoid race conditions among workers,
     * which is important in a distributed deployments such as ours.
     */
    private final LockRegistry lockRegistry;
    private final WorkerService workerService;
    private final CoverageV3Service coverageV3Service;
    private final CoverageV3LockWrapper coverageV3LockWrapper;

    public JobHandler(
            LockRegistry lockRegistry,
            WorkerService workerService,
            CoverageV3Service coverageV3Service,
            CoverageV3LockWrapper coverageV3LockWrapper) {
        this.lockRegistry = lockRegistry;
        this.workerService = workerService;
        this.coverageV3Service = coverageV3Service;
        this.coverageV3LockWrapper = coverageV3LockWrapper;
    }

    @Override
    public void handleMessage(Message<?> message) {

        // Worker is not able to be engaged in processing
        if (workerService.getEngagement() == FeatureEngagement.NEUTRAL) {
            return;
        }

        final List<Map<String, Object>> payload = (List<Map<String, Object>>) message.getPayload();

        if (!payload.isEmpty()) {
            log.info("iterating over {} submitted jobs to attempt to find one to start", payload.size());
        }

        for (Map<String, Object> submittedJob : payload) {

            final String jobId = getJobId(submittedJob);

            MDC.put(JOB_LOG, jobId);

            final Lock lock = lockRegistry.obtain(jobId);

            // Inability to obtain a lock means other worker is already taking care of the request
            // in which case we do nothing and return.
            if (lock.tryLock()) {

                try {

                    var syncCoverageV3Successful = true;
                    try {
                        syncCoverageV3Successful = trySyncCoverageV3(submittedJob);
                    } catch (Exception e) {
                        log.error("Error calling trySyncCoverageV3", e);
                        throw e;
                    }

                    if (!syncCoverageV3Successful) {
                        throw new IllegalStateException("trySyncCoverageV3 returned false after several attempts");
                    }

                    // Attempt to start (mark an eob job as in progress) an eob job.
                    // A job may not be started if the workers are busy or if coverage metadata needs an update.
                    Job job = workerService.process(jobId);
                    if (job.getStatus() == JobStatus.IN_PROGRESS) {
                        log.info("{} job has been started so exiting loop", jobId);
                        break;
                    }

                } catch (ResourceNotFoundException rnfe) {
                    throw new MessagingException("could not find job in database for " + jobId + " job uuid", rnfe);
                } catch (Exception exception) {
                    throw new MessagingException("could not check coverage due to unexpected exception", exception);
                } finally {
                    lock.unlock();
                }
            }

            MDC.remove(JOB_LOG);
        }
    }

    private String getJobId(Map<String, Object> submittedJob) {
        return String.valueOf(submittedJob.get("job_uuid"));
    }

    private String getContractNumber(Map<String, Object> submittedJob) {
        return String.valueOf(submittedJob.get("contract_number"));
    }

    private FhirVersion getFhirVersion(Map<String, Object> submittedJob) {
        return FhirVersion.valueOf(String.valueOf(submittedJob.get("fhir_version")));
    }

    private boolean trySyncCoverageV3(Map<String, Object> submittedJob) throws InterruptedException {
        val fhirVersion = getFhirVersion(submittedJob);
        if (fhirVersion != FhirVersion.R4V3) {
            return true;
        }

        // TODO update handling here and retry here

        val contract = getContractNumber(submittedJob);
        coverageV3Service.moveOldCoverageToHistoricalCoverage(contract, JOB_HANDLER);
        coverageV3Service.moveFromStagingToRecentCoverage(contract, JOB_HANDLER);
        return true;

        /*
        var movedOldCoverage = false;
        var movedOldCoverageAttempts = 1;
        while (movedOldCoverageAttempts <= 5 && !(movedOldCoverage=coverageV3Service.moveOldCoverageToHistoricalCoverage(contract, JOB_HANDLER))) {
            log.info("Sleeping 5 seconds for movedOldCoverage");
            Thread.sleep(5000);
            movedOldCoverageAttempts++;
        }

        if (!movedOldCoverage) {
            log.info("Unable to complete movedOldCoverage step");
            return false;
        }

        var movedFromStaging = false;
        var movedFromStagingAttempts = 1;
        while (movedFromStagingAttempts <= 5 && !(movedFromStaging=coverageV3Service.moveFromStagingToRecentCoverage(contract, JOB_HANDLER))) {
            log.info("Sleeping 5 seconds for movedFromStaging");
            Thread.sleep(5000);
            movedFromStagingAttempts++;
        }

        if (!movedFromStaging) {
            log.info("Unable to complete movedFromStaging step");
            return false;
        }

        return true;
        */
    }

}
