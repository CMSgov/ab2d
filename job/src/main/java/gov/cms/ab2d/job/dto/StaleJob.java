package gov.cms.ab2d.job.dto;

import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
public class StaleJob {
    @NotNull
    String jobUuid;
    @NotNull
    String organization;
}
