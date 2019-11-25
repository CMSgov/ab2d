package gov.cms.ab2d.api.security;

public class UserNotEnabledException extends RuntimeException {

    public UserNotEnabledException(String msg) {
        super(msg);
    }
}
