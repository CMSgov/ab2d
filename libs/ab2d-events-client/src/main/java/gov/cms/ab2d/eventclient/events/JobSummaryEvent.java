package gov.cms.ab2d.eventclient.events;


import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

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

    public JobSummaryEvent() {
        // Adding a nested comment here to satisfy sonarqube code smells.
        // This method is intentionally blank for the following reason:

        // This is a constructor for a subtype of LoggableEvent.
        // We construct this type while overriding asMessage to differentiate functionality.
    }

    @Override
    public String asMessage() {
        return String.format("(%s) submitted at %s successfully searched %d", getJobId(), submittedTime, successfullySearched);
    }
}
