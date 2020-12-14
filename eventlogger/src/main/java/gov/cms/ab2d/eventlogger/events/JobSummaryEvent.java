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

    public JobSummaryEvent clone() {
        JobSummaryEvent event = (JobSummaryEvent) super.clone();
        event.setSubmittedTime(this.getSubmittedTime());
        event.setInProgressTime(this.getInProgressTime());
        event.setSuccessfulTime(this.getSuccessfulTime());
        event.setCancelledTime(this.getCancelledTime());
        event.setFailedTime(this.getFailedTime());
        event.setNumFilesCreated(this.getNumFilesCreated());
        event.setNumFilesDeleted(this.getNumFilesDeleted());
        event.setNumFilesDownloaded(this.getNumFilesDownloaded());
        event.setTotalNum(this.getTotalNum());
        event.setSuccessfullySearched(this.getSuccessfullySearched());
        event.setNumOptedOut(this.getNumOptedOut());
        event.setErrorSearched(this.getErrorSearched());
        return event;
    }
}
