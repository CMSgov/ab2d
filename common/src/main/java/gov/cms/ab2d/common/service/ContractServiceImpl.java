package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.repository.ContractRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class ContractServiceImpl implements ContractService {

    @Autowired
    private ContractRepository contractRepository;

    public Optional<Contract> getContractByContractNumber(String contractNumber) {
        return contractRepository.findContractByContractNumber(contractNumber);
    }
}
