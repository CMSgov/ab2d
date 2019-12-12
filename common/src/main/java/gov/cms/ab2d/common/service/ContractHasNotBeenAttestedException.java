package gov.cms.ab2d.common.service;

public class ContractHasNotBeenAttestedException extends RuntimeException {

    public ContractHasNotBeenAttestedException(String msg) {
        super(msg);
    }
}
