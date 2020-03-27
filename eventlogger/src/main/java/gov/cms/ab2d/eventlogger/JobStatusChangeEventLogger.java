package gov.cms.ab2d.eventlogger;

import gov.cms.ab2d.common.model.JobStatus;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * Describes the transition of a job between two statuses
 */
@Data
public class JobStatusChangeEventLogger implements LoggableEvent {
    // id
    private Long id;
    // The job id associated with the change
    private String jobId;
    // The old state
    private JobStatus oldStatus;
    // The new state
    private JobStatus newStatus;
    // A description if needed whey this state occurred if it isn't obvious
    private String description;

    @Override
    public boolean log(OffsetDateTime eventTime) {
        return false;
    }
}
