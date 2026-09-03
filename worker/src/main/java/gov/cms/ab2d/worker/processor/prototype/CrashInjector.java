package gov.cms.ab2d.worker.processor.prototype;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * crash-at picks which point in the pipeline dies (process, read, write, assemble) so a run can be
 * crashed one stage at a time. This is deliberately blunt - it halts the JVM, so the worker dies
 * exactly like a real crash with no shutdown hooks or cleanup. See docs/prototype-crash-testing.md.
 */
@Slf4j
@Component
public class CrashInjector {

    private final String crashAt;
    private final double crashProbability;

    public CrashInjector(
            @Value("${pause-resume.prototype.crash-at:process}") String crashAt,
            @Value("${pause-resume.prototype.crash-probability:0}") double crashProbability) {
        this.crashAt = crashAt == null ? "" : crashAt.trim();
        this.crashProbability = crashProbability;
        if (crashProbability > 0) {
            // Loud on purpose: if you see this in a deployed worker's logs, it is going to crash itself.
            log.warn("CRASH-INJECTION ARMED at '{}' with probability {} - this worker will halt itself, "
                    + "FOR RECOVERY TESTING ONLY", this.crashAt, crashProbability);
        }
    }

    /** Halt the worker if injection is armed for this point. A no-op unless crash-probability is set. */
    public void maybeCrash(String point) {
        if (crashProbability <= 0 || !crashAt.equalsIgnoreCase(point)) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() < crashProbability) {
            halt(point);
        }
    }

    /** Split out from the decision so tests can check what would fire without killing the JVM. */
    void halt(String point) {
        log.error("CRASH-INJECTION firing at '{}': halting worker now (exit 137) to test recovery", point);
        Runtime.getRuntime().halt(137);
    }
}
