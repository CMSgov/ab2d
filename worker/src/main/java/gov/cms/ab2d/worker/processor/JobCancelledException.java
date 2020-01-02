package gov.cms.ab2d.worker.processor;

public class JobCancelledException extends RuntimeException {

    public JobCancelledException(String message) {
        super(message);
    }
}
