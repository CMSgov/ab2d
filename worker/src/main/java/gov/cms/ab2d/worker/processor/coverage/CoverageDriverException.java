package gov.cms.ab2d.worker.processor.coverage;

public class CoverageDriverException extends RuntimeException {

    public CoverageDriverException(String message) {
        super(message);
    }

    public CoverageDriverException(String message, Throwable cause) {
        super(message, cause);
    }
}
