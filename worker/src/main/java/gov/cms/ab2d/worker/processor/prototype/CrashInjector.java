package gov.cms.ab2d.worker.processor.prototype;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Dev/test-only crash injection for exercising the worker's recovery paths on a real deployed container.
 * Off unless crash-probability is set, and it only arms in dev/test/local. crash-at picks which
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
        boolean safeEnv = isSafeEnv(executionEnv);
        if (requested && point.isEmpty()) {
            log.warn("CRASH-INJECTION disabled: crash-at '{}' is not one of {}", crashAt, Arrays.toString(CrashPoint.values()));
        }
        if (requested && !safeEnv) {
            log.warn("CRASH-INJECTION disabled: refusing to arm in '{}', crash testing only runs in dev/test/local", executionEnv);
        }
        this.armed = requested && point.isPresent() && safeEnv;
        if (armed) {
            log.warn("CRASH-INJECTION ARMED at '{}' with probability {} - this worker will halt itself, "
                    + "FOR RECOVERY TESTING ONLY", this.crashAt.configValue(), crashProbability);
        }
    }

    // Only dev, test (deployed as ab2d-east-impl), and local may crash. Anything else - prod, sandbox, or
    // an env we don't recognise - is denied, so a bad or missing value fails closed.
    private static final Set<String> SAFE_ENVS = Set.of("local", "dev", "test", "impl");
    private static boolean isSafeEnv(String executionEnv) {
        if (executionEnv == null) {
            return false;
        }
        String env = executionEnv.toLowerCase();
        return SAFE_ENVS.stream().anyMatch(env::contains);
    }

    public void maybeCrash(CrashPoint point) {
        if (!armed || crashAt != point) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() < crashProbability) {
            halt(point);
        }
    }

    // package-private so tests can stub it instead of actually halting the JVM
    void halt(CrashPoint point) {
        log.error("CRASH-INJECTION firing at '{}': halting worker now (exit 137) to test recovery", point.configValue());
        Runtime.getRuntime().halt(137);
    }
}
