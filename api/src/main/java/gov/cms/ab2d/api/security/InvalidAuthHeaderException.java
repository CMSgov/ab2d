package gov.cms.ab2d.api.security;

public class InvalidAuthHeaderException extends RuntimeException {

    public InvalidAuthHeaderException(String msg) {
        super(msg);
    }
}
