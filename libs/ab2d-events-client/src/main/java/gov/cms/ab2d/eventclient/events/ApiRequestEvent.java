package gov.cms.ab2d.eventclient.events;



import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * Class to create and log an API request coming from a user
 */
@EqualsAndHashCode(callSuper = true)
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

    public ApiRequestEvent() { }

    public ApiRequestEvent(String organization, String jobId, String url, String ipAddress, String token,
                           String requestId) {
        super(OffsetDateTime.now(), organization, jobId);
        this.url = url;
        this.ipAddress = ipAddress;
        if (token != null) {
            this.tokenHash = hashIt(token);
        }
        this.requestId = requestId;
    }

    @Override
    public String asMessage() {
        return String.format("request to %s from %s", url, ipAddress);
    }

    public static String hashIt(String val) {
        if (val == null) {
            return null;
        }
        return Hex.encodeHexString(DigestUtils.sha256(val));
    }
}
