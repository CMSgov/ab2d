package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.repository.ContractRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Service
@Transactional
public class ContractServiceImpl implements ContractService {

    @Autowired
    private ContractRepository contractRepository;

    public List<Contract> getAllAttestedContracts() {
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
}
