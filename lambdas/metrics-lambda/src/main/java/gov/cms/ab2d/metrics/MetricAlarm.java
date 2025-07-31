package gov.cms.ab2d.metrics;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class MetricAlarm {
    @JsonProperty("AlarmName")
    private String alarmName;
    @JsonProperty("AlarmDescription")
    private String alarmDescription;
    @JsonProperty("AWSAccountId")
    private String awsAccountId;
    @JsonProperty("AlarmConfigurationUpdatedTimestamp")
    private String alarmConfigurationUpdatedTimestamp;
    @JsonProperty("NewStateValue")
    private String newStateValue;
    @JsonProperty("NewStateReason")
    private String newStateReason;
    @JsonProperty("StateChangeTime")
    private String stateChangeTime;
    @JsonProperty("Region")
    private String region;
    @JsonProperty("AlarmArn")
    private String alarmArn;
    @JsonProperty("OldStateValue")
    private String oldStateValue;
    @JsonProperty("OKActions")
    private List<String> okActions;
    @JsonProperty("AlarmActions")
    private List<String> alarmActions;
    @JsonProperty("InsufficientDataActions")
    private List<Object> insufficientDataActions;
    @JsonProperty("Trigger")
    private Trigger trigger = new Trigger();
}
