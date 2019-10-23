package gov.cms.ab2d.api.controller;

public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String msg) {
        super(msg);
    }
}
