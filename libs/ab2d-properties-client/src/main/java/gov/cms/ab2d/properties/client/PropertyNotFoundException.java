package gov.cms.ab2d.properties.client;

public class PropertyNotFoundException extends RuntimeException {
    public PropertyNotFoundException(String message) {
        super(message);
    }

    public PropertyNotFoundException(String message, Throwable ex) {
        super(message, ex);
    }
}
