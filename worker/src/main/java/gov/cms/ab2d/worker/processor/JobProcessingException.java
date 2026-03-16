package gov.cms.ab2d.worker.processor;

public class JobProcessingException extends RuntimeException {
    public JobProcessingException(String message) {
        super(message);
    }

    public JobProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
