package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hard-recovery tests for scenarios where the old owner of the job is truly dead/crashed.
 * The job should be healed and restarted.
 * Hard-recovery currently restarts partitions from scratch for safety, but there is opportunity
 * to recover old partitions so long as they can be knowably healed (i.e. they are not UNKNOWN status)
 * Completed partitions get skipped either way, so they are not redone.
 */
class PrototypeHardRecoveryIntegrationTest extends AbstractPrototypeRecoveryIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(PrototypeHardRecoveryIntegrationTest.class);

    @Autowired
    private PrototypeBatchMetadataRepository batchMeta;

    @Test
    @DisplayName("A hard-crashed job is recovered and completes")
    void hardCrashWithNoZombieRecoversAndCompletes() throws Exception {
        Job job = createSubmittedV3Job("hard-crash");
        String uuid = job.getJobUuid();

        // Owner A runs until one partition has COMPLETED, then its thread is abandoned
        RunningWorker owner = startWorkerUntilOnePartitionDone(uuid, "test-crashing-owner");
        long tokenBeforeCrash = currentToken(uuid);
        owner.killHard();
        forceHardCrashState(uuid, "STARTED");

        int processedBeforeRecovery = processedLog.size();
        assertTrue(processedBeforeRecovery > 0 && processedBeforeRecovery < TOTAL_BENES,
                "expected partial progress before the crash, saw " + processedBeforeRecovery + " of " + TOTAL_BENES);

        // A peer picks the in progress job up and heals it before restarting
        log.info("=== recovering hard-crashed job {} (crashed under token {}) ===", uuid, tokenBeforeCrash);
        Job recovered = prototypeJobProcessor.process(uuid);

        assertEquals(JobStatus.SUCCESSFUL, recovered.getStatus(), "a hard-crashed job should recover to SUCCESSFUL");
        assertTrue(currentToken(uuid) > tokenBeforeCrash,
                "hard recovery must bump the token since it is not a soft resume");
        assertFalse(workerStepStatuses(uuid).contains("STARTED"),
                "the healed, then restarted, job should leave no step STARTED");
        assertEveryBeneExactlyOnceInOutput(uuid);
    }

    @Test
    @DisplayName("A hard crash that left an UNKNOWN step is healed and recovered")
    void hardCrashWithUnknownStepIsHealedAndRecovers() throws Exception {
        Job job = createSubmittedV3Job("unknown-step");
        String uuid = job.getJobUuid();

        RunningWorker owner = startWorkerUntilOnePartitionDone(uuid, "test-unknown-owner");
        long tokenBeforeCrash = currentToken(uuid);
        owner.killHard();
        // In rare cases a batch metadata write can leave a step in UNKNOWN status, and Spring Batch
        // will refuse to restart the job at all.
        // We avoid this problem by downgrading UNKNOWN to FAILED. Since we restart the partition
        // anyway and put the output in a new file, there's no risk that this causes an issue when restarting.
        forceHardCrashState(uuid, "UNKNOWN");
        assertTrue(workerStepStatuses(uuid).contains("UNKNOWN"), "precondition: an UNKNOWN step should be present");

        Job recovered = prototypeJobProcessor.process(uuid);

        assertEquals(JobStatus.SUCCESSFUL, recovered.getStatus(),
                "the heal should downgrade UNKNOWN -> FAILED so the restart can proceed to SUCCESSFUL");
        assertTrue(currentToken(uuid) > tokenBeforeCrash, "hard recovery bumps the token");
        assertEveryBeneExactlyOnceInOutput(uuid);
    }

    @Test
    @DisplayName("healIndeterminateExecutions downgrades indeterminate steps to FAILED and leaves COMPLETED ones untouched")
    void healDowngradesIndeterminateStepsButNotCompletedOnes() throws Exception {
        Job job = createSubmittedV3Job("heal-wb");
        String uuid = job.getJobUuid();

        // Complete one partition and put another one in-flight, then abandon the run.
        RunningWorker owner = startWorkerUntilOnePartitionDone(uuid, "test-heal-owner");
        owner.killHard();

        // Grab a completed step and make sure it does NOT get healed/restarted
        Long completedStepId = jdbc.queryForObject(
                "SELECT se.step_execution_id FROM batch_step_execution se "
                        + "JOIN batch_job_execution_params p ON p.job_execution_id = se.job_execution_id "
                        + "WHERE p.parameter_name = 'jobUuid' AND p.parameter_value = ? "
                        + "  AND se.step_name LIKE ? AND se.status = 'COMPLETED' "
                        + "ORDER BY se.step_execution_id LIMIT 1",
                Long.class, uuid, WORKER_STEP_LIKE);

        // Inject some indeterminate statuses
        forceHardCrashState(uuid, "UNKNOWN");
        assertEquals("COMPLETED", stepStatus(completedStepId), "precondition: the completed step is still COMPLETED");

        int healed = batchMeta.healIndeterminateExecutions(uuid);

        assertTrue(healed >= 1, "heal should have downgraded at least one indeterminate step, healed=" + healed);
        List<String> statuses = workerStepStatuses(uuid);
        assertFalse(statuses.contains("UNKNOWN"), "heal must downgrade UNKNOWN steps, saw " + statuses);
        assertFalse(statuses.contains("STARTED"), "heal must downgrade STARTED steps, saw " + statuses);
        assertEquals("COMPLETED", stepStatus(completedStepId),
                "heal must NOT touch a COMPLETED step (finished partitions must survive a restart)");
        assertEquals("FAILED", jobExecutionStatus(uuid),
                "heal must downgrade the indeterminate job execution to a restartable FAILED");
    }

    private String stepStatus(Long stepExecutionId) {
        return jdbc.queryForObject("SELECT status FROM batch_step_execution WHERE step_execution_id = ?",
                String.class, stepExecutionId);
    }

    private String jobExecutionStatus(String uuid) {
        return jdbc.queryForObject(
                "SELECT je.status FROM batch_job_execution je "
                        + "JOIN batch_job_execution_params p ON p.job_execution_id = je.job_execution_id "
                        + "WHERE p.parameter_name = 'jobUuid' AND p.parameter_value = ? "
                        + "ORDER BY je.job_execution_id DESC LIMIT 1",
                String.class, uuid);
    }
}
