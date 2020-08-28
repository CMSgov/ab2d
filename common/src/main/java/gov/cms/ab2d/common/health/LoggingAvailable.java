package gov.cms.ab2d.common.health;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class LoggingAvailable {

    private LoggingAvailable() { }

    public static boolean canLog() {
        try {
            if (!log.isErrorEnabled()) {
                return false;
            }
            log.info("Logging Health Check");
            return true;
        } catch (Exception ex) {
            System.out.println("Logging failed health check");
            ex.printStackTrace();
            return false;
        }
    }
}
