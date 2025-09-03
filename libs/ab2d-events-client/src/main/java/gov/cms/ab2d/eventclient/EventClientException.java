package gov.cms.ab2d.eventclient;

public class EventClientException extends RuntimeException {
    public EventClientException(String message) {
        super(message);
    }

    public EventClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
