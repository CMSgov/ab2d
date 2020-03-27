package gov.cms.ab2d.eventlogger.events;

import gov.cms.ab2d.eventlogger.LoggableEvent;
import lombok.Data;

/**
 * Class to create and log an API request coming from a user
 */
@Data
public class ApiRequestEvent extends LoggableEvent {
    // The URL requested including request parameters
    private String url;
    // The IP address of the user doing the API request
    private String ipAddress;
    // A hash of the token used. We won't be able to re-read it, but we'll be able to tell if they are the same
    private String tokenHash;
    // The unique id of this request (to pair with the response)
    private String requestId;
}
