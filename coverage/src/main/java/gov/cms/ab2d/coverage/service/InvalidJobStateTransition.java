package gov.cms.ab2d.coverage.service;

public class InvalidJobStateTransition extends RuntimeException {

    public InvalidJobStateTransition(String msg) {
        super(msg);
    }
}
