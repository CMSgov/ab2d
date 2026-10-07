package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.coverage.model.CoverageSummary;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * Tests concurrent item processing. Checks that the work is actually being done in parallel, that
 * retry/failure/skipping is item-level so that one bad bene doesn't fail the whole chunk.
 */
@TestPropertySource(properties = {
        // we don't need a delay for this test
        "pause-resume.prototype.item-delay-ms=0"
})
class PrototypeConcurrentItemIntegrationTest extends AbstractPrototypeRecoveryIntegrationTest {

    private static final long TARGET_BENE = 6L;
    private static final long CHUNK_MATE = 5L;

    /** Every call made to "bfd". */
    private final Map<Long, AtomicInteger> attempts = new ConcurrentHashMap<>();

    private int attemptsFor(long bene) {
        AtomicInteger count = attempts.get(bene);
        return count == null ? 0 : count.get();
    }

    private void recordAttempt(long bene) {
        attempts.computeIfAbsent(bene, k -> new AtomicInteger()).incrementAndGet();
    }

    @Test
    @DisplayName("A chunk's beneficiaries are fetched in parallel")
    void chunkBenesAreProcessedConcurrently() throws Exception {
        Job job = createSubmittedV3Job("item-concurrent");
        String uuid = job.getJobUuid();

        // The mocked answer causes a processed bene to wait around until this latch opens
        // the latch opens when it counts down twice
        // If the benes are being processed in parallel:
        //  bene 1 finishes and counts down the latch
        //  bene 1 waits for the latch to open
        //  at the same time, bene 2 on a different thread finishes
        //  bene 2 counts down, the latch opens, both benes complete
        // If the benes are being processed in series:
        //  bene 1 finishes and counts down the latch
        //  bene 1 waits for the latch to open
        //  the latch doesn't open since bene 2 isn't being processed
        //  bene 1 times out
        //  bene 2 starts and the same thing happens, it times out
        //  the test knows that the benes are not being processed in parallel
        CountDownLatch bothInFlight = new CountDownLatch(2);
        AtomicBoolean timedOut = new AtomicBoolean(false);
        List<String> threadNames = new CopyOnWriteArrayList<>();

        doAnswer(inv -> {
            CoverageSummary patient = inv.getArgument(1);
            threadNames.add(Thread.currentThread().getName());
            bothInFlight.countDown();
            if (!bothInFlight.await(15, TimeUnit.SECONDS)) {
                timedOut.set(true);
            }
            return oneEobFor(patient);
        }).when(patientClaimsProcessor).getEobBundleResources(any(), any());

        Job result = prototypeJobProcessor.process(uuid);

        assertFalse(timedOut.get(),
                "the chunk was not processed in parallel, one or more benes timed out");
        assertEquals(JobStatus.SUCCESSFUL, result.getStatus());
        assertEveryBeneExactlyOnceInOutput(uuid);

        assertTrue(new HashSet<>(threadNames).size() > 1,
                "expected more than one worker thread to serve the partition, saw " + new HashSet<>(threadNames));
        assertTrue(threadNames.stream().allMatch(name -> name.startsWith("pcp-")),
                "bene work should run on the shared patient claims pool");
    }

    @Test
    @DisplayName("A transient failure retries only the beneficiary that failed, not its chunk")
    void transientFailureRetriesOnlyTheFailingBene() throws Exception {
        Job job = createSubmittedV3Job("item-retry");
        String uuid = job.getJobUuid();

        // bene 6 fails twice with a retryable exception, then succeeds within the retry limit of 3
        doAnswer(inv -> {
            CoverageSummary patient = inv.getArgument(1);
            long id = patientId(patient);
            recordAttempt(id);
            if (id == TARGET_BENE && attemptsFor(id) <= 2) {
                throw new SocketTimeoutException("transient failure " + attemptsFor(id) + " for bene " + id);
            }
            return oneEobFor(patient);
        }).when(patientClaimsProcessor).getEobBundleResources(any(), any());

        Job result = prototypeJobProcessor.process(uuid);

        assertEquals(JobStatus.SUCCESSFUL, result.getStatus(), "a bene that recovers within its retries is not an error");
        assertEquals(3, attemptsFor(TARGET_BENE), "bene " + TARGET_BENE + " should have been retried until it succeeded");
        assertEquals(1, attemptsFor(CHUNK_MATE),
                "bene " + CHUNK_MATE + " shares a chunk with " + TARGET_BENE + " and must not be re-fetched for it");
        assertEveryBeneExactlyOnceInOutput(uuid);
    }

    @Test
    @DisplayName("A skipped beneficiary still lets the rest of its chunk commit")
    void skippedBeneDoesNotCostItsChunkMates() throws Exception {
        Job job = createSubmittedV3Job("item-skip");
        String uuid = job.getJobUuid();

        doAnswer(inv -> {
            CoverageSummary patient = inv.getArgument(1);
            long id = patientId(patient);
            recordAttempt(id);
            if (id == TARGET_BENE) {
                throw new IllegalStateException("persistent failure for bene " + id);
            }
            return oneEobFor(patient);
        }).when(patientClaimsProcessor).getEobBundleResources(any(), any());

        Job result = prototypeJobProcessor.process(uuid);

        // one failure out of twenty is under the 10% threshold
        assertEquals(JobStatus.SUCCESSFUL, result.getStatus());
        assertEquals(1, attemptsFor(TARGET_BENE), "a non-transient failure should not be retried");
        assertEquals(1, attemptsFor(CHUNK_MATE), "the chunk-mate of a skipped bene should not be retried");

        Set<Long> delivered = new HashSet<>(benesIn(deliveredOutputFiles(uuid)));
        assertFalse(delivered.contains(TARGET_BENE), "the skipped bene should not be in the output");
        assertTrue(delivered.contains(CHUNK_MATE), "the chunk-mate of the skipped bene should still be output");
        assertEquals(TOTAL_BENES - 1, delivered.size(), "total benes delivered does not include skipped bene");
    }
}
