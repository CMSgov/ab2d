package gov.cms.ab2d.job.service;

public class InvalidJobStateTransition extends RuntimeException {

    public InvalidJobStateTransition(String msg) {
        super(msg);
    }
}
