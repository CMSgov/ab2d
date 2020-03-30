package gov.cms.ab2d.api.security;

public class TokenResponseError extends RuntimeException {

    public TokenResponseError(String msg) {
        super(msg);
    }
}
