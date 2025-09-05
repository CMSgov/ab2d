package gov.cms.ab2d.common.health;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class LoggingAvailable {

    private LoggingAvailable() { }

    public static boolean canLog(org.slf4j.Logger logger) {
        try {
            if (!logger.isErrorEnabled()) {
                return false;
            }
            logger.info("Logging Health Check");
            return true;
        } catch (Exception ex) {
            System.out.println("Logging failed health check");
            ex.printStackTrace();
            return false;
        }
    }
}
