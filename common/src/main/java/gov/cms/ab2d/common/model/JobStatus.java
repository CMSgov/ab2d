package gov.cms.ab2d.common.model;

import lombok.Getter;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Getter
public enum JobStatus {

    SUBMITTED(true, false) {
        @Override
        public boolean isExpired(OffsetDateTime completedAt, int ttl) {
            return false;
        }
    },
    IN_PROGRESS(true, false) {
        @Override
        public boolean isExpired(OffsetDateTime completedAt, int ttl) {
            return false;
        }
    },
    FAILED(false, true) {
        @Override
        public boolean isExpired(OffsetDateTime completedAt, int ttl) {
            return true;
        }
    },
    SUCCESSFUL(false, true) {
        @Override
        public boolean isExpired(OffsetDateTime completedAt, int ttl) {
            // This really should be an assert as if a job is successful, it should have a completion timestamp.
            if (completedAt == null) {
                return false;
            }
            Instant deleteBoundary = Instant.now().minus(ttl, ChronoUnit.HOURS);
            return completedAt.toInstant().isBefore(deleteBoundary);
        }
    },
    CANCELLED(false, true) {
        @Override
        public boolean isExpired(OffsetDateTime completedAt, int ttl) {
            return true;
        }
    };

    private final boolean isCancellable;

    private final boolean isFinished;

    JobStatus(boolean isCancellable, boolean isFinished) {
        this.isCancellable = isCancellable;
        this.isFinished = isFinished;
    }

    public abstract boolean isExpired(OffsetDateTime completedAt, int ttl);
}
