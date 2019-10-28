package gov.cms.ab2d.common.service;

public class InvalidJobStateTransition extends RuntimeException {

    public InvalidJobStateTransition(String msg) {
        super(msg);
    }
}
