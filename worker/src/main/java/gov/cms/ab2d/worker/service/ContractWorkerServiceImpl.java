package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.worker.model.ContractWorker;
import gov.cms.ab2d.worker.repository.ContractWorkerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ContractWorkerServiceImpl implements ContractWorkerService {

    //to be replaced with FeignClient once we move to serverless
    private final ContractWorkerRepository contractRepository;

    public ContractWorkerServiceImpl(ContractWorkerRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    @Override
    public ContractWorker getContractByContractNumber(String contractNumber) {
        return contractRepository.findContractByContractNumber(contractNumber);
    }
}
