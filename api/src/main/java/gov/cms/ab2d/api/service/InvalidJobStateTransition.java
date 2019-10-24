package gov.cms.ab2d.api.service;

public class InvalidJobStateTransition extends RuntimeException {

    public InvalidJobStateTransition(String msg) {
        super(msg);
    }
}
