package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.contracts.model.ContractDTO;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.common.service.ContractService;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ContractWorkerClient {

    private final ContractService contractService;

    @Autowired
    public ContractWorkerClient(ContractService contractService) {
        this.contractService = contractService;
    }

    public ContractDTO getContractByContractNumber(String contractNumber) {
        Optional<Contract> contractOptional = contractService.getContractByContractNumber(contractNumber);
        return contractOptional.map(Contract::toDTO).orElse(null);
    }
}
