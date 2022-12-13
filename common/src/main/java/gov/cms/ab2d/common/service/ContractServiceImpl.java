package gov.cms.ab2d.common.service;

import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.common.repository.ContractRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@AllArgsConstructor
@Service
@Transactional
public class ContractServiceImpl implements ContractService {

    private final ContractRepository contractRepository;

    public List<Contract> getAllAttestedContracts() {
        // There are about 100 contracts including test contracts so this call has minimal cost.
        // This call is only made once a day as well.
        return contractRepository.findAll()
                .stream().filter(Contract::hasAttestation).collect(toList());
    }

    public Optional<Contract> getContractByContractNumber(String contractNumber) {
        return contractRepository.findContractByContractNumber(contractNumber);
    }

    @Override
    public void updateContract(Contract contract) {
        contractRepository.save(contract);
    }

    @Override
    public Contract getContractByContractId(Long contractId) {
        Optional<Contract> optionalContract = contractRepository.findById(contractId);
        return optionalContract.orElse(null);
    }

}
