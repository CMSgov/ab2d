package gov.cms.ab2d.api.controller;

public class InvalidUserInputException extends RuntimeException {

    public InvalidUserInputException(String msg) {
        super(msg);
    }
}
