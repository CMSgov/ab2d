package gov.cms.ab2d.audit;

public class AuditException extends RuntimeException {
    public AuditException(String error) {
        super(error);
    }

    public AuditException(Exception exception) {
        super(exception);
    }

    public AuditException(String error, Exception exception) {
        super(error, exception);
    }
}
