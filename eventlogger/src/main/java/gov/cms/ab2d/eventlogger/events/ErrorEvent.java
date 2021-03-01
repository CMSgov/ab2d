package gov.cms.ab2d.eventlogger.events;

import gov.cms.ab2d.eventlogger.LoggableEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/**
 * Logs exceptions that occur. I assume this logger will increase the most over time
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ErrorEvent extends LoggableEvent {
    public enum ErrorType {
        FILE_ALREADY_DELETED,
        CONTRACT_NOT_FOUND,
        UNAUTHORIZED_CONTRACT,
        TOO_MANY_STATUS_REQUESTS,
        TOO_MANY_SEARCH_ERRORS
    }
    // The type of error we're reporting
    private ErrorType errorType;
    // A description of the error
    private String description;

    public ErrorEvent() { }

    public ErrorEvent(String organization, String jobId, ErrorType errorType, String description) {
        super(OffsetDateTime.now(), organization, jobId);
        this.errorType = errorType;
        this.description = description;
    }
}
