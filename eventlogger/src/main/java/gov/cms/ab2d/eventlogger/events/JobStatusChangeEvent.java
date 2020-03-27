package gov.cms.ab2d.eventlogger.events;

import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import lombok.Data;

/**
 * Describes the transition of a job between two statuses
 */
@Data
public class JobStatusChangeEvent extends LoggableEvent {
    // The old state
    private JobStatus oldStatus;
    // The new state
    private JobStatus newStatus;
    // A description if needed whey this state occurred if it isn't obvious
    private String description;
}
