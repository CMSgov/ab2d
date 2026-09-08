package gov.cms.ab2d.worker.processor.prototype;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Dev/test-only crash injection for exercising the worker's recovery paths on a real deployed container.
 * Off unless crash-probability is set, and it refuses to arm in prod or sandbox. crash-at picks which
 * {@link CrashPoint} in the pipeline dies, so a run can be crashed one stage at a time. Deliberately blunt
 * - it halts the JVM, so the worker dies like a real crash with no cleanup. See docs/prototype-crash-testing.md.
 */
@Slf4j
@Component
public class CrashInjector {

    private final CrashPoint crashAt;
    private final double crashProbability;
    private final boolean armed;

    public CrashInjector(
            @Value("${pause-resume.prototype.crash-at:process}") String crashAt,
            @Value("${pause-resume.prototype.crash-probability:0}") double crashProbability,
            @Value("${execution.env:local}") String executionEnv) {
        this.crashProbability = crashProbability;
        Optional<CrashPoint> point = CrashPoint.from(crashAt);
        this.crashAt = point.orElse(null);

        boolean requested = crashProbability > 0;
        boolean protectedEnv = isProtectedEnv(executionEnv);
        if (requested && point.isEmpty()) {
            log.warn("CRASH-INJECTION disabled: crash-at '{}' is not one of {}", crashAt, Arrays.toString(CrashPoint.values()));
        }
        if (requested && protectedEnv) {
            log.warn("CRASH-INJECTION disabled: refusing to arm in '{}', crash testing is for dev/test only", executionEnv);
        }

        this.armed = requested && point.isPresent() && !protectedEnv;
        if (armed) {
            // Loud on purpose: if you see this in a deployed worker's logs, it is going to crash itself.
            log.warn("CRASH-INJECTION ARMED at '{}' with probability {} - this worker will halt itself, "
                    + "FOR RECOVERY TESTING ONLY", this.crashAt.configValue(), crashProbability);
        }
    }

    /** A prod or sandbox worker must never crash itself, no matter what the properties say. */
    private static boolean isProtectedEnv(String executionEnv) {
        String env = executionEnv == null ? "" : executionEnv.toLowerCase();
        return env.contains("prod") || env.contains("sandbox");
    }

    /** Halt the worker if injection is armed for this point. A no-op unless armed for testing. */
    public void maybeCrash(CrashPoint point) {
        if (!armed || crashAt != point) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() < crashProbability) {
            halt(point);
        }
    }

    /** Split out from the decision so tests can check what would fire without killing the JVM. */
    void halt(CrashPoint point) {
        log.error("CRASH-INJECTION firing at '{}': halting worker now (exit 137) to test recovery", point.configValue());
        Runtime.getRuntime().halt(137);
    }
}
