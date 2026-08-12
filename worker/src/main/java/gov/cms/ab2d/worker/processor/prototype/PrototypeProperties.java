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

    /** How many times a job's batch execution can fail before it is marked terminally failed. */
    private int maxFailureAttempts = 8;

    /** How many times a job can be restarted, this is for safety and should basically never reach max. */
    private int maxStartAttempts = 50;

    /** Item-level retries before marking an item as failed. */
    private int itemRetryLimit = 3;

    /** How long shutdown waits for the batch execution to finish cleaning up before shutting down. */
    private long shutdownAwaitMs = 32000;
}
