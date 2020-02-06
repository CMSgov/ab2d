package gov.cms.ab2d.common.health;

import ch.qos.logback.classic.LoggerContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LoggerConfiguration;

@Slf4j
public class LoggingAvailable {
    public static boolean canLog() {
        if (!log.isErrorEnabled()) {
            return false;
        }
        log.info("Logging Health Check");
        return true;
    }
}
