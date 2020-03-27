package gov.cms.ab2d.eventlogger.events;

import gov.cms.ab2d.eventlogger.LoggableEvent;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Logs exceptions that occur. I assume this logger will increase the most over time
 */
@Data
@AllArgsConstructor
public class ErrorEvent extends LoggableEvent {
    public enum ErrorType {
        FILE_ALREADY_DELETED,
        INVALID_CONTRACT,
        UNAUTHORIZED_CONTRACT,
        TOO_MANY_STATUS_REQUESTS,
        TOO_MANY_SEARCH_ERRORS
    }
    // The type of error we're reporting
    private ErrorType errorType;
    // A description of the error
    private String description;
}
