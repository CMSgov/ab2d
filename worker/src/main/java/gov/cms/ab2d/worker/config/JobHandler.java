package gov.cms.ab2d.worker.config;

import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobStatus;
import gov.cms.ab2d.common.service.FeatureEngagement;
import gov.cms.ab2d.common.service.ResourceNotFoundException;
import gov.cms.ab2d.worker.service.WorkerService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import static gov.cms.ab2d.common.util.Constants.JOB_LOG;


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

    public JobHandler(
            LockRegistry lockRegistry,
            WorkerService workerService) {
        this.lockRegistry = lockRegistry;
        this.workerService = workerService;
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

            // prototype jobs can be deferred and left for the worker to retry picking up when it's not as busy
            // TODO: remove when done with testing
            if (isPrototypeJob(submittedJob) && !workerService.isPrototypeAdmissible()) {
                log.info("{} is a prototype job and this worker is too busy to start one", jobId);
                MDC.remove(JOB_LOG);
                continue;
            }

            final Lock lock = lockRegistry.obtain(jobId);

            // Inability to obtain a lock means other worker is already taking care of the request
            // in which case we do nothing and return.
            if (lock.tryLock()) {
                try {
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
                    unlockQuietly(lock, jobId);
                }
            }
            MDC.remove(JOB_LOG);
        }
    }

    /**
     * Unlocks the lock, but if the lock was lost, just warn and don't do anything
     */
    private void unlockQuietly(Lock lock, String jobId) {
        try {
            lock.unlock();
        } catch (ConcurrentModificationException lockLost) {
            log.warn("lock for {} was lost, exiting",
                    jobId);
        }
    }

    private String getJobId(Map<String, Object> submittedJob) {
        return String.valueOf(submittedJob.get("job_uuid"));
    }

    private FhirVersion getFhirVersion(Map<String, Object> submittedJob) {
        return FhirVersion.valueOf(String.valueOf(submittedJob.get("fhir_version")));
    }

    private boolean isPrototypeJob(Map<String, Object> submittedJob) {
        return Boolean.TRUE.equals(submittedJob.get("pause_eligible"))
                && getFhirVersion(submittedJob) == FhirVersion.R4V3;
    }

}
