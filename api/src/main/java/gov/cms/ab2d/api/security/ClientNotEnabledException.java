package gov.cms.ab2d.api.security;

public class ClientNotEnabledException extends RuntimeException {

    public ClientNotEnabledException(String msg) {
        super(msg);
    }
}
