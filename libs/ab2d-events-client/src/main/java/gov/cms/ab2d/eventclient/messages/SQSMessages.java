package gov.cms.ab2d.eventclient.messages;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.*;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Data

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.WRAPPER_ARRAY
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AlertSQSMessage.class, name = "AlertSQSMessage"),
        @JsonSubTypes.Type(value = GeneralSQSMessage.class, name = "GeneralSQSMessage"),
        @JsonSubTypes.Type(value = KinesisSQSMessage.class, name = "KinesisSQSMessage"),
        @JsonSubTypes.Type(value = LogAndTraceSQSMessage.class, name = "LogAndTraceSQSMessage"),
        @JsonSubTypes.Type(value = SlackSQSMessage.class, name = "SlackSQSMessage"),
        @JsonSubTypes.Type(value = TraceAndAlertSQSMessage.class, name = "TraceAndAlertSQSMessage"),
        @JsonSubTypes.Type(value = TraceSQSMessage.class, name = "TraceSQSMessage")
})
public abstract class SQSMessages {
    protected SQSMessages() { }
}

