package gov.cms.ab2d.lambdalibs.exceptions;

public class PropertiesException extends RuntimeException {

    public PropertiesException(String error) {
        super(error);
    }

    public PropertiesException(Exception error) {
        super(error);
    }
}
