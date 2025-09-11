package gov.cms.ab2d.contracts.service;

import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.contracts.model.ContractDTO;
import java.util.List;

public interface ContractService {
    void updateContract(ContractDTO contract);
    List<ContractDTO> getAllContracts();
    Contract getContractByContractId(Long contractId);
    Contract getContractByContractNumber(String contractNumber);
}
