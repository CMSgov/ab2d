package gov.cms.ab2d.eventlogger.events;

import gov.cms.ab2d.eventlogger.LoggableEvent;
import lombok.Data;

/**
 * Class to create and log an API request sent back to the user
 */
@Data
public class ApiResponseEvent extends LoggableEvent {
    // The HTTP response code
    private int responseCode;
    // The response sent - assuming not all the data
    private String responseString;
    // A description giving context to the response
    private String description;
    // The unique id that this response is a response to
    private String requestId;
}
