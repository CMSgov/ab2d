package gov.cms.ab2d.job.service;

public class JobOutputMissingException extends RuntimeException {

    public JobOutputMissingException(String msg) {
        super(msg);
    }
}
