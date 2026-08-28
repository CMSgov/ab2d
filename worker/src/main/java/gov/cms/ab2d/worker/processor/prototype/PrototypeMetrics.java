package gov.cms.ab2d.worker.processor.prototype;

import com.timgroup.statsd.StatsDClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Datadog metrics for the pause/resume prototype.
 *
 * The {@link StatsDClient} bean prefixes everything with {@code ab2d}, so the metrics land in Datadog as
 * {@code ab2d.worker.prototype.*}. The client is looked up through an {@link ObjectProvider} so that tests
 * and any context without a DogStatsD sidecar degrade to a no-op instead of failing to start.
 */
@Slf4j
@Component
public class PrototypeMetrics {

    // Job lifecycle. The resume mode is a tag on the start, so fresh/soft/hard are all one metric.
    static final String JOB_STARTED = "worker.prototype.job.started";
    static final String JOB_COMPLETED = "worker.prototype.job.completed";
    static final String JOB_CANCELLED = "worker.prototype.job.cancelled";
    static final String JOB_PATIENTS_PROCESSED = "worker.prototype.job.patients_processed";

    // Pause / resume
    static final String JOB_SUSPENDED = "worker.prototype.job.suspended";
    static final String COPY_FORWARD_SEEDED = "worker.prototype.copy_forward.seeded";
    static final String COPY_FORWARD_RESTARTED = "worker.prototype.copy_forward.restarted";

    // Shutdown drain
    static final String DRAIN_STARTED = "worker.prototype.shutdown.drain_started";
    static final String DRAIN_FINISHED = "worker.prototype.shutdown.drain_finished";
    static final String DRAIN_DURATION_MS = "worker.prototype.shutdown.drain_duration_ms";

    // Ownership / fencing
    static final String JOB_FENCED_OUT = "worker.prototype.job.fenced_out";
    static final String CHUNK_FENCE_LOST = "worker.prototype.chunk.fence_lost";
    static final String LEASE_RENEW_FAILED = "worker.prototype.lease.renew_failed";
    static final String LEASE_ACTIVE = "worker.prototype.lease.active";
    static final String LEASE_STALE = "worker.prototype.lease.stale";
    static final String LEASE_UNRECOVERED = "worker.prototype.lease.unrecovered";

    // Failures. The reason tag distinguishes the three terminal paths, including the failure threshold.
    static final String JOB_FAILED = "worker.prototype.job.failed";
    static final String JOB_FAILURE_ATTEMPT = "worker.prototype.job.failure_attempt";
    static final String JOB_ATTEMPTS_REMAINING = "worker.prototype.job.attempts_remaining";
    static final String JOB_APPROACHING_MAX_FAILURES = "worker.prototype.job.approaching_max_failures";
    static final String BENE_SKIPPED = "worker.prototype.bene.skipped";

    private final String executionEnv;
    private final StatsDClient statsDClient;

    public PrototypeMetrics(
            @Value("${execution.env:local}") String executionEnv,
            ObjectProvider<StatsDClient> statsDClientProvider) {
        this.executionEnv = executionEnv;
        this.statsDClient = statsDClientProvider.getIfAvailable();
        if (this.statsDClient == null) {
            log.info("No StatsDClient bean available; pause/resume prototype metrics are disabled (no-op)");
        }
    }

    /**
     * A worker has claimed a job and is about to run it. The {@code mode} tag separates a fresh start from
     * the two kinds of resume, so recoveries are counted by filtering rather than by a second metric.
     */
    public void jobStarted(String contract, ResumeMode mode) {
        increment(JOB_STARTED, tags(contract, "mode:" + mode.tagValue()));
    }

    /** The batch execution completed, the output assembled, and the job was marked SUCCESSFUL. */
    public void jobCompleted(String contract, int patientsProcessed) {
        increment(JOB_COMPLETED, tags(contract));
        count(JOB_PATIENTS_PROCESSED, patientsProcessed, tags(contract));
    }

    /** The job was cancelled out from under the worker while it was running. */
    public void jobCancelled(String contract) {
        increment(JOB_CANCELLED, tags(contract));
    }

    /**
     * The batch execution stopped for a shutdown and the job went back to SUBMITTED.
     *
     * @param cleanSuspend whether the clean-suspend marker was recorded. False means the next pickup has to
     *                     hard-recover instead of resuming in place, which is the expensive path.
     */
    public void jobSuspended(String contract, boolean cleanSuspend) {
        increment(JOB_SUSPENDED, tags(contract, "clean:" + cleanSuspend));
    }

    /**
     * Result of the hard-recovery copy-forward pass: how many partitions kept their committed chunks and how
     * many had to be thrown away and redone.
     */
    public void copyForward(int seeded, int restarted) {
        if (seeded > 0) {
            count(COPY_FORWARD_SEEDED, seeded, tags(null));
        }
        if (restarted > 0) {
            count(COPY_FORWARD_RESTARTED, restarted, tags(null));
        }
    }

    /** A shutdown started signalling running batch executions to stop at their next chunk boundary. */
    public void drainStarted() {
        increment(DRAIN_STARTED, tags(null));
    }

    /**
     * The shutdown drain finished. {@code drained=false} means the wait timed out with executions still
     * running, so the affected jobs will be hard-recovered rather than resumed in place.
     */
    public void drainFinished(boolean drained, long elapsedMs) {
        increment(DRAIN_FINISHED, tags(null, "drained:" + drained));
        time(DRAIN_DURATION_MS, elapsedMs, tags(null, "drained:" + drained));
    }

    /**
     * This worker ran under a token that is no longer current, so a peer owns the job now. Expected when a
     * worker stalls past the lease TTL; a steady stream of these means heartbeats are not keeping up.
     */
    public void fencedOut(String contract, String phase) {
        increment(JOB_FENCED_OUT, tags(contract, "phase:" + phase));
    }

    /** A chunk commit was rejected because this worker no longer holds the lease. */
    public void chunkFenceLost() {
        increment(CHUNK_FENCE_LOST, tags(null));
    }

    /** The scheduled heartbeat renewal found the lease no longer held at this worker's token. */
    public void leaseRenewFailed() {
        increment(LEASE_RENEW_FAILED, tags(null));
    }

    /**
     * Fleet-wide view of IN_PROGRESS jobs and their leases. The three buckets are mutually exclusive.
     *
     * @param active      leases whose heartbeat is inside the TTL
     * @param stale       leases past the TTL but still inside the grace window, so a takeover is expected
     * @param unrecovered leases past the grace window, which means a takeover should already have happened
     *                    and did not - recovery is broken
     */
    public void leaseHeartbeats(long active, long stale, long unrecovered) {
        gauge(LEASE_ACTIVE, active, tags(null));
        gauge(LEASE_STALE, stale, tags(null));
        gauge(LEASE_UNRECOVERED, unrecovered, tags(null));
    }

    /**
     * A job reached a terminal FAILED state. The reason tag covers all three failure paths, including the
     * failure threshold, so no separate threshold metric is needed.
     */
    public void jobFailed(String contract, FailureReason reason) {
        increment(JOB_FAILED, tags(contract, "reason:" + reason.tagValue()));
    }

    /**
     * A recoverable failure sent the job back to SUBMITTED for another attempt. Emits which attempt this is
     * and how many are left, so a job grinding toward its cap is visible before it dies.
     */
    public void jobFailureAttempt(String contract, int attempt, int maxAttempts, boolean approachingMax) {
        gauge(JOB_FAILURE_ATTEMPT, attempt, tags(contract));
        gauge(JOB_ATTEMPTS_REMAINING, Math.max(0, maxAttempts - attempt), tags(contract));
        if (approachingMax) {
            increment(JOB_APPROACHING_MAX_FAILURES, tags(contract));
        }
    }

    /** One beneficiary was skipped after failing persistently. Counted against the failure threshold. */
    public void beneSkipped(String phase, Throwable cause) {
        increment(BENE_SKIPPED, tags(null, "phase:" + phase,
                "cause:" + (cause == null ? "unknown" : cause.getClass().getSimpleName())));
    }

    private void increment(String aspect, String[] tags) {
        if (statsDClient != null) {
            statsDClient.increment(aspect, tags);
        }
    }

    private void count(String aspect, long value, String[] tags) {
        if (statsDClient != null) {
            statsDClient.count(aspect, value, tags);
        }
    }

    private void gauge(String aspect, long value, String[] tags) {
        if (statsDClient != null) {
            statsDClient.gauge(aspect, value, tags);
        }
    }

    private void time(String aspect, long millis, String[] tags) {
        if (statsDClient != null) {
            statsDClient.recordExecutionTime(aspect, millis, tags);
        }
    }

    /**
     * Every metric carries the environment. Contract is added when the caller knows it; the extra tags are
     * the per-event dimensions.
     */
    private String[] tags(String contract, String... extra) {
        List<String> tags = new ArrayList<>(2 + extra.length);
        tags.add("environment:" + executionEnv);
        if (contract != null) {
            tags.add("contract:" + contract);
        }
        tags.addAll(List.of(extra));
        return tags.toArray(new String[0]);
    }

    /** Which of the processor's failure paths ended the job. */
    public enum FailureReason {

        /** Too many beneficiaries errored during the run. */
        THRESHOLD_EXCEEDED,

        /** The batch execution failed repeatedly and ran out of resume attempts. */
        ATTEMPTS_EXHAUSTED,

        /** The batch job could not be launched or threw outside the execution itself. */
        LAUNCH_FAILED;

        public String tagValue() {
            return name().toLowerCase();
        }
    }
}
