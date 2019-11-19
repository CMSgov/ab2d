package gov.cms.ab2d.api.security;

public class BadJWTTokenException extends RuntimeException {

    public BadJWTTokenException(String msg) {
        super(msg);
    }

    public BadJWTTokenException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
