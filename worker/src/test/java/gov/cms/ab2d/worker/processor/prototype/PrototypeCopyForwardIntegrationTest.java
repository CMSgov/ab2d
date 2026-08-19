package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for the copy-forward feature. After a hard crash, we check
 * to see if we can recover each in-progress partition, and if we can, copy the contents
 * of the old file to the new one instead of restarting the partition.
 */
class PrototypeCopyForwardIntegrationTest extends AbstractPrototypeRecoveryIntegrationTest {

    private static final int CHUNKS_BEFORE_CRASH = 3;

    @Autowired
    private PrototypeProperties props;

    @AfterEach
    void restoreCopyForward() {
        props.setCopyForwardEnabled(true);
    }

    @Test
    @DisplayName("A hard-crashed partition resumes from its last chunk instead of being redone")
    void hardCrashResumesTheInFlightPartition() throws Exception {
        Job job = createSubmittedV3Job("copy-fwd");
        String uuid = job.getJobUuid();

        crashMidPartition(uuid, "test-copy-forward-owner", "STARTED");

        Job recovered = prototypeJobProcessor.process(uuid);

        assertEquals(JobStatus.SUCCESSFUL, recovered.getStatus(), "a hard-crashed job should recover to SUCCESSFUL");
        assertEveryBeneExactlyOnceInOutput(uuid);

        // When the job restarts after crashing mid-partition, we expect it to only restart chunks that
        // were in-progress. We do CHUNKS_BEFORE_CRASH number of chunks before crashing. If the recovery
        // restarts the partition from scratch, we'd expect to see CHUNKS_BEFORE_CRASH * CHUNK_SIZE duplicates
        // because we'd be redoing all the chunks. If it's restarting from the last good chunk, we expect to see
        // at most CHUNK_SIZE duplicates, since only one chunk will be in progress at a time.
        List<Long> refetched = duplicates(processedLog);
        assertTrue(refetched.size() <= CHUNK_SIZE,
                "copy-forward should only redo the chunks that were in-progress");
    }

    @Test
    @DisplayName("With copy-forward off, the same crash redoes everything the partition had committed")
    void disablingCopyForwardRedoesTheInFlightPartition() throws Exception {
        props.setCopyForwardEnabled(false);
        Job job = createSubmittedV3Job("no-copy-fwd");
        String uuid = job.getJobUuid();

        long committedBeforeCrash = crashMidPartition(uuid, "test-no-copy-forward-owner", "STARTED");

        Job recovered = prototypeJobProcessor.process(uuid);

        assertEquals(JobStatus.SUCCESSFUL, recovered.getStatus(), "the job should still recover to SUCCESSFUL");
        assertEveryBeneExactlyOnceInOutput(uuid);

        // this is the behaviour copy-forward replaces, and the contrast that gives the test above its meaning
        assertTrue(duplicates(processedLog).size() >= committedBeforeCrash,
                "the number of duplicates is too small, the partition did not restart entirely");
    }

    @Test
    @DisplayName("A partition left UNKNOWN is redone from scratch and the job still completes")
    void unknownPartitionIsRedone() throws Exception {
        Job job = createSubmittedV3Job("unknown-no-copy");
        String uuid = job.getJobUuid();

        // UNKNOWN is Spring Batch saying it cannot vouch for the output file, so the checkpoint is not trusted
        long committedBeforeCrash = crashMidPartition(uuid, "test-unknown-owner", "UNKNOWN");
        assertTrue(workerStepStatuses(uuid).contains("UNKNOWN"), "UNKNOWN status not set or healed prematurely");

        Job recovered = prototypeJobProcessor.process(uuid);

        assertEquals(JobStatus.SUCCESSFUL, recovered.getStatus(),
                "an UNKNOWN partition should be healed, redone, and the job should still finish");
        assertEveryBeneExactlyOnceInOutput(uuid);
        assertTrue(duplicates(processedLog).size() >= committedBeforeCrash,
                "an UNKNOWN partition must not be resumed");
    }

    /**
     * Run a job until one partition has finished and the next has committed several chunks, then abandon the
     * worker and leave the batch metadata looking like a crash.
     *
     * @return how many beneficiaries were processed, which is the number we expect copy-forward to save
     */
    private long crashMidPartition(String uuid, String threadName, String inflightStepStatus) throws Exception {
        RunningWorker owner = startWorkerUntilOnePartitionDone(uuid, threadName);
        waitUntil(() -> committedReadsInFlight(uuid) >= (long) CHUNKS_BEFORE_CRASH * CHUNK_SIZE
                || isTerminal(jobRepository.findByJobUuid(uuid)), 90);

        long committed = committedReadsInFlight(uuid);
        owner.killHard();
        forceHardCrashState(uuid, inflightStepStatus);

        assertTrue(committed >= (long) CHUNKS_BEFORE_CRASH * CHUNK_SIZE,
                "not enough chunks were committed while running the worker");
        assertTrue(processedLog.size() < TOTAL_BENES,
                "the job finished before it could be crashed");
        return committed;
    }

    /**
     * Beneficiaries committed by partitions that have not finished.
     */
    private long committedReadsInFlight(String uuid) {
        Long reads = jdbc.queryForObject(
                "SELECT COALESCE(SUM(se.read_count), 0) FROM batch_step_execution se "
                        + "JOIN batch_job_execution_params p ON p.job_execution_id = se.job_execution_id "
                        + "WHERE p.parameter_name = 'jobUuid' AND p.parameter_value = ? "
                        + "  AND se.step_name LIKE ? AND se.status <> 'COMPLETED'",
                Long.class, uuid, WORKER_STEP_LIKE);
        return reads == null ? 0L : reads;
    }
}
