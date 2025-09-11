package gov.cms.ab2d.eventclient.messages;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.*;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Data
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.WRAPPER_ARRAY
)

public abstract class SQSMessages {
    protected SQSMessages() { }
}

