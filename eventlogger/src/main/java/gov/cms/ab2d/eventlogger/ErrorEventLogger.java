package gov.cms.ab2d.eventlogger;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * Logs exceptions that occur. I assume this logger will increase the most over time
 */
@Data
@AllArgsConstructor
public class ErrorEventLogger implements LoggableEvent {
    public enum ErrorType {
        FILE_ALREADY_DELETED,
        INVALID_CONTRACT,
        UNAUTHORIZED_CONTRACT,
        TOO_MANY_STATUS_REQUESTS,
        TOO_MANY_SEARCH_ERRORS
    }
    // id
    private Long id;
    // The type of error we're reporting
    private ErrorType errorType;
    // The user name associated with this error (if there is any)
    private String user;
    // The IP address associated with this error (if there is any)
    private String ipAddress;
    // The job id associated with this error (if there is any)
    private String jobId;
    // A description of the error
    private String description;

    @Override
    public boolean log(OffsetDateTime eventTime) {
        return false;
    }
}
