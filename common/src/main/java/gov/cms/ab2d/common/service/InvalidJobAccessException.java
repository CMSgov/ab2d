package gov.cms.ab2d.common.service;

public class InvalidJobAccessException extends RuntimeException {

    public InvalidJobAccessException(String msg) {
        super(msg);
    }
}
