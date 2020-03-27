package gov.cms.ab2d.eventlogger;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * Class to create and log an API request sent back to the user
 */
@Data
public class ApiResponseEventLogger implements LoggableEvent {
    // id
    private Long id;
    // The user who wanted the answer
    private String user;
    // The HTTP response code
    private int responseCode;
    // The response sent - assuming not all the data
    private String responseString;
    // A description giving context to the response
    private String description;
    // The unique id that this response is a response to
    private String requestId;

    @Override
    public boolean log(OffsetDateTime eventTime) {
        return false;
    }
}
