package gov.cms.ab2d.api.controller;

public class InMaintenanceModeException extends RuntimeException {

    public InMaintenanceModeException(String msg) {
        super(msg);
    }
}
