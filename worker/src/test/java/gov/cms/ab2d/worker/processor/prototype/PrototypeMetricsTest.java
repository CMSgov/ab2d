package gov.cms.ab2d.worker.processor.prototype;

import com.timgroup.statsd.StatsDClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.Invocation;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the pause/resume metric emitter. These assert the metric names and tag values that Datadog
 * monitors and dashboards key off, so a rename here is caught rather than silently breaking an alert.
 */
class PrototypeMetricsTest {

    private static final String ENV = "ab2d-dev";
    private static final String CONTRACT = "Z0001";
    private static final String JOB = "job-uuid";

    private StatsDClient statsDClient;
    private PrototypeMetrics metrics;

    @BeforeEach
    void setUp() {
        statsDClient = mock(StatsDClient.class);
        metrics = new PrototypeMetrics(ENV, providerOf(statsDClient));
    }

    @Test
    @DisplayName("A fresh start counts as a start but not as a recovery")
    void freshStartIsNotARecovery() {
        metrics.jobStarted(JOB, CONTRACT, ResumeMode.FRESH, 1L);

        List<String[]> started = incrementTags(PrototypeMetrics.JOB_STARTED);
        assertEquals(1, started.size(), "a start should be counted once");
        assertTrue(Arrays.asList(started.get(0)).contains("mode:fresh"));
        assertTrue(Arrays.asList(started.get(0)).contains("environment:" + ENV));
        assertTrue(Arrays.asList(started.get(0)).contains("contract:" + CONTRACT));
        assertTrue(incrementTags(PrototypeMetrics.JOB_RECOVERED).isEmpty(),
                "a first claim has no prior work, so it is not a recovery");
    }

    @Test
    @DisplayName("Soft and hard resumes are both recoveries but stay distinguishable by tag")
    void softAndHardRecoveriesAreTaggedSeparately() {
        metrics.jobStarted(JOB, CONTRACT, ResumeMode.SOFT, 4L);
        metrics.jobStarted("other-job", CONTRACT, ResumeMode.HARD, 9L);

        List<String[]> recovered = incrementTags(PrototypeMetrics.JOB_RECOVERED);
        assertEquals(2, recovered.size(), "both resumes should count as recoveries");
        assertTrue(Arrays.asList(recovered.get(0)).contains("mode:soft"));
        assertTrue(Arrays.asList(recovered.get(1)).contains("mode:hard"));
    }

    @Test
    @DisplayName("Each failure reason is tagged so the three failure paths can be told apart")
    void failureReasonsAreTagged() {
        metrics.jobFailed(JOB, CONTRACT, PrototypeMetrics.FailureReason.THRESHOLD_EXCEEDED, "too many errors");
        metrics.jobFailed(JOB, CONTRACT, PrototypeMetrics.FailureReason.ATTEMPTS_EXHAUSTED, "out of attempts");
        metrics.jobFailed(JOB, CONTRACT, PrototypeMetrics.FailureReason.LAUNCH_FAILED, "could not launch");

        List<String[]> failed = incrementTags(PrototypeMetrics.JOB_FAILED);
        assertEquals(3, failed.size(), "every failure path should be counted");
        assertTrue(Arrays.asList(failed.get(0)).contains("reason:threshold_exceeded"));
        assertTrue(Arrays.asList(failed.get(1)).contains("reason:attempts_exhausted"));
        assertTrue(Arrays.asList(failed.get(2)).contains("reason:launch_failed"));
    }

    @Test
    @DisplayName("A job running low on resume attempts is flagged, one with attempts to spare is not")
    void approachingMaxFailuresOnlyCountsWhenAttemptsRunLow() {
        metrics.jobFailureAttempt(JOB, CONTRACT, 2, 8, false);
        assertTrue(incrementTags(PrototypeMetrics.JOB_APPROACHING_MAX_FAILURES).isEmpty(),
                "an early failure is routine and should not raise the approaching-max signal");

        metrics.jobFailureAttempt(JOB, CONTRACT, 7, 8, true);
        assertEquals(1, incrementTags(PrototypeMetrics.JOB_APPROACHING_MAX_FAILURES).size());
        assertEquals(1, gaugeValues(PrototypeMetrics.JOB_ATTEMPTS_REMAINING).stream()
                .filter(value -> value == 1L).count(), "one attempt should be reported as remaining");
    }

    @Test
    @DisplayName("A suspend records whether the clean-suspend marker was written")
    void suspendRecordsWhetherItWasClean() {
        metrics.jobSuspended(JOB, CONTRACT, 3L, true);
        metrics.jobSuspended(JOB, CONTRACT, 3L, false);

        List<String[]> suspended = incrementTags(PrototypeMetrics.JOB_SUSPENDED);
        assertEquals(2, suspended.size());
        assertTrue(Arrays.asList(suspended.get(0)).contains("clean:true"));
        assertTrue(Arrays.asList(suspended.get(1)).contains("clean:false"),
                "a suspend that could not be marked clean forces a hard recovery later, so it must be visible");
    }

    @Test
    @DisplayName("A drain that times out is recorded as not drained, with its duration")
    void drainRecordsOutcomeAndDuration() {
        metrics.drainStarted(2);
        metrics.drainFinished(false, 32000L, 2);

        assertEquals(1, incrementTags(PrototypeMetrics.DRAIN_STARTED).size());
        List<String[]> finished = incrementTags(PrototypeMetrics.DRAIN_FINISHED);
        assertEquals(1, finished.size());
        assertTrue(Arrays.asList(finished.get(0)).contains("drained:false"));
        assertEquals(List.of(32000L), executionTimes(PrototypeMetrics.DRAIN_DURATION_MS));
    }

    @Test
    @DisplayName("Skipped beneficiaries are tagged with the phase that dropped them")
    void skippedBenesCarryPhase() {
        metrics.beneSkipped(JOB, "process", new IllegalStateException("boom"));

        List<String[]> skipped = incrementTags(PrototypeMetrics.BENE_SKIPPED);
        assertEquals(1, skipped.size());
        assertTrue(Arrays.asList(skipped.get(0)).contains("phase:process"));
        assertTrue(Arrays.asList(skipped.get(0)).contains("cause:IllegalStateException"));
    }

    @Test
    @DisplayName("Lease health gauges active, stale, and unrecovered separately")
    void leaseHealthGaugesEachBucket() {
        metrics.leaseHeartbeats(3, 2, 1);

        assertEquals(List.of(3L), gaugeValues(PrototypeMetrics.LEASE_ACTIVE));
        assertEquals(List.of(2L), gaugeValues(PrototypeMetrics.LEASE_STALE));
        assertEquals(List.of(1L), gaugeValues(PrototypeMetrics.LEASE_UNRECOVERED));
    }

    @Test
    @DisplayName("Copy-forward reports carried and restarted partitions, and stays quiet when there are none")
    void copyForwardReportsBothOutcomes() {
        metrics.copyForward(JOB, 2L, 0, 0);
        assertTrue(counts(PrototypeMetrics.COPY_FORWARD_SEEDED).isEmpty()
                        && counts(PrototypeMetrics.COPY_FORWARD_RESTARTED).isEmpty(),
                "a recovery that carried nothing and restarted nothing has nothing to report");

        metrics.copyForward(JOB, 3L, 2, 1);
        assertEquals(List.of(2L), counts(PrototypeMetrics.COPY_FORWARD_SEEDED));
        assertEquals(List.of(1L), counts(PrototypeMetrics.COPY_FORWARD_RESTARTED));
    }

    @Test
    @DisplayName("Without a DogStatsD client the emitter is a no-op instead of failing")
    void noStatsDClientIsANoOp() {
        PrototypeMetrics noClient = new PrototypeMetrics(ENV, providerOf(null));

        assertDoesNotThrow(() -> {
            noClient.jobStarted(JOB, CONTRACT, ResumeMode.HARD, 2L);
            noClient.jobFailed(JOB, CONTRACT, PrototypeMetrics.FailureReason.LAUNCH_FAILED, "boom");
            noClient.jobFailureAttempt(JOB, CONTRACT, 7, 8, true);
            noClient.leaseHeartbeats(1, 1, 1);
            noClient.chunkFenceLost(JOB, 2L);
            noClient.drainFinished(true, 10L, 0);
        });
    }

    @Test
    @DisplayName("Metrics without a contract still carry the environment tag")
    void environmentIsAlwaysTagged() {
        metrics.chunkFenceLost(JOB, 5L);

        List<String[]> fenceLost = incrementTags(PrototypeMetrics.CHUNK_FENCE_LOST);
        assertEquals(1, fenceLost.size());
        assertTrue(Arrays.asList(fenceLost.get(0)).contains("environment:" + ENV));
        assertFalse(Arrays.stream(fenceLost.get(0)).anyMatch(tag -> tag.startsWith("contract:")),
                "a chunk-level fence loss has no contract to tag with");
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<StatsDClient> providerOf(StatsDClient client) {
        ObjectProvider<StatsDClient> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(client);
        return provider;
    }

    /** The tag arrays passed to every {@code increment} of the given aspect, in call order. */
    private List<String[]> incrementTags(String aspect) {
        return argumentsOf("increment", aspect).stream().map(args -> (String[]) args[1]).toList();
    }

    /** The values passed to every {@code gauge} of the given aspect, in call order. */
    private List<Long> gaugeValues(String aspect) {
        return argumentsOf("gauge", aspect).stream().map(args -> ((Number) args[1]).longValue()).toList();
    }

    /** The values passed to every {@code count} of the given aspect, in call order. */
    private List<Long> counts(String aspect) {
        return argumentsOf("count", aspect).stream().map(args -> ((Number) args[1]).longValue()).toList();
    }

    /** The durations passed to every {@code recordExecutionTime} of the given aspect, in call order. */
    private List<Long> executionTimes(String aspect) {
        return argumentsOf("recordExecutionTime", aspect).stream()
                .map(args -> ((Number) args[1]).longValue()).toList();
    }

    /**
     * Raw invocation arguments are used instead of argument matchers because the DogStatsD API takes tags as
     * varargs, which matchers flatten and make impossible to correlate back to the call that produced them.
     */
    private List<Object[]> argumentsOf(String method, String aspect) {
        return mockingDetails(statsDClient).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals(method))
                .filter(invocation -> aspect.equals(rawArguments(invocation)[0]))
                .map(PrototypeMetricsTest::rawArguments)
                .toList();
    }

    private static Object[] rawArguments(Invocation invocation) {
        return invocation.getRawArguments();
    }
}
