package gov.cms.ab2d.eventclient.jackson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import gov.cms.ab2d.eventclient.messages.*;

public class MessagesJacksonModule extends SimpleModule {

    public MessagesJacksonModule() {
        super("MessagesJacksonModule");
        registerSubtypes(
                new NamedType(AlertSQSMessage.class, "AlertSQSMessage"),
                new NamedType(GeneralSQSMessage.class, "GeneralSQSMessage"),
                new NamedType(KinesisSQSMessage.class, "KinesisSQSMessage"),
                new NamedType(LogAndTraceSQSMessage.class, "LogAndTraceSQSMessage"),
                new NamedType(SlackSQSMessage.class, "SlackSQSMessage"),
                new NamedType(TraceAndAlertSQSMessage.class, "TraceAndAlertSQSMessage"),
                new NamedType(TraceSQSMessage.class, "TraceSQSMessage")
        );
    }

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        context.setMixInAnnotations(SQSMessages.class, SQSMessagesMixin.class);
    }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.WRAPPER_ARRAY
    )
    private static abstract class SQSMessagesMixin { }
}
