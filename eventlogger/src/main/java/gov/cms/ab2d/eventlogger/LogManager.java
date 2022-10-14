package gov.cms.ab2d.eventlogger;


import gov.cms.ab2d.eventclient.clients.EventClient;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import gov.cms.ab2d.eventclient.events.LoggableEvent;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LogManager {
    private final SQSEventClient eventClient;

    public LogManager(SQSEventClient eventClient) {
         this.eventClient = eventClient;
    }

    /**
     * Log an event to all available event loggers without alerting
     *
     * @param event the event to log
     */
    public void log(LoggableEvent event) {
        //send to sqs
        eventClient.sendLogs(event);
    }

    /**
     * Alert only AB2D team via relevant loggers with a high priority alert
     *
     * @param message      message to provide
     * @param environments AB2D environments to alert on
     */
    public void alert(String message, List<Ab2dEnvironment> environments) {
        eventClient.alert(message, environments);
    }

    /**
     * Alert only AB2D team via relevant loggers with a low priority alert
     *
     * @param message      message to provide
     * @param environments AB2D environments to alert on
     */
    public void trace(String message, List<Ab2dEnvironment> environments) {
        eventClient.trace(message, environments);
    }

    /**
     * Log the event and alert to relevant alert loggers only in the provided execution environments.
     * The message published will be built using {@link LoggableEvent#asMessage()}
     *
     * @param event        event to log an alert for.
     * @param environments environments to log an alert for
     */
    public void logAndAlert(LoggableEvent event, List<Ab2dEnvironment> environments) {
        eventClient.logAndAlert(event, environments);
    }

    /**
     * Log the event and provided traces to relevant trace loggers only in the provided execution environments.
     * The message published will be built using {@link LoggableEvent#asMessage()}
     *
     * @param event        event to log an alert for.
     * @param environments environments to log an alert for
     */
    public void logAndTrace(LoggableEvent event, List<Ab2dEnvironment> environments) {
        eventClient.logAndTrace(event, environments);
    }

    /**
     * Log an event without alerting
     *
     * @param type  type of event logger to use
     * @param event event to log
     */
    public void log(EventClient.LogType type, LoggableEvent event) {
        eventClient.log(type, event);
    }
}
