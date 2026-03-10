package gov.cms.ab2d.api.security;

public class EndpointNotAvailableException extends RuntimeException {

    public EndpointNotAvailableException(String msg) {
        super(msg);
    }
}
