package gov.cms.ab2d.eventclient.messages;

import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import java.util.List;

import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
public class TraceSQSMessage extends SQSMessages {

    private String message;

    private List<Ab2dEnvironment> environments;

    public TraceSQSMessage() { }

    public TraceSQSMessage(String message, List<Ab2dEnvironment> environments) {
        super();
        this.message = message;
        this.environments = environments;
    }
}
