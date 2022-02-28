package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.worker.model.ContractWorkerDto;
import gov.cms.ab2d.worker.repository.ContractWorkerRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ContractWorkerService {

    //to be replaced with FeignClient once we move to serverless
    private final ContractWorkerRepository contractRepository;

    public ContractWorkerService(ContractWorkerRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    public Optional<ContractWorkerDto> getContractByContractNumber(String contractNumber) {
        return contractRepository.findContractByContractNumber(contractNumber);
    }
}
