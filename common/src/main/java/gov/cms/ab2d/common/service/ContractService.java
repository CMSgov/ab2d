package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.Contract;

import java.util.List;
import java.util.Optional;

public interface ContractService {

    List<Contract> getAllAttestedContracts();

    Optional<Contract> getContractByContractNumber(String contractNumber);

    void updateContract(Contract contract);

    Contract getContractByContractId(Long contractId);
}
