package gov.cms.ab2d.eventclient.messages;

import gov.cms.ab2d.eventclient.events.LoggableEvent;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
public class GeneralSQSMessage extends SQSMessages {
    private LoggableEvent loggableEvent;

    public GeneralSQSMessage() { }

    public GeneralSQSMessage(LoggableEvent loggableEvent) {
        super();
        this.loggableEvent = loggableEvent;
    }
}
