package gov.cms.ab2d.eventclient.messages;

import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import gov.cms.ab2d.eventclient.events.LoggableEvent;
import java.util.List;

import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
public class LogAndTraceSQSMessage extends SQSMessages {

    private LoggableEvent loggableEvent;

    private List<Ab2dEnvironment> environments;

    public LogAndTraceSQSMessage() { }

    public LogAndTraceSQSMessage(LoggableEvent loggableEvent, List<Ab2dEnvironment> environments) {
        super();
        this.loggableEvent = loggableEvent;
        this.environments = environments;
    }
}
