package gov.cms.ab2d.worker.processor.prototype;

import com.timgroup.statsd.StatsDClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Datadog metrics and structured logging for the pause/resume prototype.
 *
 * Every lifecycle event the prototype can go through emits a counter so the whole pause/resume story is
 * visible without reading logs: how a job was claimed (fresh/soft/hard), whether a shutdown drained, whether
 * a worker was fenced out, how close a job is to exhausting its failure attempts, and how many leases are
 * sitting stale. Each emit also logs, so a metric spike always has a matching log line to dig into.
 *
 * The {@link StatsDClient} bean prefixes everything with {@code ab2d}, so the metrics land in Datadog as
 * {@code ab2d.worker.prototype.*}. The client is looked up through an {@link ObjectProvider} so that tests
 * and any context without a DogStatsD sidecar degrade to a no-op instead of failing to start.
 */
@Slf4j
@Component
public class PrototypeMetrics {

    // Job lifecycle
    static final String JOB_STARTED = "worker.prototype.job.started";
    static final String JOB_RECOVERED = "worker.prototype.job.recovered";
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

    // Failures
    static final String JOB_FAILED = "worker.prototype.job.failed";
    static final String JOB_FAILURE_ATTEMPT = "worker.prototype.job.failure_attempt";
    static final String JOB_ATTEMPTS_REMAINING = "worker.prototype.job.attempts_remaining";
    static final String JOB_APPROACHING_MAX_FAILURES = "worker.prototype.job.approaching_max_failures";
    static final String JOB_THRESHOLD_EXCEEDED = "worker.prototype.job.threshold_exceeded";
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
     * A worker has claimed a job and is about to run it. {@code mode} separates a fresh start from the two
     * kinds of resume, which is the top-level signal for "is recovery happening, and which kind".
     */
    public void jobStarted(String jobUuid, String contract, ResumeMode mode, long fenceToken) {
        log.info("prototype metrics: job {} started contract={} mode={} token={}",
                jobUuid, contract, mode.tagValue(), fenceToken);
        increment(JOB_STARTED, tags(contract, "mode:" + mode.tagValue()));
        if (mode.isRecovery()) {
            // A second, recovery-only counter so a Datadog monitor can watch resumes without filtering
            // every job start. Soft and hard stay separate on the mode tag.
            increment(JOB_RECOVERED, tags(contract, "mode:" + mode.tagValue()));
        }
    }

    /** The batch execution completed, the output assembled, and the job was marked SUCCESSFUL. */
    public void jobCompleted(String jobUuid, String contract, int patientsProcessed, int outputFiles) {
        log.info("prototype metrics: job {} completed contract={} patients={} files={}",
                jobUuid, contract, patientsProcessed, outputFiles);
        increment(JOB_COMPLETED, tags(contract));
        count(JOB_PATIENTS_PROCESSED, patientsProcessed, tags(contract));
    }

    /** The job was cancelled out from under the worker while it was running. */
    public void jobCancelled(String jobUuid, String contract) {
        log.warn("prototype metrics: job {} cancelled mid-run contract={}", jobUuid, contract);
        increment(JOB_CANCELLED, tags(contract));
    }

    /**
     * The batch execution stopped for a shutdown and the job went back to SUBMITTED.
     *
     * @param cleanSuspend whether the clean-suspend marker was recorded. False means the next pickup has to
     *                     hard-recover instead of resuming in place, which is the expensive path.
     */
    public void jobSuspended(String jobUuid, String contract, long fenceToken, boolean cleanSuspend) {
        if (cleanSuspend) {
            log.info("prototype metrics: job {} suspended cleanly contract={} token={}",
                    jobUuid, contract, fenceToken);
        } else {
            log.warn("prototype metrics: job {} suspended WITHOUT a clean-suspend marker contract={} token={} - "
                    + "the next pickup will have to hard-recover", jobUuid, contract, fenceToken);
        }
        increment(JOB_SUSPENDED, tags(contract, "clean:" + cleanSuspend));
    }

    /**
     * Result of the hard-recovery copy-forward pass: how many partitions kept their committed chunks and how
     * many had to be thrown away and redone.
     */
    public void copyForward(String jobUuid, long fenceToken, int seeded, int restarted) {
        log.info("prototype metrics: job {} copy-forward token={} seeded={} restarted={}",
                jobUuid, fenceToken, seeded, restarted);
        if (seeded > 0) {
            count(COPY_FORWARD_SEEDED, seeded, tags(null));
        }
        if (restarted > 0) {
            count(COPY_FORWARD_RESTARTED, restarted, tags(null));
        }
    }

    /** A shutdown started signalling running batch executions to stop at their next chunk boundary. */
    public void drainStarted(int runningExecutions) {
        log.info("prototype metrics: shutdown drain started executions={}", runningExecutions);
        increment(DRAIN_STARTED, tags(null));
    }

    /**
     * The shutdown drain finished. {@code drained=false} means the wait timed out with executions still
     * running, so the affected jobs will be hard-recovered rather than resumed in place.
     */
    public void drainFinished(boolean drained, long elapsedMs, int stillRunning) {
        if (drained) {
            log.info("prototype metrics: shutdown drain finished in {}ms", elapsedMs);
        } else {
            log.warn("prototype metrics: shutdown drain timed out after {}ms with {} execution(s) still running",
                    elapsedMs, stillRunning);
        }
        increment(DRAIN_FINISHED, tags(null, "drained:" + drained));
        time(DRAIN_DURATION_MS, elapsedMs, tags(null, "drained:" + drained));
    }

    /**
     * This worker ran under a token that is no longer current, so a peer owns the job now. Expected when a
     * worker stalls past the lease TTL; a steady stream of these means heartbeats are not keeping up.
     */
    public void fencedOut(String jobUuid, String contract, long ranUnderToken, String phase) {
        log.warn("prototype metrics: job {} fenced out contract={} ranUnderToken={} phase={}",
                jobUuid, contract, ranUnderToken, phase);
        increment(JOB_FENCED_OUT, tags(contract, "phase:" + phase));
    }

    /** A chunk commit was rejected because this worker no longer holds the lease. */
    public void chunkFenceLost(String jobUuid, long token) {
        log.warn("prototype metrics: job {} lost the fence at token {} while committing a chunk", jobUuid, token);
        increment(CHUNK_FENCE_LOST, tags(null));
    }

    /** The scheduled heartbeat renewal found the lease no longer held at this worker's token. */
    public void leaseRenewFailed(String jobUuid, long token) {
        log.warn("prototype metrics: job {} lease renewal failed at token {}", jobUuid, token);
        increment(LEASE_RENEW_FAILED, tags(null));
    }

    /**
     * Fleet-wide view of IN_PROGRESS jobs and their leases.
     *
     * @param active      leases whose heartbeat is inside the TTL
     * @param stale       leases past the TTL and therefore eligible for takeover
     * @param unrecovered leases past the longer grace window, which means a takeover should already have
     *                    happened and did not - recovery is broken
     */
    public void leaseHeartbeats(long active, long stale, long unrecovered) {
        gauge(LEASE_ACTIVE, active, tags(null));
        gauge(LEASE_STALE, stale, tags(null));
        gauge(LEASE_UNRECOVERED, unrecovered, tags(null));
        if (unrecovered > 0) {
            log.error("prototype metrics: {} IN_PROGRESS job(s) past the recovery grace window "
                    + "(active={}, stale={}) - recovery is not picking them up", unrecovered, active, stale);
        } else if (stale > 0) {
            log.info("prototype metrics: {} IN_PROGRESS job(s) with a stale lease awaiting takeover (active={})",
                    stale, active);
        }
    }

    /**
     * A job reached a terminal FAILED state. One of the three failure paths in the processor.
     */
    public void jobFailed(String jobUuid, String contract, FailureReason reason, String message) {
        log.error("prototype metrics: job {} FAILED contract={} reason={} message={}",
                jobUuid, contract, reason.tagValue(), message);
        increment(JOB_FAILED, tags(contract, "reason:" + reason.tagValue()));
    }

    /**
     * A recoverable failure sent the job back to SUBMITTED for another attempt. Emits which attempt this is
     * and how many are left, so a job grinding toward its cap is visible before it dies.
     */
    public void jobFailureAttempt(String jobUuid, String contract, int attempt, int maxAttempts,
                                  boolean approachingMax) {
        int remaining = Math.max(0, maxAttempts - attempt);
        if (approachingMax) {
            log.warn("prototype metrics: job {} failed attempt {} of {} contract={} - only {} attempt(s) left "
                    + "before it fails terminally", jobUuid, attempt, maxAttempts, contract, remaining);
        } else {
            log.info("prototype metrics: job {} failed attempt {} of {} contract={} - will resume",
                    jobUuid, attempt, maxAttempts, contract);
        }
        gauge(JOB_FAILURE_ATTEMPT, attempt, tags(contract));
        gauge(JOB_ATTEMPTS_REMAINING, remaining, tags(contract));
        if (approachingMax) {
            increment(JOB_APPROACHING_MAX_FAILURES, tags(contract));
        }
    }

    /** Too many beneficiaries errored, so the job is failed terminally rather than delivering a partial file. */
    public void thresholdExceeded(String jobUuid, String contract, int failures, int total) {
        log.error("prototype metrics: job {} exceeded the failure threshold contract={} failed={} of {}",
                jobUuid, contract, failures, total);
        increment(JOB_THRESHOLD_EXCEEDED, tags(contract));
    }

    /** One beneficiary was skipped after failing persistently. Counted against the failure threshold. */
    public void beneSkipped(String jobUuid, String phase, Throwable cause) {
        log.warn("prototype metrics: job {} skipped a beneficiary phase={} cause={}",
                jobUuid, phase, cause == null ? "unknown" : cause.getClass().getSimpleName());
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
