package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.Contract;

import java.util.Optional;

public interface ContractService {

    Optional<Contract> getContractByContractNumber(String contractNumber);
}
