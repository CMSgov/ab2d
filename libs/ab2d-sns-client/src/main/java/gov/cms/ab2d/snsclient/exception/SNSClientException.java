package gov.cms.ab2d.snsclient.exception;

public class SNSClientException extends RuntimeException {
    public SNSClientException(String message) {
        super(message);
    }

    public SNSClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
