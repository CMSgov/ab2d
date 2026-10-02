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
        prototypeJobProcessor.stopForShutdown(Set.of(uuid));
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

        // Step 5e output files are available, and has JobOutput rows
        List<Path> outputs = deliveredOutputFiles(uuid);
        assertFalse(outputs.isEmpty(), "expected .ndjson.gz output files in the job root for job " + uuid);
        assertTrue(jobOutputFilePaths(uuid).stream().allMatch(p -> p.endsWith(".ndjson.gz")),
                "every JobOutput should reference a compressed .ndjson.gz file: " + jobOutputFilePaths(uuid));
        assertTrue(finishedFiles(uuid).isEmpty() && streamingFiles(uuid).isEmpty(),
                "streaming/ and finished/ working directories should be cleaned up after success");
    }

    @Test
    @DisplayName("A cancelled job stops at the next chunk instead of finishing")
    void jobStopsShortWhenCancelledMidRun() throws Exception {
        Job job = createSubmittedV3Job("cancel-it");
        String uuid = job.getJobUuid();

        // start the worker and let it do one partition
        RunningWorker worker = startWorkerUntilOnePartitionDone(uuid, "test-cancel-worker");
        int processedBeforeCancel = new HashSet<>(processedLog).size();
        log.info("=== {} of {} benes processed, cancelling job {} mid-run ===",
                processedBeforeCancel, TOTAL_BENES, uuid);

        // directly change the job status to canceled
        assertEquals(1, jdbc.update("UPDATE job SET status = 'CANCELLED' WHERE job_uuid = ?", uuid),
                "expected exactly one job row to cancel for " + uuid);

        // wait until the worker notices the job's been canceled
        worker.awaitReturn(90);

        Set<Long> distinctProcessed = new HashSet<>(processedLog);
        assertEquals(JobStatus.CANCELLED, jobRepository.findByJobUuid(uuid).getStatus(),
                "a cancelled job should be left CANCELLED");

        // check to make sure the batch did didn't finish
        assertTrue(distinctProcessed.size() < TOTAL_BENES,
                "all " + TOTAL_BENES + " benes were processed, so the cancellation was never noticed mid-run");

        // check to make sure the job got cleaned up
        assertTrue(deliveredOutputFiles(uuid).isEmpty() && finishedFiles(uuid).isEmpty()
                        && streamingFiles(uuid).isEmpty(),
                "a cancelled job should leave no output or working files behind");
    }

    @Test
    @DisplayName("A shutdown leaves batch executions this worker does not own alone, stranded ones included")
    void shutdownOnlyStopsThisWorkersExecutions() throws Exception {
        Job job = createSubmittedV3Job("stranded-neighbour");
        String uuid = job.getJobUuid();

        // A worker killed mid-stop leaves its execution in STOPPING with no end time
        // spring batch considers that row as a "running" execution, basically forever
        long strandedId = strandPrototypeExecution("job-owned-by-another-worker");

        RunningWorker worker = startWorkerUntilOnePartitionDone(uuid, "test-stranded-neighbour-worker");

        long startedAt = System.currentTimeMillis();
        prototypeJobProcessor.stopForShutdown(Set.of(uuid));
        long drainMs = System.currentTimeMillis() - startedAt;
        worker.awaitReturn(90);

        assertEquals(JobStatus.SUBMITTED, jobRepository.findByJobUuid(uuid).getStatus(),
                "the job this worker owns should still suspend cleanly");
        assertTrue(drainMs < prototypeProperties.getShutdownAwaitMs(),
                "the drain waited out its full " + prototypeProperties.getShutdownAwaitMs() + "ms budget ("
                        + drainMs + "ms), so it never noticed its own job had stopped");
        assertEquals("STOPPING", jdbc.queryForObject(
                        "SELECT status FROM batch_job_execution WHERE job_execution_id = ?", String.class, strandedId),
                "another worker's stranded execution should be untouched");
    }

    /**
     * Write the batch metadata a worker leaves behind when it is killed between being told to stop and
     * finishing
     */
    private long strandPrototypeExecution(String foreignJobUuid) {
        long instanceId = jdbc.queryForObject("SELECT nextval('batch_job_instance_seq')", Long.class);
        long executionId = jdbc.queryForObject("SELECT nextval('batch_job_execution_seq')", Long.class);
        jdbc.update("INSERT INTO batch_job_instance (job_instance_id, version, job_name, job_key) "
                        + "VALUES (?, 0, ?, ?)",
                instanceId, PrototypeJobProcessorImpl.PROTOTYPE_JOB_NAME, "stranded-" + instanceId);
        jdbc.update("INSERT INTO batch_job_execution (job_execution_id, version, job_instance_id, create_time, "
                        + "start_time, end_time, status, exit_code, last_updated) "
                        + "VALUES (?, 1, ?, now(), now(), NULL, 'STOPPING', 'UNKNOWN', now())",
                executionId, instanceId);
        jdbc.update("INSERT INTO batch_job_execution_params (job_execution_id, parameter_name, parameter_type, "
                        + "parameter_value, identifying) VALUES (?, 'jobUuid', ?, ?, 'Y')",
                executionId, String.class.getName(), foreignJobUuid);
        return executionId;
    }
}
