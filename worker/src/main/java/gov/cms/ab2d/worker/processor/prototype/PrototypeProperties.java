package gov.cms.ab2d.worker.processor.prototype;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

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

    /**
     * Ceiling on beneficiaries being fetched at once, across every prototype job on this worker.
     * The processor uses virtual threads, so it is not bounded by number of threads.
     */
    private int itemConcurrency = 128;

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

    private long maxDurationSecondsAfterCreateLease = Duration.ofMinutes(1).toSeconds();
    private long maxDurationSecondsCreateAggregatedTable = Duration.ofMinutes(10).toSeconds();
    private long maxDurationSecondsAfterWriteCallback = Duration.ofMinutes(2).toSeconds();
    private long maxDurationSecondsAssembleFiles = Duration.ofMinutes(5).toSeconds();
}
