package gov.cms.ab2d.contracts.service;

import gov.cms.ab2d.contracts.controller.InvalidContractException;
import gov.cms.ab2d.contracts.controller.InvalidContractParamException;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.contracts.model.ContractDTO;
import gov.cms.ab2d.contracts.repository.ContractRepository;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import static java.util.stream.Collectors.toList;

@Service
@AllArgsConstructor
@Slf4j
public class ContractServiceImpl implements ContractService {
    private final ContractRepository contractRepository;

    @Override
    public void updateContract(ContractDTO contractDTO) {
        Optional<Contract> optionalContract = contractRepository.findById(contractDTO.getId());
        if (optionalContract.isPresent()) {
            Contract contract = dtoToContract(contractDTO, optionalContract.get());
            contractRepository.save(contract);
        }
        else {
            throw new InvalidContractException("Contract Given is Invalid");
        }
    }

    @Override
    public List<ContractDTO> getAllContracts() {
        return contractRepository.findAll()
                .stream().filter(Contract::hasAttestation).map(Contract::toDTO).collect(toList());
    }

    @Override
    public Contract getContractByContractId(Long contractId) {
        Optional<Contract> contract = contractRepository.findById(contractId);
        if (contract.isPresent()) {
            return contract.get();
        }
        throw new InvalidContractException("Invalid Contract Given");
    }

    @Override
    public Contract getContractByContractNumber(String contractNumber) {
        Optional<Contract> contract = contractRepository.findContractByContractNumber(contractNumber);

        if (contract.isPresent()) {
            return contract.get();
        }
        throw new InvalidContractException("Invalid Contract Given");
    }

    private static Contract dtoToContract(ContractDTO contractDTO, Contract contract) {
        contract.setContractName(contractDTO.getContractName());
        contract.setContractType(contractDTO.getContractType());
        contract.setAttestedOn(contractDTO.getAttestedOn());
        return contract;
    }

}
