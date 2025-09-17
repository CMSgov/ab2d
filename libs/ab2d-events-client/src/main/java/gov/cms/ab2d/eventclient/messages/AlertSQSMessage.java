package gov.cms.ab2d.eventclient.messages;

import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import java.util.List;

import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
public class AlertSQSMessage extends SQSMessages {

    private String message;

    private List<Ab2dEnvironment> environments;

    public AlertSQSMessage() { }

    public AlertSQSMessage(String message, List<Ab2dEnvironment> environments) {
        super();
        this.message = message;
        this.environments = environments;
    }
}
