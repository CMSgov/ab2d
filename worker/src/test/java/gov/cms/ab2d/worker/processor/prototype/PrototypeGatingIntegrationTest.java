package gov.cms.ab2d.worker.processor.prototype;

import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.common.service.FeatureEngagement;
import gov.cms.ab2d.coverage.service.v3.CoverageV3Service;
import gov.cms.ab2d.coverage.service.v3.CoverageV3SyncResult;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobStatus;
import gov.cms.ab2d.worker.config.JobHandler;
import gov.cms.ab2d.worker.config.JobMessageSource;
import gov.cms.ab2d.worker.processor.JobPreProcessor;
import gov.cms.ab2d.worker.processor.JobProcessor;
import gov.cms.ab2d.worker.service.ShutDownService;
import gov.cms.ab2d.worker.service.WorkerService;
import gov.cms.ab2d.worker.service.WorkerServiceImpl;
import gov.cms.ab2d.worker.stuckjob.CancelStuckJobsProcessorImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.messaging.support.GenericMessage;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static gov.cms.ab2d.common.util.PropertyConstants.PAUSE_RESUME_PROTOTYPE_ENABLED;
import static gov.cms.ab2d.fhir.FhirVersion.R4V3;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for making sure the prototype doesn't interfere with real job work
 */
class PrototypeGatingIntegrationTest extends AbstractPrototypeRecoveryIntegrationTest {

    private static final String PROTOTYPE_JOB = "33333333-prototype";

    @Autowired
    private JobProcessor normalJobProcessor;

    @Test
    @DisplayName("The prototype rejects a job that is not eligible")
    void prototypeRejectsJobWithoutTheFlag() {
        Job job = createSubmittedV3Job("not-eligible");
        String uuid = job.getJobUuid();
        job.setPauseEligible(false);
        jobRepository.saveAndFlush(job);

        Job rejected = prototypeJobProcessor.process(uuid);

        assertEquals(JobStatus.FAILED, rejected.getStatus(), "a job the prototype may not claim must not run");
        assertTrue(rejected.getStatusMessage().contains("not pause/resume eligible"),
                "the status message should say why, saw: " + rejected.getStatusMessage());
        assertEquals(0, jdbc.queryForObject(
                        "SELECT count(*) FROM ab2d.job_lease WHERE job_uuid = ?", Integer.class, uuid),
                "a rejected job should not have claimed a lease");
    }

    @Test
    @DisplayName("The normal pathway rejects an opted-in job instead of redoing its export")
    void normalPathwayRejectsAnOptedInJob() {
        Job job = createSubmittedV3Job("wrong-pathway");
        String uuid = job.getJobUuid();

        Job rejected = normalJobProcessor.process(uuid);

        assertEquals(JobStatus.FAILED, rejected.getStatus(),
                "an opted-in job must not run on the normal pathway");
        assertTrue(rejected.getStatusMessage().contains("pause/resume eligible"),
                "the status message should say why, saw: " + rejected.getStatusMessage());
        assertTrue(deliveredOutputFiles(uuid).isEmpty() && streamingFiles(uuid).isEmpty(),
                "a rejected job should not have written any output");
    }


    @Test
    @DisplayName("A job asked to yield pauses without blocking the caller, then resumes from its checkpoint")
    void yieldedJobPausesAndResumes() throws Exception {
        Job job = createSubmittedV3Job("yield-resume");
        String uuid = job.getJobUuid();

        RunningWorker worker = startWorkerUntilOnePartitionDone(uuid, "test-yield-worker");
        List<CompletedPartitionExecution> completedBeforeYield = completedPartitions(uuid);

        // real work has arrived, so the prototype is asked to step aside
        long before = System.currentTimeMillis();
        prototypeJobProcessor.stopRunning();
        long elapsed = System.currentTimeMillis() - before;
        assertTrue(elapsed < 2000, "stopRunning should only signal the stop, but it took " + elapsed + "ms");

        worker.awaitReturn(90);

        int processedBeforeResume = processedLog.size();
        assertEquals(JobStatus.SUBMITTED, jobRepository.findByJobUuid(uuid).getStatus(),
                "a yielded job should be left SUBMITTED for a later pickup");
        assertTrue(processedBeforeResume > 0 && processedBeforeResume < TOTAL_BENES,
                "expected partial progress before the yield, saw " + processedBeforeResume + " of " + TOTAL_BENES);

        // the worker is quiet again, so the job is admissible and gets picked back up
        Job resumed = prototypeJobProcessor.process(uuid);

        assertEquals(JobStatus.SUCCESSFUL, resumed.getStatus(), "a yielded job should resume and finish");
        for (CompletedPartitionExecution beforePartition : completedBeforeYield) {
            CompletedPartitionExecution after = completedPartitions(uuid).stream()
                    .filter(partition -> partition.stepExecutionId() == beforePartition.stepExecutionId())
                    .findFirst()
                    .orElse(null);
            assertNotNull(after, "partition " + beforePartition.stepExecutionId() + " is missing after the resume");
            assertEquals(beforePartition.startTime(), after.startTime(),
                    "partition " + beforePartition.stepExecutionId() + " was re-run instead of skipped");
        }
        assertTrue(new HashSet<>(processedLog).containsAll(ALL_BENES),
                "every beneficiary should be processed at least once; missing="
                        + missing(ALL_BENES, new HashSet<>(processedLog)));
        assertTrue(processedLog.size() <= TOTAL_BENES + CHUNK_SIZE,
                "the resume redid too much work (" + processedLog.size() + " calls for " + TOTAL_BENES
                        + " benes), so it restarted rather than resumed");
    }


    @Test
    @DisplayName("A pause request holds the job until it is released, then it resumes from its checkpoint")
    void pauseRequestHoldsTheJobUntilReleased() throws Exception {
        Job job = createSubmittedV3Job("pause-request");
        String uuid = job.getJobUuid();

        RunningWorker worker = startWorkerUntilOnePartitionDone(uuid, "test-pause-request-worker");

        assertEquals(1, jdbc.update(
                "UPDATE ab2d.job_lease SET pause_requested = true WHERE job_uuid = ?", uuid),
                "expected exactly one lease row to mark for " + uuid);

        worker.awaitReturn(90);

        assertEquals(JobStatus.SUBMITTED, jobRepository.findByJobUuid(uuid).getStatus(),
                "a job asked to pause should be left SUBMITTED, not cancelled or failed");
        assertTrue(processedLog.size() < TOTAL_BENES,
                "the pause should have stopped the job mid-stream, saw " + processedLog.size()
                        + " of " + TOTAL_BENES);

        // directly paused jobs are not polled
        assertTrue(jobLease.isPauseRequested(uuid), "the request should stand until it is released");
        assertFalse(polledJobUuids().contains(uuid),
                "a held job must not be polled");

        // releasing makes it eligible again
        jobLease.clearPauseRequest(uuid);
        assertTrue(polledJobUuids().contains(uuid), "a released job should be polled again");

        Job resumed = prototypeJobProcessor.process(uuid);

        assertEquals(JobStatus.SUCCESSFUL, resumed.getStatus(), "a released job should resume and finish");
        assertTrue(new HashSet<>(processedLog).containsAll(ALL_BENES),
                "every beneficiary should still be processed; missing="
                        + missing(ALL_BENES, new HashSet<>(processedLog)));
    }

    /** Job uuids the real poll query currently selects. */
    private Set<String> polledJobUuids() {
        return jdbc.queryForList(JobMessageSource.buildQuery(60)).stream()
                .map(row -> String.valueOf(row.get("job_uuid")))
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("A worker admits a prototype job up to its real-job tolerance, and asks one to pause past it")
    void admissionAndYieldFollowTheRealJobCount() {
        Harness harness = new Harness();

        assertTrue(harness.workerService.isPrototypeAdmissible(), "an idle worker has room");

        harness.withRealJobsRunning(1, () -> assertTrue(harness.workerService.isPrototypeAdmissible(),
                "the default tolerance of 1 leaves room next to one real job"));
        verify(harness.prototypeJobProcessor, never()).stopRunning();

        harness.withRealJobsRunning(2, () -> assertFalse(harness.workerService.isPrototypeAdmissible(),
                "two real jobs is over the default tolerance"));
        verify(harness.prototypeJobProcessor).stopRunning();

        assertTrue(harness.workerService.isPrototypeAdmissible(),
                "the worker should reopen once the real work has finished");
    }

    @Test
    @DisplayName("A running prototype job does not count as real work, so it never displaces itself")
    void prototypeJobIsNotRealWork() {
        Harness harness = new Harness();
        harness.givenPrototypeJob(PROTOTYPE_JOB);

        harness.workerService.process(PROTOTYPE_JOB);

        verify(harness.prototypeJobProcessor, never()).stopRunning();
    }

    @Test
    @DisplayName("Nothing is held back when the feature flag or pause-under-load is off")
    void yieldingCanBeTurnedOff() {
        Harness flagOff = new Harness();
        when(flagOff.propertiesService.isToggleOn(PAUSE_RESUME_PROTOTYPE_ENABLED, false)).thenReturn(false);
        flagOff.withRealJobsRunning(2, () -> assertTrue(flagOff.workerService.isPrototypeAdmissible(),
                "with the feature off nothing is routed to the prototype, so nothing is held back"));
        verify(flagOff.prototypeJobProcessor, never()).stopRunning();

        Harness pauseOff = new Harness();
        pauseOff.props.setPauseUnderLoad(false);
        pauseOff.withRealJobsRunning(2, () -> assertTrue(pauseOff.workerService.isPrototypeAdmissible(),
                "with pause-under-load off the prototype keeps running under any load"));
        verify(pauseOff.prototypeJobProcessor, never()).stopRunning();
    }


    @Test
    @DisplayName("The handler leaves a prototype job alone while the worker is busy, and starts it when quiet")
    void handlerSkipsPrototypeJobsWhileBusy() {
        Job started = new Job();
        started.setStatus(JobStatus.IN_PROGRESS);

        WorkerService busy = mock(WorkerService.class);
        when(busy.getEngagement()).thenReturn(FeatureEngagement.IN_GEAR);
        when(busy.isPrototypeAdmissible()).thenReturn(false);
        when(busy.process(anyString())).thenReturn(started);

        LockRegistry lockRegistry = mock(LockRegistry.class);
        when(lockRegistry.obtain(anyString())).thenReturn(new ReentrantLock());
        CoverageV3Service coverage = mock(CoverageV3Service.class);
        when(coverage.moveFromStagingToRecentCoverage(anyString(), any()))
                .thenReturn(CoverageV3SyncResult.SYNC_SUCCESSFUL_FOR_CONTRACT);

        // a prototype row behind real work: the prototype row is passed over, the real one still runs
        new JobHandler(lockRegistry, busy, coverage).handleMessage(new GenericMessage<>(List.of(
                pollRow(PROTOTYPE_JOB, "R4V3", true), pollRow("a-real-job", "STU3", false))));

        verify(busy, never()).process(PROTOTYPE_JOB);
        verify(busy).process("a-real-job");
        verify(lockRegistry, never()).obtain(PROTOTYPE_JOB);

        WorkerService quiet = mock(WorkerService.class);
        when(quiet.getEngagement()).thenReturn(FeatureEngagement.IN_GEAR);
        when(quiet.isPrototypeAdmissible()).thenReturn(true);
        when(quiet.process(anyString())).thenReturn(started);

        new JobHandler(lockRegistry, quiet, coverage)
                .handleMessage(new GenericMessage<>(List.of(pollRow(PROTOTYPE_JOB, "R4V3", true))));

        verify(quiet).process(PROTOTYPE_JOB);
    }

    @Test
    @DisplayName("Only an opted-in job gets the paused-time exemption from the stuck-job cancel")
    void onlyOptedInJobsAreExemptFromTheStuckCancel() {
        assertEquals(JobStatus.IN_PROGRESS, stuckCancelOutcome(true),
                "an opted-in job that has barely run is probably paused, so it is left alone");
        assertEquals(JobStatus.CANCELLED, stuckCancelOutcome(false),
                "a v3 job that never opted in is cancelled on wall clock as it always was");
    }

    /** Run the stuck-job sweep over one long-created v3 job that has barely any active runtime. */
    private JobStatus stuckCancelOutcome(boolean pauseEligible) {
        int thresholdHours = 48;
        gov.cms.ab2d.job.repository.JobRepository jobs =
                mock(gov.cms.ab2d.job.repository.JobRepository.class);
        PrototypeBatchMetadataRepository batchMeta = mock(PrototypeBatchMetadataRepository.class);
        PropertiesService properties = mock(PropertiesService.class);
        when(properties.isToggleOn(PAUSE_RESUME_PROTOTYPE_ENABLED, false)).thenReturn(true);
        // well under the threshold
        when(batchMeta.activeRuntimeSeconds(anyString())).thenReturn(60L);

        Job stuck = inProgressJob("stuck-" + pauseEligible, R4V3, pauseEligible);
        stuck.setCreatedAt(OffsetDateTime.now().minusHours(thresholdHours + 1L));
        when(jobs.findStuckJobs(any(OffsetDateTime.class))).thenReturn(List.of(stuck));

        new CancelStuckJobsProcessorImpl(jobs, mock(gov.cms.ab2d.eventclient.clients.SQSEventClient.class),
                thresholdHours, mock(CoverageV3Service.class), batchMeta, properties).process();

        return stuck.getStatus();
    }

    // fixtures

    private static Map<String, Object> pollRow(String jobUuid, String fhirVersion, boolean pauseEligible) {
        Map<String, Object> row = new HashMap<>();
        row.put("job_uuid", jobUuid);
        row.put("contract_number", CONTRACT);
        row.put("fhir_version", fhirVersion);
        row.put("pause_eligible", pauseEligible);
        return row;
    }

    private static Job inProgressJob(String jobUuid, gov.cms.ab2d.fhir.FhirVersion version, boolean pauseEligible) {
        Job job = new Job();
        job.setJobUuid(jobUuid);
        job.setStatus(JobStatus.IN_PROGRESS);
        job.setFhirVersion(version);
        job.setContractNumber(CONTRACT);
        job.setPauseEligible(pauseEligible);
        return job;
    }

    /**
     * a harness with all the mocked services we need to test
     */
    private static final class Harness {

        private final JobPreProcessor jobPreprocessor = mock(JobPreProcessor.class);
        private final JobProcessor jobProcessor = mock(JobProcessor.class);
        private final PropertiesService propertiesService = mock(PropertiesService.class);
        private final CoverageV3Service coverageV3Service = mock(CoverageV3Service.class);
        private final PrototypeJobProcessor prototypeJobProcessor = mock(PrototypeJobProcessor.class);
        private final PrototypeProperties props = new PrototypeProperties();
        private final WorkerServiceImpl workerService;

        private Harness() {
            when(propertiesService.isToggleOn(PAUSE_RESUME_PROTOTYPE_ENABLED, false)).thenReturn(true);
            workerService = new WorkerServiceImpl(jobPreprocessor, jobProcessor, mock(ShutDownService.class),
                    propertiesService, coverageV3Service, prototypeJobProcessor, props);
        }

        private void givenPrototypeJob(String jobUuid) {
            Job job = inProgressJob(jobUuid, R4V3, true);
            when(jobPreprocessor.preprocess(jobUuid)).thenReturn(job);
            when(prototypeJobProcessor.process(jobUuid)).thenReturn(job);
        }

        /** Run {@code count} real jobs that are all still in flight when {@code whileRunning} is invoked. */
        private void withRealJobsRunning(int count, Runnable whileRunning) {
            List<String> uuids = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                uuids.add("real-" + i);
            }
            for (int i = 0; i < count; i++) {
                final int next = i + 1;
                String uuid = uuids.get(i);
                Job job = inProgressJob(uuid, STU3, false);
                when(jobPreprocessor.preprocess(uuid)).thenReturn(job);
                when(jobProcessor.process(uuid)).thenAnswer(invocation -> {
                    if (next < count) {
                        workerService.process(uuids.get(next));
                    } else {
                        whileRunning.run();
                    }
                    return job;
                });
            }
            workerService.process(uuids.get(0));
        }
    }
}
