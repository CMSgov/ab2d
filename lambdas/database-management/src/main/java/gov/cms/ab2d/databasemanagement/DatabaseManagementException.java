package gov.cms.ab2d.databasemanagement;

public class DatabaseManagementException extends RuntimeException {
    public DatabaseManagementException(String message, Exception exception) {
        super(message, exception);
    }
}
