package gov.cms.ab2d.api.security;

public class MissingTokenException extends RuntimeException {

    public MissingTokenException(String msg) {
        super(msg);
    }
}
