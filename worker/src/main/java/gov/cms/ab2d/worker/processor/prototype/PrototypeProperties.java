package gov.cms.ab2d.worker.processor.prototype;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Props of the prototype job processor, provided here as a component so that the
 * job processor's already behemoth constructor doesn't need an individual line for
 * each of the configs
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "pause-resume.prototype")
public class PrototypeProperties {

    /** Beneficiaries per partition. */
    private int partitionSize = 1000;

    /** Beneficiaries per chunk commit. */
    private int chunkSize = 100;

    /** Number of partitions processed concurrently. */
    private int concurrency = 4;

    /**
     * Whether a partition processes its chunk's beneficiaries concurrently.
     */
    private boolean itemConcurrencyEnabled = true;

    /** How many times a job's batch execution can fail before it is marked terminally failed. */
    private int maxFailureAttempts = 8;

    /** How many times a job can be restarted, this is for safety and should basically never reach max. */
    private int maxStartAttempts = 50;

    /** Item-level retries before marking an item as failed. */
    private int itemRetryLimit = 3;

    /** How long shutdown waits for the batch execution to finish cleaning up before shutting down. */
    private long shutdownAwaitMs = 32000;

    /**
     * Whether hard recovery restarts partitions from scratch or from the last good chunk
     */
    private boolean copyForwardEnabled = true;

    /**
     * How many attempts can be left before a failing job is reported as approaching its cap. Failing a job
     * is expected and recoverable, but grinding toward the terminal failure is worth knowing about early.
     */
    private int failureAttemptsWarnRemaining = 2;

    /**
     * Multiple of the lease TTL after which an unrecovered IN_PROGRESS job means recovery is not working.
     * A job past the TTL is normally taken over on the next poll, so anything still there several TTLs
     * later is stranded.
     */
    private int leaseGraceMultiplier = 3;

    /**
     * How long a stranded job stays quiet after it has been reported. A stranded job stays stranded, so
     * without a cooldown every worker would report it on every poll; the claim is stamped in the database so
     * this rate limit holds across the whole fleet, not just per worker.
     */
    private int leaseAlertCooldownMinutes = 60;
}
