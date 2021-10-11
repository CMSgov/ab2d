package gov.cms.ab2d.worker.processor.coverage;

import java.util.List;

public class CoverageVerificationException extends RuntimeException {

    private final List<String> issues;

    public CoverageVerificationException(String message, List<String> issues) {
        super(message);

        this.issues = issues;
    }

    public CoverageVerificationException(String message, Throwable cause, List<String> issues) {
        super(message, cause);

        this.issues = issues;
    }

    public String getAlertMessage() {
        return String.join(",\n\t", issues);
    }
}
