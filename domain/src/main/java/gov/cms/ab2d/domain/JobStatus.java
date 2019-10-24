package gov.cms.ab2d.domain;

import lombok.Getter;

@Getter
public enum JobStatus {

    SUBMITTED(true),
    IN_PROGRESS(true),
    FAILED(false),
    SUCCESSFUL(false),
    CANCELLED(false);

    private final boolean isCancellable;

    JobStatus(boolean isCancellable) {
        this.isCancellable = isCancellable;
    }
}
