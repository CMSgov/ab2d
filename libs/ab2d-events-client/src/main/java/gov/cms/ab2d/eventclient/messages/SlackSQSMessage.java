package gov.cms.ab2d.eventclient.messages;

import gov.cms.ab2d.eventclient.events.LoggableEvent;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
public class SlackSQSMessage extends SQSMessages {
    private LoggableEvent loggableEvent;

    public SlackSQSMessage() { }

    public SlackSQSMessage(LoggableEvent loggableEvent) {
        super();
        this.loggableEvent = loggableEvent;
    }
}
