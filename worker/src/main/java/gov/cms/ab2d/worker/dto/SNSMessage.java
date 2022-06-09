package gov.cms.ab2d.worker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SNSMessage {
    @JsonProperty("Message")
    private String message;
    @JsonProperty("Subject")
    private String subject;
}
