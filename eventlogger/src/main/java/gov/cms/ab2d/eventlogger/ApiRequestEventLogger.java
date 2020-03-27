package gov.cms.ab2d.eventlogger;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * Class to create and log an API request coming from a user
 */
@Data
public class ApiRequestEventLogger implements LoggableEvent {
    // id
    private Long id;
    // The user doing the API request
    private String user;
    // The URL requested including request parameters
    private String url;
    // The IP address of the user doing the API request
    private String ipAddress;
    // A hash of the token used. We won't be able to re-read it, but we'll be able to tell if they are the same
    private String tokenHash;
    // The unique id of this request (to pair with the response)
    private String requestId;

    @Override
    public boolean log(OffsetDateTime eventTime) {
        return false;
    }
}
