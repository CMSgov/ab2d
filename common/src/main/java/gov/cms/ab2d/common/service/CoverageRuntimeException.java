package gov.cms.ab2d.common.service;

public class CoverageRuntimeException extends RuntimeException {

    public CoverageRuntimeException(String message) {
        super(message);
    }

    public CoverageRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
