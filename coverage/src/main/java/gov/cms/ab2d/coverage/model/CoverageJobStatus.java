package gov.cms.ab2d.coverage.model;

import lombok.Getter;

@Getter
public enum CoverageJobStatus {
    SUBMITTED,
    IN_PROGRESS,
    FAILED,
    SUCCESSFUL,
    CANCELLED;
}
