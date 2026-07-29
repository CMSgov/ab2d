package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the cases where a worker that hasn't crashed loses the lock, but still thinks it has it.
 * The fencing token ensures that only one worker can work on a job at a time.
 *
 * The tests check to ensure that a worker that has stalled and lost the lease gets fenced out, and
 * doesn't affect the job anymore. That the zombie doesn't resubmit/retry the job and enter a loop, and
 * the new owner is able to resume from the completed partitions.
 *
 */
class PrototypeFenceGuardIntegrationTest extends AbstractPrototypeRecoveryIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(PrototypeFenceGuardIntegrationTest.class);

    @Test
    @DisplayName("A worker superseded mid-run is fenced cleanly, does not resubmit, and the peer resumes to completion")
    void supersededWorkerIsFencedCleanlyAndPeerResumes() throws Exception {
        Job job = createSubmittedV3Job("fence-it");
        String uuid = job.getJobUuid();

        // Original owner picks up the job under token 1 and processes until one partition has completed.
        log.info("=== STEP 1: original owner picking up job {} ({} benes across 3 partitions) ===", uuid, TOTAL_BENES);
        RunningWorker zombie = startWorkerUntilOnePartitionDone(uuid, "test-zombie-worker");
        long tokenN = currentToken(uuid);

        // to simulate a peer taking over, the token is bumped. The running worker's next attempt to commit
        // a chunk should get denied and throw.
        log.info("=== STEP 2: simulating peer takeover by bumping the lease token for {} (was {}) ===", uuid, tokenN);
        long tokenAfterBump = bumpLeaseOutOfBand(uuid, "peer-sim");
        assertEquals(tokenN + 1, tokenAfterBump, "peer takeover should advance the token by exactly one");

        // We give the zombie some time to realize it's been fenced and die
        Job zombieResult = zombie.run().get(90, java.util.concurrent.TimeUnit.SECONDS);
        zombie.pool().shutdownNow();

        // The dead worker should fail cleanly, with partitions failed and no unknown step executions
        List<String> statuses = workerStepStatuses(uuid);
        log.info("=== worker step statuses after fence: {} ===", statuses);
        assertTrue(statuses.contains("FAILED"), "the in-flight partition should have been fenced to FAILED, saw " + statuses);
        assertFalse(statuses.contains("UNKNOWN"),
                "fencing rolls back before commit, so no step should be UNKNOWN, saw " + statuses);
        assertTrue(fenceLostRecorded(uuid), "the failed step should carry a FenceLostException in its exit message");

        // the dying worker should not resubmit the job
        assertEquals(JobStatus.IN_PROGRESS, zombieResult.getStatus(),
                "a fenced-out worker must exit quietly without affecting the job");
        assertEquals(tokenAfterBump, currentToken(uuid),
                "a fenced-out worker must not bump the token again");

        // New owner resumes the job and bumps the token
        log.info("=== STEP 3: new owner resuming job {} ===", uuid);
        Job resumed = prototypeJobProcessor.process(uuid);
        assertEquals(JobStatus.SUCCESSFUL, resumed.getStatus(), "the new owner should resume the fenced job to completion");
        assertTrue(currentToken(uuid) > tokenAfterBump, "the resuming owner should have taken a fresh token");

        // No data loss and no duplicate output despite the fenced chunk.
        assertEveryBeneExactlyOnceInOutput(uuid);
        log.info("=== STEP 4: job {} completed; every beneficiary present exactly once in the output ===", uuid);
    }

    @Test
    @DisplayName("A false-positive TTL takeover")
    void falsePositiveTtlTakeoverIsStillSafe() throws Exception {
        Job job = createSubmittedV3Job("false-ttl");
        String uuid = job.getJobUuid();

        // Owner A is picked up and is genuinely healthy and making progress.
        RunningWorker owner = startWorkerUntilOnePartitionDone(uuid, "test-healthy-owner");
        long tokenN = currentToken(uuid);

        // Simulate a transient stall so that the job appears recoverable
        ageHeartbeat(uuid, 120);
        assertTrue(leaseStale(uuid, 60), "aged heartbeat should read as stale - the (false-positive) takeover trigger");

        // A peer acts on that false positive and takes over, even though A is fine. A is now blocked out.
        long tokenAfterBump = bumpLeaseOutOfBand(uuid, "peer-false-positive");
        assertEquals(tokenN + 1, tokenAfterBump, "the takeover advances the token by one");

        Job ownerResult = owner.run().get(90, java.util.concurrent.TimeUnit.SECONDS);
        owner.pool().shutdownNow();

        // The unnecessary takeover is harmless and can finish the job
        assertFalse(workerStepStatuses(uuid).contains("UNKNOWN"),
                "even an unnecessary takeover must not corrupt a step to UNKNOWN");
        assertEquals(JobStatus.IN_PROGRESS, ownerResult.getStatus(),
                "the old owner must exit quietly without resubmitting");

        Job resumed = prototypeJobProcessor.process(uuid);
        assertEquals(JobStatus.SUCCESSFUL, resumed.getStatus(), "the peer should complete the job");
        assertEveryBeneExactlyOnceInOutput(uuid);
    }
}
