package gov.cms.ab2d.worker.processor.prototype;

import java.util.Optional;

/**
 * The points in the pipeline where a crash can be injected for recovery testing. The config value
 * pause-resume.prototype.crash-at is matched against these by name, case-insensitively.
 */
public enum CrashPoint {

    PROCESS,
    READ,
    WRITE,
    ASSEMBLE;

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
