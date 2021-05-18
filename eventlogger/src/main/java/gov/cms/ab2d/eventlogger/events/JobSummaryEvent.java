package gov.cms.ab2d.eventlogger.events;

import gov.cms.ab2d.eventlogger.LoggableEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/**
 * Class to create and log an API request coming from a user
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class JobSummaryEvent extends LoggableEvent {
    private OffsetDateTime submittedTime;
    private OffsetDateTime inProgressTime;
    private OffsetDateTime successfulTime;
    private OffsetDateTime cancelledTime;
    private OffsetDateTime failedTime;
    private int numFilesCreated;
    private int numFilesDeleted;
    private int numFilesDownloaded;
    private int totalNum;
    private int successfullySearched;
    private int numOptedOut;
    private int errorSearched;

    public JobSummaryEvent() { }

    @Override
    public String asMessage() {
        return String.format("(%s) submitted at %s successfully searched %d", getJobId(), submittedTime, successfullySearched);
    }
}
