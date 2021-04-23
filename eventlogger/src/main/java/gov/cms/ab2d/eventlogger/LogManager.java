package gov.cms.ab2d.eventlogger;

import gov.cms.ab2d.eventlogger.eventloggers.kinesis.KinesisEventLogger;
import gov.cms.ab2d.eventlogger.eventloggers.slack.SlackLogger;
import gov.cms.ab2d.eventlogger.eventloggers.sql.SqlEventLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class LogManager {
    private final SqlEventLogger sqlEventLogger;
    private final KinesisEventLogger kinesisEventLogger;
    private final SlackLogger slackLogger;

    public LogManager(SqlEventLogger sqlEventLogger, KinesisEventLogger kinesisEventLogger, SlackLogger slackLogger) {
        this.sqlEventLogger = sqlEventLogger;
        this.kinesisEventLogger = kinesisEventLogger;
        this.slackLogger = slackLogger;
    }

    public enum LogType {
        SQL,
        KINESIS
    }

    /**
     * Log an event to all available event loggers without alerting
     * @param event the event to log
     */
    public void log(LoggableEvent event) {
        // Save to the database
        sqlEventLogger.log(event);

        // This event will contain the db id, block so we can get the
        // aws id.
        kinesisEventLogger.log(event, true);

        // This event will not contain the AWS Id, update event in the DB
        sqlEventLogger.updateAwsId(event.getAwsId(), event);
    }

    /**
     * Alert only AB2D team via relevant loggers with a high priority alert
     * @param message message to provide
     * @param environments AB2D environments to alert on
     */
    public void alert(String message, List<Ab2dEnvironment> environments) {
        slackLogger.logAlert(message, environments);
    }

    /**
     * Alert only AB2D team via relevant loggers with a low priority alert
     * @param message message to provide
     * @param environments AB2D environments to alert on
     */
    public void trace(String message, List<Ab2dEnvironment> environments) {
        slackLogger.logTrace(message, environments);
    }

    /**
     * Log the event and alert to relevant alert loggers only in the provided execution environments.
     *
     * The message published will be built using {@link LoggableEvent#asMessage()}
     *
     * @param event event to log an alert for.
     * @param environments environments to log an alert for
     */
    public void logAndAlert(LoggableEvent event, List<Ab2dEnvironment> environments) {
        log(event);

        slackLogger.logAlert(event, environments);
    }

    /**
     * Log the event and provided traces to relevant trace loggers only in the provided execution environments.
     *
     * The message published will be built using {@link LoggableEvent#asMessage()}
     *
     * @param event event to log an alert for.
     * @param environments environments to log an alert for
     */
    public void logAndTrace(LoggableEvent event, List<Ab2dEnvironment> environments) {
        log(event);

        slackLogger.logTrace(event, environments);
    }

    /**
     * Log an event without alerting
     * @param type type of event logger to use
     * @param event event to log
     */
    public void log(LogType type, LoggableEvent event) {
        switch (type) {
            case SQL:
                sqlEventLogger.log(event);
                break;
            case KINESIS:
                kinesisEventLogger.log(event);
                break;
            default:
                break;
        }
    }
}
