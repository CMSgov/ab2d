package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End to end regression test for the soft-resume
 * After a graceful shutdown, the worker should pause the job, and recovery should
 * be able to restart from the latest chunk checkpoint, no partition restarting.
 */
class PrototypeJobPauseResumeIntegrationTest extends AbstractPrototypeRecoveryIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(PrototypeJobPauseResumeIntegrationTest.class);

    @Test
    @DisplayName("A V3 job paused by a graceful shutdown resumes from its checkpoint and completes")
    void jobResumesAfterGracefulShutdown() throws Exception {
        Job job = createSubmittedV3Job("resume-it");
        String uuid = job.getJobUuid();

        // Step 1/2 the worker picks up a job, completes at least one partition
        log.info("=== worker starting up and picking up SUBMITTED job {} ({} benes across 3 partitions) ===",
                uuid, TOTAL_BENES);
        RunningWorker worker = startWorkerUntilOnePartitionDone(uuid, "test-prototype-worker");

        List<CompletedPartitionExecution> completedBeforeStop = completedPartitions(uuid);
        log.info("=== {} partition(s) COMPLETED (step_execution_ids={}), {} of {} benes processed so far"
                        + " - now shutting the worker down mid-job ===",
                completedBeforeStop.size(),
                completedBeforeStop.stream().map(CompletedPartitionExecution::stepExecutionId).toList(),
                processedLog.size(), TOTAL_BENES);

        // Step 3 gracefully shut down the worker
        prototypeJobProcessor.stopForShutdown();
        worker.awaitReturn(90);

        int processedInPhase1 = processedLog.size();

        // the job should be back to submitted status ready to be resumed
        assertEquals(JobStatus.SUBMITTED, jobRepository.findByJobUuid(uuid).getStatus(),
                "a gracefully stopped prototype job should be reset to SUBMITTED");
        assertTrue(processedInPhase1 > 0 && processedInPhase1 < TOTAL_BENES,
                "expected partial progress before the pause, saw " + processedInPhase1 + " of " + TOTAL_BENES);

        // Step 4 the worker restarts right where it left off
        log.info("=== worker restarting and resuming job {} ===", uuid);
        Job resumed = prototypeJobProcessor.process(uuid);

        // Step 5a check the job finished successfully
        assertEquals(JobStatus.SUCCESSFUL, resumed.getStatus(), "resumed job should complete successfully");
        log.info("=== job {} finished with status {}; {} total EOB calls for {} benes"
                        + " ===",
                uuid, resumed.getStatus(), processedLog.size(), TOTAL_BENES);

        // Step 5b partitions that were already completed do not get rerun
        List<CompletedPartitionExecution> completedAfter = completedPartitions(uuid);
        for (CompletedPartitionExecution before : completedBeforeStop) {
            CompletedPartitionExecution after = completedAfter.stream()
                    .filter(p -> p.stepExecutionId() == before.stepExecutionId())
                    .findFirst()
                    .orElse(null);
            assertNotNull(after, "previously completed partition " + before.stepExecutionId()
                    + " is missing after resume");
            assertEquals(before.startTime(), after.startTime(),
                    "partition " + before.stepExecutionId() + " start time changed, it was re-run instead of skipped");
        }

        // Step 5c no benes were skipped
        Set<Long> distinctProcessed = new HashSet<>(processedLog);
        assertTrue(distinctProcessed.containsAll(ALL_BENES),
                "every beneficiary should be processed at least once; missing=" + missing(ALL_BENES, distinctProcessed));

        // Step 5d we do not do any unnecessary work
        assertTrue(processedLog.size() <= TOTAL_BENES + CHUNK_SIZE,
                "resume reprocessed too much work (" + processedLog.size() + " calls for " + TOTAL_BENES
                        + " benes) - it looks like it restarted instead of resuming");

        // Step 5e output files were produced for each partition
        List<Path> outputs = finishedFiles(uuid);
        assertFalse(outputs.isEmpty(), "expected ndjson output files under the finished dir for job " + uuid);
    }
}
