package gov.cms.ab2d.worker.processor.prototype;

import java.util.Optional;

/**
 * The points in the pipeline where a crash can be injected for recovery testing. The config value
 * pause-resume.prototype.crash-at is matched against these by name, case-insensitively.
 */
public enum CrashPoint {

    /** While turning a bene's claims into ndjson. */
    PROCESS,

    /** While reading the next bene out of the partition. */
    READ,

    /** While writing a chunk to the output file. */
    WRITE,

    /** While assembling the finished per-partition files into the delivered output. */
    ASSEMBLE;

    /** The name a worker uses to select this point, e.g. "write". */
    public String configValue() {
        return name().toLowerCase();
    }

    /** The point named by a crash-at value, or empty if it names nothing we recognise. */
    public static Optional<CrashPoint> from(String value) {
        if (value == null) {
            return Optional.empty();
        }
        for (CrashPoint point : values()) {
            if (point.name().equalsIgnoreCase(value.trim())) {
                return Optional.of(point);
            }
        }
        return Optional.empty();
    }
}
