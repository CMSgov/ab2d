package gov.cms.ab2d.worker.service;

public class JobCancelledException extends RuntimeException {

    public JobCancelledException(String message) {
        super(message);
    }
}
