package gov.cms.ab2d.optout;

public class OptOutException extends RuntimeException {
    public OptOutException(String errorMessage, Exception exception) {
        super(errorMessage, exception);
    }

    public OptOutException(String errorMessage) {
        super(errorMessage);
    }
}
