package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.worker.model.ContractWorkerDto;
import gov.cms.ab2d.worker.repository.ContractWorkerRepository;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import static java.util.stream.Collectors.toList;

@AllArgsConstructor
@Service
@Transactional
public class ContractWorkerService {

    //to be replaced with FeignClient once we move to serverless
    private final ContractWorkerRepository contractRepository;

    public List<ContractWorkerDto> getAllAttestedContracts() {
        // There are about 100 contracts including test contracts so this call has minimal cost.
        // This call is only made once a day as well.
        return contractRepository.findAll()
                .stream().filter(ContractWorkerDto::hasAttestation).collect(toList());
    }

    public Optional<ContractWorkerDto> getContractByContractNumber(String contractNumber) {
        return contractRepository.findContractByContractNumber(contractNumber);
    }


    public void updateContract(ContractWorkerDto contract) {
        contractRepository.save(contract);
    }
}
