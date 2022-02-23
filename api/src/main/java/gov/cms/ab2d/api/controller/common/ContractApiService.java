package gov.cms.ab2d.api.controller.common;

import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import static java.util.stream.Collectors.toList;

@AllArgsConstructor
@Service
@Transactional
public class ContractApiService {

    //to be replaced with FeignClient once we move to serverless
    private final ContractApiRepository contractRepository;

    public List<ContractApiDto> getAllAttestedContracts() {
        // There are about 100 contracts including test contracts so this call has minimal cost.
        // This call is only made once a day as well.
        return contractRepository.findAll()
                .stream().filter(ContractApiDto::hasAttestation).collect(toList());
    }

    public Optional<ContractApiDto> getContractByContractNumber(String contractNumber) {
        return contractRepository.findContractByContractNumber(contractNumber);
    }


    public void updateContract(ContractApiDto contract) {
        contractRepository.save(contract);
    }
}
