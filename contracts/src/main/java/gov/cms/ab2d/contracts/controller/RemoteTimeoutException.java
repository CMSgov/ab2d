package gov.cms.ab2d.contracts.controller;

public class RemoteTimeoutException extends RuntimeException {
    public RemoteTimeoutException(String msg) {
        super(msg);
    }
}