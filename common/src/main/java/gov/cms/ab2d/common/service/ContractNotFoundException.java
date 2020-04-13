package gov.cms.ab2d.common.service;

public class ContractNotFoundException extends RuntimeException {

    public ContractNotFoundException(String msg) {
        super(msg);
    }
}
