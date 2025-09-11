package gov.cms.ab2d.eventclient.clients;

import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import gov.cms.ab2d.eventclient.events.LoggableEvent;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface EventClient {

    enum LogType {
        SQL,
        KINESIS
    }

    void sendLogs(LoggableEvent requestEvent);

    void alert(String message, List<Ab2dEnvironment> environments);

    void trace(String message, List<Ab2dEnvironment> environments);

    void logAndAlert(LoggableEvent event, List<Ab2dEnvironment> environments);

    void logAndTrace(LoggableEvent event, List<Ab2dEnvironment> environments);

    void log(LogType type, LoggableEvent event);
}
