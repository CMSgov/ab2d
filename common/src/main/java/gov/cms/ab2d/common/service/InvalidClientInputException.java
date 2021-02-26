package gov.cms.ab2d.common.service;

public class InvalidClientInputException extends RuntimeException {

    public InvalidClientInputException(String msg) {
        super(msg);
    }
}
