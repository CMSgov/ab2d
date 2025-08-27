package gov.cms.ab2d.contracts.controller;

public class InvalidContractException extends RuntimeException {
    public InvalidContractException(String msg) {
        super(msg);
    }
}
