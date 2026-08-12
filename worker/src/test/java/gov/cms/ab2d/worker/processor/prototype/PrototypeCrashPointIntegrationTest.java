package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.coverage.model.CoverageSummary;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * Crash recovery integration tests that force failure at each distinct step of the pipeline and then
 * recovers it. The steps are:
 *      partitioning - fail mid-partition, on recovery the partitioning step should restart entirely
 *      processing   - fail while running the job processor, chunk rolls back, restarts from the prior chunk
 *      file writing - fail while writing a file. Recovery ensures no duplicate or corrupted output
 *      assembly     - fail while assembling the finished files. Recovery continues assembly idempotently
 *
 */
class PrototypeCrashPointIntegrationTest extends AbstractPrototypeRecoveryIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(PrototypeCrashPointIntegrationTest.class);
    private static final Pattern PATIENT_REF = Pattern.compile("Patient/(\\d+)");

    @Test
    @DisplayName("Crash during partitioning: the job is resubmitted and a later pickup restarts it")
    void crashDuringPartitioningRecovers() throws Exception {
        Job job = createSubmittedV3Job("crash-partition");
        String uuid = job.getJobUuid();

        // Fail the first attempt to build the partitions
        AtomicBoolean thrown = new AtomicBoolean(false);
        when(coverageV3Service.getPartitionBoundaryPatientIds(eq(CONTRACT), anyInt())).thenAnswer(inv -> {
            if (thrown.compareAndSet(false, true)) {
                log.info("=== injecting partitioning failure for {} ===", uuid);
                throw new IllegalStateException("injected partitioning failure");
            }
            return BOUNDARIES;
        });

        Job result = processUntilSuccessful(uuid, 3);

        assertEquals(JobStatus.SUCCESSFUL, result.getStatus(), "a partitioning crash should recover on a later pickup");
        assertTrue(thrown.get(), "the partitioning failure should actually have been injected");
        assertEveryBeneExactlyOnceInOutput(uuid);
    }

    @Test
    @DisplayName("Item steps should be retried after failing and every bene is still delivered once")
    void crashDuringProcessingRecovers() throws Exception {
        Job job = createSubmittedV3Job("crash-processing");
        String uuid = job.getJobUuid();

        // Inject an exception that crashes the bene processing, so we can restart it
        // The thrown exception won't/shouldn't trigger step-retry, so it just fails
        long failBene = 6L;
        AtomicBoolean thrown = new AtomicBoolean(false);
        doAnswer(inv -> {
            CoverageSummary patient = inv.getArgument(1);
            if (patientId(patient) == failBene && thrown.compareAndSet(false, true)) {
                log.info("=== injecting transient processing failure for bene {} on job {} ===", failBene, uuid);
                throw new org.springframework.web.client.ResourceAccessException(
                        "injected transient failure for bene " + failBene);
            }
            return oneEobFor(patient);
        }).when(patientClaimsProcessor).getEobBundleResources(any(), any());

        Job result = processUntilSuccessful(uuid, 3);

        assertEquals(JobStatus.SUCCESSFUL, result.getStatus(), "a transient processing blip should recover");
        assertTrue(thrown.get(), "the processing failure should actually have been injected");
        // The rolled-back chunk must not have committed output, so despite the failed attempt the delivered
        // output is still exactly-once.
        assertEveryBeneExactlyOnceInOutput(uuid);
    }

    @Test
    @DisplayName("Crash during file writing: a hard kill mid-write recovers with no torn or duplicated output")
    void crashDuringFileWritingRecoversWithCleanOutput() throws Exception {
        Job job = createSubmittedV3Job("crash-writing");
        String uuid = job.getJobUuid();

        // the work is committed after it's been synced. When hard recovering, the token bumps and
        // a new file will be created, so there's no risk of bleeding over from a dead worker.
        // TODO: Copy-forward can be used on partitions which have a not-UNKNOWN status to avoid redoing some work
        // When the worker crashes, the restart will get a new lock, new token, new file, and redo the partition
        RunningWorker owner = startWorkerUntilOnePartitionDone(uuid, "test-writing-crash");
        owner.killHard();
        forceHardCrashState(uuid, "STARTED");

        Job recovered = prototypeJobProcessor.process(uuid);

        assertEquals(JobStatus.SUCCESSFUL, recovered.getStatus(), "a mid-write crash should hard-recover to SUCCESSFUL");
        for (Path file : deliveredOutputFiles(uuid)) {
            List<Long> inFile = new ArrayList<>();
            for (String line : linesOf(file)) {
                Matcher m = PATIENT_REF.matcher(line);
                assertTrue(m.find(), "output file " + file.getFileName() + " has a line with no Patient reference "
                        + "(torn write?): " + line);
                inFile.add(Long.parseLong(m.group(1)));
            }
            assertEquals(inFile.size(), new HashSet<>(inFile).size(),
                    "component file " + file.getFileName() + " has a duplicated beneficiary (doubled write): "
                            + duplicates(inFile));
        }
        assertEveryBeneExactlyOnceInOutput(uuid);
    }

    @Test
    @DisplayName("Crash during assembly: a re-assembly on recovery is idempotent and registers no duplicate JobOutput rows")
    void reassemblyAfterCrashIsIdempotent() throws Exception {
        Job job = createSubmittedV3Job("crash-assembly");
        String uuid = job.getJobUuid();

        // First, run the job all the way to a successful, assembled result.
        Job completed = prototypeJobProcessor.process(uuid);
        assertEquals(JobStatus.SUCCESSFUL, completed.getStatus(), "baseline run should complete");
        int baselineRows = jobOutputRowCount(uuid);
        List<String> baselinePaths = jobOutputFilePaths(uuid);
        assertTrue(baselineRows >= 1, "the completed job should have at least one JobOutput row");

        // Simulate a crash after the job is done, but before it changes its status
        // A hard-recovery will complete the job and run assembly.
        // Assembly is idempotent and should not create duplicate files or duplicate JobOutput
        jdbc.update("UPDATE job SET status = 'IN_PROGRESS' WHERE job_uuid = ?", uuid);
        jdbc.update("UPDATE ab2d.job_lease SET clean_suspend_token = NULL WHERE job_uuid = ?", uuid);

        Job reassembled = prototypeJobProcessor.process(uuid);

        assertEquals(JobStatus.SUCCESSFUL, reassembled.getStatus(), "re-running a COMPLETED job should stay SUCCESSFUL");
        assertEquals(baselineRows, jobOutputRowCount(uuid),
                "re-assembly must not add duplicate JobOutput rows (it added "
                        + (jobOutputRowCount(uuid) - baselineRows) + ")");
        assertEquals(baselinePaths, jobOutputFilePaths(uuid),
                "re-assembly must register the same manifest file set, not duplicates");
        Set<String> distinctPaths = new HashSet<>(jobOutputFilePaths(uuid));
        assertEquals(distinctPaths.size(), jobOutputFilePaths(uuid).size(),
                "no JobOutput file_path should be registered twice");
        assertEveryBeneExactlyOnceInOutput(uuid);
    }

    /** Drive {@code process()} up to {@code maxAttempts} times, stopping as soon as the job is SUCCESSFUL. */
    private Job processUntilSuccessful(String uuid, int maxAttempts) {
        Job result = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            result = prototypeJobProcessor.process(uuid);
            log.info("=== pickup attempt {} for {} ended {} ===", attempt, uuid, result.getStatus());
            if (result.getStatus() == JobStatus.SUCCESSFUL) {
                return result;
            }
            assertFalse(result.getStatus() == JobStatus.FAILED && attempt == maxAttempts,
                    "job FAILED terminally within " + maxAttempts + " attempts");
        }
        return result;
    }
}
