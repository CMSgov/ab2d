package gov.cms.ab2d.metrics;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Trigger {
    @JsonProperty("Dimensions")
    private Dimensions[] dimensions;
    @JsonProperty("MetricName")
    private String metricName;
    @JsonProperty("Namespace")
    private String namespace;
    @JsonProperty("StatisticType")
    private String statisticType;
    @JsonProperty("Statistic")
    private String statistic;
    @JsonProperty("Unit")
    private String unit;
    @JsonProperty("Period")
    private int period;
    @JsonProperty("EvaluationPeriods")
    private String evaluationPeriods;
    @JsonProperty("ComparisonOperator")
    private String comparisonOperator;
    @JsonProperty("Threshold")
    private int threshold;
    @JsonProperty("TreatMissingData")
    private String treatMissingData;
    @JsonProperty("EvaluateLowSampleCountPercentile")
    private String evaluateLowSampleCountPercentile;

}
