package gov.cms.ab2d.eventlogger;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * Interface describing a loggable event
 */
@Data
public abstract class LoggableEvent {
    public LoggableEvent() { }

    // id
    private Long id;

    // Time the event occurred
    private OffsetDateTime timeOfEvent;

    // The user the event may be related to
    private String user;

    // The job the event may be related to
    private String jobId;

    public LoggableEvent(OffsetDateTime timeOfEvent, String user, String jobId) {
        this.timeOfEvent = timeOfEvent;
        this.user = user;
        this.jobId = jobId;
    }
}
