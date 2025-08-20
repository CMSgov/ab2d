
package gov.cms.ab2d.metrics;

public class MetricsLambdaException extends RuntimeException {
    public MetricsLambdaException(String message) {
        super(message);
    }

    public MetricsLambdaException(String message, Throwable cause) {
        super(message, cause);
    }
}
