package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.worker.model.ContractWorkerDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ContractWorkerClient {

    private final ContractWorkerService contractWorkerService;

    public ContractWorkerClient(ContractWorkerService contractWorkerService) {
        this.contractWorkerService = contractWorkerService;
    }

    public ContractWorkerDto getContractByContractNumber(String contractNumber) {
        return contractWorkerService.getContractByContractNumber(contractNumber);
    }
}
