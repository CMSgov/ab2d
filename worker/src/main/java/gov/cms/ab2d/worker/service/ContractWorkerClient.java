package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.common.dto.ContractDTO;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.service.ContractService;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ContractWorkerClient {

    private final ContractService contractService;

    public ContractWorkerClient(ContractService contractService) {
        this.contractService = contractService;
    }

    public ContractDTO getContractByContractNumber(String contractNumber) {
        Optional<Contract> contractOptional = contractService.getContractByContractNumber(contractNumber);
        if (contractOptional.isPresent()) {
            Contract contract = contractOptional.get();
            return new ContractDTO(contract.getContractNumber(), contract.getContractName(),
                    contract.getAttestedOn().toString(), contract.getContractType());
        }
        return null;
    }
}
