package gov.cms.ab2d.eventclient.messages;

import gov.cms.ab2d.eventclient.events.LoggableEvent;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
public class KinesisSQSMessage extends SQSMessages {
    private LoggableEvent loggableEvent;

    public KinesisSQSMessage() { }

    public KinesisSQSMessage(LoggableEvent loggableEvent) {
        super();
        this.loggableEvent = loggableEvent;
    }
}
