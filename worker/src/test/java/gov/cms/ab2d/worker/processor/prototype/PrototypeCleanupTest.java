package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobStatus;
import gov.cms.ab2d.worker.processor.prototype.cleanup.PrototypeCleanupScheduler;
import gov.cms.ab2d.worker.processor.prototype.cleanup.PrototypeCleanupTasklet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static gov.cms.ab2d.common.util.PropertyConstants.PAUSE_RESUME_PROTOTYPE_ENABLED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Tests the batch metadata cleanup functions. Runs several test jobs with different circumstances, then
 * ages their metadata to be expired and makes sure the cleanup deletes expired metadata, but leaves everything
 * else alone.
 */
class PrototypeCleanupTest extends AbstractPrototypeRecoveryIntegrationTest {

    private static final int RETENTION_DAYS = 30;
    private static final int EXPIRED_DAYS = RETENTION_DAYS + 5;

    @Autowired
    private PrototypeCleanupTasklet cleanupTasklet;

    @Autowired
    private PrototypeCleanupScheduler cleanupScheduler;

    @Autowired
    private PrototypeProperties props;

    @Autowired
    private PropertiesService propertiesService;

    @BeforeEach
    void enableCleanup() {
        props.setCleanupRetentionDays(RETENTION_DAYS);
        props.setCleanupMaxExecutionsPerSweep(200);
        props.setCleanupEnabled(true);
        propertiesService.updateProperty(PAUSE_RESUME_PROTOTYPE_ENABLED, "true");
    }

    @Test
    @DisplayName("Cleanup cleans up old metadata and leaves current work alone")
    void cleanupDeletesIntendedTargets() {
        // ordinary successful job that is past retention, most common target
        Job succeeded = runJob("cleanup-done");

        // Crashed job with a failed status and unknown execution. It cannot be restarted.
        Job crashed = runJob("cleanup-crashed");

        // A job that has been paused for more than 30 days. Unlikely to happen but might as well cover it.
        Job paused = runJob("cleanup-paused");

        // A job in-progress
        Job running = runJob("cleanup-running");

        // A successful job that is not past retention
        Job recent = runJob("cleanup-recent");

        // none of the jobs will be cleaned up since their timestamps are still recent
        cleanupScheduler.sweepBatchHistory();
        assertTrue(cleanupExecutions() > 0, "the cleanup should have recorded an execution of its own");

        forceHardCrashState(crashed.getJobUuid(), "UNKNOWN");
        setStatus(crashed, JobStatus.FAILED);
        setStatus(paused, JobStatus.SUBMITTED);
        setStatus(running, JobStatus.IN_PROGRESS);

        age(succeeded);
        age(crashed);
        age(paused);
        age(running);
        ageCleanupHistory();

        assertTrue(metadataRows(succeeded) > 0, "the run should have left metadata behind to clean up");

        // run the task
        cleanupTasklet.execute(mock(StepContribution.class), null);

        assertEquals(0, metadataRows(succeeded), "a finished job past the retention window");
        assertEquals(0, metadataRows(crashed), "a failed job that can't be restarted");
        assertEquals(0, cleanupExecutions(), "an expired cleanup task");

        assertTrue(metadataRows(paused) > 0, "a paused job");
        assertTrue(metadataRows(running) > 0, "a job still in progress");
        assertTrue(metadataRows(recent) > 0, "a completed job still inside the retention window");

        // instances and leases follow suit
        assertEquals(0, orphanInstances(), "instances left with no executions should have been collected");
        assertEquals(0, countLease(succeeded.getJobUuid()), "a deleted job's lease is deleted");
        assertTrue(countLease(paused.getJobUuid()) > 0, "a paused job must keep its lease to be resumable");
        assertTrue(countLease(running.getJobUuid()) > 0, "a running job must keep its lease");

        // cleanup shouldn't delete any job rows
        for (Job job : List.of(succeeded, crashed, paused, running, recent)) {
            assertEquals(1, countJobRow(job.getJobUuid()),
                    "the cleanup must not delete job rows: " + job.getJobUuid());
        }
    }

    /** Create and run a v3 job */
    private Job runJob(String uuidPrefix) {
        Job job = createSubmittedV3Job(uuidPrefix);
        prototypeJobProcessor.process(job.getJobUuid());
        return jobRepository.findByJobUuid(job.getJobUuid());
    }

    /** Modifies job metadata to push execution timestamps back in time so that the data appears expired */
    private void age(Job job) {
        jdbc.update("UPDATE batch_job_execution je "
                        + "SET create_time = je.create_time - make_interval(days => ?), "
                        + "    start_time = je.start_time - make_interval(days => ?), "
                        + "    end_time = je.end_time - make_interval(days => ?) "
                        + "FROM batch_job_execution_params p "
                        + "WHERE p.job_execution_id = je.job_execution_id "
                        + "  AND p.parameter_name = 'jobUuid' AND p.parameter_value = ?",
                PrototypeCleanupTest.EXPIRED_DAYS, PrototypeCleanupTest.EXPIRED_DAYS, PrototypeCleanupTest.EXPIRED_DAYS, job.getJobUuid());
    }

    /** same as above but for cleanup task metadata */
    private void ageCleanupHistory() {
        jdbc.update("UPDATE batch_job_execution je "
                        + "SET create_time = je.create_time - make_interval(days => ?), "
                        + "    start_time = je.start_time - make_interval(days => ?), "
                        + "    end_time = je.end_time - make_interval(days => ?) "
                        + "FROM batch_job_instance bi "
                        + "WHERE bi.job_instance_id = je.job_instance_id AND bi.job_name = ?",
                PrototypeCleanupTest.EXPIRED_DAYS, PrototypeCleanupTest.EXPIRED_DAYS, PrototypeCleanupTest.EXPIRED_DAYS, PrototypeCleanupScheduler.CLEANUP_JOB_NAME);
    }

    private void setStatus(Job job, JobStatus status) {
        Job current = jobRepository.findByJobUuid(job.getJobUuid());
        current.setStatus(status);
        jobRepository.saveAndFlush(current);
    }

    /** Every batch metadata row belonging to a job, across all five tables. */
    private int metadataRows(Job job) {
        return count("""
                WITH execs AS (
                    SELECT DISTINCT je.job_execution_id
                      FROM batch_job_execution je
                      JOIN batch_job_execution_params p ON p.job_execution_id = je.job_execution_id
                     WHERE p.parameter_name = 'jobUuid' AND p.parameter_value = ?
                )
                SELECT (SELECT count(*) FROM batch_job_execution
                         WHERE job_execution_id IN (SELECT job_execution_id FROM execs))
                     + (SELECT count(*) FROM batch_job_execution_params
                         WHERE job_execution_id IN (SELECT job_execution_id FROM execs))
                     + (SELECT count(*) FROM batch_job_execution_context
                         WHERE job_execution_id IN (SELECT job_execution_id FROM execs))
                     + (SELECT count(*) FROM batch_step_execution
                         WHERE job_execution_id IN (SELECT job_execution_id FROM execs))
                     + (SELECT count(*) FROM batch_step_execution_context
                         WHERE step_execution_id IN (SELECT step_execution_id FROM batch_step_execution
                                 WHERE job_execution_id IN (SELECT job_execution_id FROM execs)))
                """, job.getJobUuid());
    }

    private int cleanupExecutions() {
        return count("SELECT count(*) FROM batch_job_execution je "
                        + "JOIN batch_job_instance bi ON bi.job_instance_id = je.job_instance_id "
                        + "WHERE bi.job_name = ?",
                PrototypeCleanupScheduler.CLEANUP_JOB_NAME);
    }

    private int orphanInstances() {
        return count("SELECT count(*) FROM batch_job_instance bi WHERE NOT EXISTS "
                + "(SELECT 1 FROM batch_job_execution je "
                + "  WHERE je.job_instance_id = bi.job_instance_id)");
    }

    private int countLease(String jobUuid) {
        return count("SELECT count(*) FROM ab2d.job_lease WHERE job_uuid = ?", jobUuid);
    }

    private int countJobRow(String jobUuid) {
        return count("SELECT count(*) FROM job WHERE job_uuid = ?", jobUuid);
    }

    private int count(String sql, Object... args) {
        Integer count = jdbc.queryForObject(sql, Integer.class, args);
        return count == null ? 0 : count;
    }
}
