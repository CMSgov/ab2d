package gov.cms.ab2d.coverage.model;

import lombok.Getter;

@Getter
public enum JobStatus {

    SUBMITTED(true, false),
    IN_PROGRESS(true, false),
    FAILED(false, true),
    SUCCESSFUL(false, true),
    CANCELLED(false, true);

    private final boolean isCancellable;

    private final boolean isFinished;

    JobStatus(boolean isCancellable, boolean isFinished) {
        this.isCancellable = isCancellable;
        this.isFinished = isFinished;
    }
}
