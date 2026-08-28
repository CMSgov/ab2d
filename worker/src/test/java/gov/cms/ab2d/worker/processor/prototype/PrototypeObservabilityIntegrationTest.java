package gov.cms.ab2d.worker.processor.prototype;

import com.timgroup.statsd.StatsDClient;
import gov.cms.ab2d.coverage.model.CoverageSummary;
import gov.cms.ab2d.eventclient.events.JobStatusChangeEvent;
import gov.cms.ab2d.eventclient.events.LoggableEvent;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobStatus;
import gov.cms.ab2d.worker.processor.prototype.lease.JobLeaseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.Invocation;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;

/**
 * Observability coverage for the prototype: every terminal failure path must reach Slack, and a resume must
 * be countable in Datadog with soft and hard recoveries kept apart.
 *
 * {@code max-failure-attempts=1} makes the first batch failure terminal, which is what lets the
 * attempts-exhausted path be reached without failing the same job eight times.
 */
@TestPropertySource(properties = {
        "pause-resume.prototype.max-failure-attempts=1",
        "pause-resume.prototype.failure-attempts-warn-remaining=2"
})
class PrototypeObservabilityIntegrationTest extends AbstractPrototypeRecoveryIntegrationTest {

    @MockitoBean
    private StatsDClient statsDClient;

    @Test
    @DisplayName("Failure path 1 of 3: tripping the failure threshold alerts to Slack and counts its reason")
    void thresholdFailureAlerts() throws Exception {
        Job job = createSubmittedV3Job("obs-threshold");
        String uuid = job.getJobUuid();
        Set<Long> failBenes = Set.of(6L, 14L);

        // Two failures out of 20 reaches the 10% threshold, so the job fails terminally.
        doAnswer(inv -> {
            CoverageSummary patient = inv.getArgument(1);
            if (failBenes.contains(patientId(patient))) {
                throw new IllegalStateException("persistent failure for bene " + patientId(patient));
            }
            return oneEobFor(patient);
        }).when(patientClaimsProcessor).getEobBundleResources(any(), any());

        Job result = prototypeJobProcessor.process(uuid);

        assertEquals(JobStatus.FAILED, result.getStatus());
        assertTrue(failureAlertMessage(uuid).contains("threshold_exceeded"),
                "the Slack alert should name the failure path: " + failureAlertMessage(uuid));
        assertTrue(tagsOf("increment", PrototypeMetrics.JOB_FAILED).stream()
                        .anyMatch(tags -> tags.contains("reason:threshold_exceeded")),
                "the terminal failure should count under its own reason tag");
        // The skipped beneficiaries are what tripped the threshold, so they must be visible too.
        assertTrue(tagsOf("increment", PrototypeMetrics.BENE_SKIPPED).size() >= failBenes.size(),
                "every skipped beneficiary should be counted, saw "
                        + tagsOf("increment", PrototypeMetrics.BENE_SKIPPED).size());
        assertTrue(tagsOf("increment", PrototypeMetrics.BENE_SKIPPED).stream()
                        .allMatch(tags -> tags.contains("phase:process")),
                "these beneficiaries failed while their claims were being fetched");
    }

    @Test
    @DisplayName("Failure path 2 of 3: running out of resume attempts alerts to Slack and counts its reason")
    void attemptsExhaustedFailureAlerts() throws Exception {
        Job job = createSubmittedV3Job("obs-attempts");
        String uuid = job.getJobUuid();

        // Fail inside the partitioner. The batch execution ends FAILED without a threshold breach, which is
        // the resumable path - and with max-failure-attempts=1 the first failure exhausts the budget.
        doThrow(new IllegalStateException("partitioning blew up"))
                .when(coverageV3Service).getPartitionBoundaryPatientIds(eq(CONTRACT), anyInt());

        Job result = prototypeJobProcessor.process(uuid);

        assertEquals(JobStatus.FAILED, result.getStatus(),
                "with no attempts left the job must fail terminally rather than resubmit");
        assertTrue(failureAlertMessage(uuid).contains("attempts_exhausted"),
                "the Slack alert should name the failure path: " + failureAlertMessage(uuid));
        assertTrue(tagsOf("increment", PrototypeMetrics.JOB_FAILED).stream()
                        .anyMatch(tags -> tags.contains("reason:attempts_exhausted")),
                "the terminal failure should count under its own reason tag");
    }

    @Test
    @DisplayName("Failure path 3 of 3: a launch failure alerts to Slack and counts its reason")
    void launchFailureAlerts() throws Exception {
        Job job = createSubmittedV3Job("obs-launch");
        String uuid = job.getJobUuid();

        // Blow up before the batch job is ever launched, which is the outer catch in the processor.
        doThrow(new IllegalStateException("cannot build the aggregated table"))
                .when(coverageV3Service).createAggregatedAttributionTable(CONTRACT);

        Job result = prototypeJobProcessor.process(uuid);

        assertEquals(JobStatus.FAILED, result.getStatus());
        assertTrue(failureAlertMessage(uuid).contains("launch_failed"),
                "the Slack alert should name the failure path: " + failureAlertMessage(uuid));
        assertTrue(tagsOf("increment", PrototypeMetrics.JOB_FAILED).stream()
                        .anyMatch(tags -> tags.contains("reason:launch_failed")),
                "the terminal failure should count under its own reason tag");
    }

    @Test
    @DisplayName("A clean pause and its pickup are counted as a soft recovery, not a hard one")
    void softResumeIsCountedSeparately() throws Exception {
        Job job = createSubmittedV3Job("obs-soft");
        String uuid = job.getJobUuid();

        RunningWorker worker = startWorkerUntilOnePartitionDone(uuid, "test-obs-soft-worker");
        prototypeJobProcessor.stopForShutdown();
        worker.awaitReturn(90);
        assertEquals(JobStatus.SUBMITTED, jobRepository.findByJobUuid(uuid).getStatus());

        // The pause itself is observable, and it recorded that the clean-suspend marker was written.
        assertTrue(tagsOf("increment", PrototypeMetrics.JOB_SUSPENDED).stream()
                        .anyMatch(tags -> tags.contains("clean:true")),
                "a graceful shutdown should record a clean suspend");
        assertFalse(tagsOf("increment", PrototypeMetrics.DRAIN_STARTED).isEmpty(),
                "the shutdown drain should be observable");
        assertTrue(tagsOf("increment", PrototypeMetrics.DRAIN_FINISHED).stream()
                        .anyMatch(tags -> tags.contains("drained:true")),
                "the drain finished before the timeout, which is what makes the soft resume possible");

        Job resumed = prototypeJobProcessor.process(uuid);

        assertEquals(JobStatus.SUCCESSFUL, resumed.getStatus());
        assertTrue(tagsOf("increment", PrototypeMetrics.JOB_STARTED).stream()
                        .anyMatch(tags -> tags.contains("mode:soft")),
                "picking a cleanly paused job back up is a soft recovery");
        assertFalse(tagsOf("increment", PrototypeMetrics.JOB_STARTED).stream()
                        .anyMatch(tags -> tags.contains("mode:hard")),
                "a clean pause must not be reported as a crash recovery");
    }

    @Test
    @DisplayName("A crashed job's takeover is counted as a hard recovery, with what copy-forward salvaged")
    void hardRecoveryIsCountedSeparately() throws Exception {
        Job job = createSubmittedV3Job("obs-hard");
        String uuid = job.getJobUuid();

        RunningWorker owner = startWorkerUntilOnePartitionDone(uuid, "test-obs-hard-worker");
        owner.killHard();
        forceHardCrashState(uuid, "STARTED");

        Job recovered = prototypeJobProcessor.process(uuid);

        assertEquals(JobStatus.SUCCESSFUL, recovered.getStatus());
        assertTrue(tagsOf("increment", PrototypeMetrics.JOB_STARTED).stream()
                        .anyMatch(tags -> tags.contains("mode:fresh")),
                "the original run should have been recorded as a fresh start");
        assertTrue(tagsOf("increment", PrototypeMetrics.JOB_STARTED).stream()
                        .anyMatch(tags -> tags.contains("mode:hard")),
                "taking over a crashed job is a hard recovery");
        assertFalse(tagsOf("increment", PrototypeMetrics.JOB_STARTED).stream()
                        .anyMatch(tags -> tags.contains("mode:soft")),
                "a crash must not be reported as a clean pause");
        // Copy-forward is what decides how much of the crashed partition survived, so it is reported either
        // way: as carried chunks or as a partition that had to start over.
        assertFalse(counted("count", PrototypeMetrics.COPY_FORWARD_SEEDED).isEmpty()
                        && counted("count", PrototypeMetrics.COPY_FORWARD_RESTARTED).isEmpty(),
                "a hard recovery should report what copy-forward did with the in-flight partition");
    }

    @Test
    @DisplayName("A successful job neither alerts a failure nor counts one")
    void successDoesNotAlertFailure() throws Exception {
        Job job = createSubmittedV3Job("obs-success");
        String uuid = job.getJobUuid();

        Job result = prototypeJobProcessor.process(uuid);

        assertEquals(JobStatus.SUCCESSFUL, result.getStatus());
        assertTrue(tagsOf("increment", PrototypeMetrics.JOB_FAILED).isEmpty(),
                "a successful job must not report a failure");
        assertTrue(failureAlertMessages(uuid).isEmpty(),
                "a successful job must not raise a failure alert: " + failureAlertMessages(uuid));
        assertFalse(tagsOf("increment", PrototypeMetrics.JOB_COMPLETED).isEmpty(),
                "a successful job should be countable");
    }

    @Test
    @DisplayName("An IN_PROGRESS job counts as stale once its heartbeat passes the TTL, then as unrecovered")
    void deadHeartbeatsAreCountedStaleThenUnrecovered() {
        Job job = createSubmittedV3Job("obs-lease");
        String uuid = job.getJobUuid();
        jdbc.update("UPDATE job SET status = 'IN_PROGRESS' WHERE job_uuid = ?", uuid);

        int ttl = 40;
        int grace = ttl * 3;
        JobLeaseRepository.HeartbeatHealth before = jobLease.heartbeatHealth(ttl, grace);

        // A live owner: the heartbeat is fresh, so the job is healthy and nothing should take it over.
        jobLease.bump(uuid, "test-lease-owner");
        JobLeaseRepository.HeartbeatHealth live = jobLease.heartbeatHealth(ttl, grace);
        assertEquals(before.active() + 1, live.active(), "a job with a fresh heartbeat should count as active");
        assertEquals(before.stale(), live.stale(), "a live owner is not a takeover candidate");

        // The owner dies. Past the TTL the job is a takeover candidate, which is expected and self-healing.
        ageHeartbeat(uuid, ttl + 20);
        JobLeaseRepository.HeartbeatHealth stale = jobLease.heartbeatHealth(ttl, grace);
        assertEquals(before.stale() + 1, stale.stale(), "a dead heartbeat past the TTL should count as stale");
        assertEquals(before.unrecovered(), stale.unrecovered(),
                "just past the TTL a takeover has not had time to happen yet");

        // Still nobody picked it up well past the grace window, which means recovery is broken.
        ageHeartbeat(uuid, grace + 60);
        JobLeaseRepository.HeartbeatHealth stranded = jobLease.heartbeatHealth(ttl, grace);
        assertEquals(before.unrecovered() + 1, stranded.unrecovered(),
                "a job nobody recovered past the grace window is stranded and must be visible");
        assertEquals(before.stale(), stranded.stale(),
                "the buckets are mutually exclusive - a stranded job must not also be counted as stale");
        assertTrue(jobLease.claimUnrecoveredForAlert(grace, 3600).contains(uuid),
                "the stranded job should be nameable, not just countable");
        assertFalse(jobLease.claimUnrecoveredForAlert(grace, 3600).contains(uuid),
                "a second claim inside the cooldown must return nothing, so workers cannot flood Slack");
    }

    /**
     * The message of the FAILED status-change event sent to Slack for the given job. Fails the test if the
     * processor did not alert at all, which is the condition each failure-path test is really asserting.
     */
    private String failureAlertMessage(String jobUuid) {
        return failureAlertMessages(jobUuid).stream().findFirst()
                .orElseThrow(() -> new AssertionError("no FAILED Slack alert was sent for job " + jobUuid));
    }

    /** Descriptions of every FAILED status-change event alerted to Slack for the given job. */
    private List<String> failureAlertMessages(String jobUuid) {
        ArgumentCaptor<LoggableEvent> event = ArgumentCaptor.forClass(LoggableEvent.class);
        verify(eventLogger, atLeast(0)).logAndAlert(event.capture(), any());
        return event.getAllValues().stream()
                .filter(JobStatusChangeEvent.class::isInstance)
                .map(JobStatusChangeEvent.class::cast)
                .filter(e -> jobUuid.equals(e.getJobId()))
                .filter(e -> JobStatus.FAILED.name().equals(e.getNewStatus()))
                .map(JobStatusChangeEvent::getDescription)
                .toList();
    }

    /** Tag lists passed to each call of the given DogStatsD method for the given aspect. */
    private List<List<String>> tagsOf(String method, String aspect) {
        return invocationArgs(method, aspect).stream()
                .map(args -> Arrays.asList((String[]) args[args.length - 1]))
                .toList();
    }

    /** Values passed to each call of a value-carrying DogStatsD method for the given aspect. */
    private List<Long> counted(String method, String aspect) {
        return invocationArgs(method, aspect).stream().map(args -> ((Number) args[1]).longValue()).toList();
    }

    /**
     * Raw invocation arguments rather than matchers: the DogStatsD API takes tags as varargs, which matchers
     * flatten and make impossible to correlate back to the call that produced them.
     */
    private List<Object[]> invocationArgs(String method, String aspect) {
        return mockingDetails(statsDClient).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals(method))
                .filter(invocation -> aspect.equals(rawArguments(invocation)[0]))
                .map(PrototypeObservabilityIntegrationTest::rawArguments)
                .toList();
    }

    private static Object[] rawArguments(Invocation invocation) {
        return invocation.getRawArguments();
    }
}
