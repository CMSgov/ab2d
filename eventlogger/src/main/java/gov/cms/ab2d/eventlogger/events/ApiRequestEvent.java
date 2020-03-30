package gov.cms.ab2d.eventlogger.events;

import gov.cms.ab2d.eventlogger.LoggableEvent;
import lombok.Data;

import java.time.OffsetDateTime;

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

    public ApiRequestEvent(String user, String jobId, String url, String ipAddress, String tokenHash,
                            String requestId) {
        super(OffsetDateTime.now(), user, jobId);
        this.url = url;
        this.ipAddress = ipAddress;
        this.tokenHash = tokenHash;
        this.requestId = requestId;
    }
}
