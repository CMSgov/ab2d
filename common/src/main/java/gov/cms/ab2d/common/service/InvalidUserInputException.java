package gov.cms.ab2d.common.service;

public class InvalidUserInputException extends RuntimeException {

    public InvalidUserInputException(String msg) {
        super(msg);
    }
}
