package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.eventclient.clients.EventClient;
import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import gov.cms.ab2d.eventclient.events.LoggableEvent;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MockEventClient implements EventClient {
    @Override
    public void sendLogs(LoggableEvent loggableEvent) {
        // TDDO - get fancier with mocking at a later point.
    }

    public void alert(String message, List<Ab2dEnvironment> environments) {

    }

    public void trace(String message, List<Ab2dEnvironment> environments) {

    }

    public void logAndAlert(LoggableEvent event, List<Ab2dEnvironment> environments) {

    }

    public void logAndTrace(LoggableEvent event, List<Ab2dEnvironment> environments) {

    }

    public void log(EventClient.LogType type, LoggableEvent event) {

    }
}
