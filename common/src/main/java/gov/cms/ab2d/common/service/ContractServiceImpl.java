package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.feign.ContractFeignClient;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.contracts.model.ContractDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import static java.util.stream.Collectors.toList;

@Service
@Transactional
public class ContractServiceImpl implements ContractService {

    private final ContractRepository contractRepository;

    private final ContractFeignClient contractFeignClient;

    private final boolean contractServiceEnabled;

    public ContractServiceImpl(ContractRepository contractRepository, ContractFeignClient contractFeignClient,
                               @Value("${contract.service.enabled}") boolean contractServiceEnabled) {
        this.contractRepository = contractRepository;
        this.contractFeignClient = contractFeignClient;
        this.contractServiceEnabled = contractServiceEnabled;
    }


    public List<Contract> getAllAttestedContracts() {
        // There are about 100 contracts including test contracts so this call has minimal cost.
        // This call is only made once a day as well.
        if (contractServiceEnabled)
            return contractFeignClient.getContracts(null).stream().filter(contractDTO -> contractDTO.getAttestedOn() != null)
                    .map(this::dtoToContract).toList();

        return contractRepository.findAll()
                .stream().filter(Contract::hasAttestation).collect(toList());
    }

    public Optional<Contract> getContractByContractNumber(String contractNumber) {
        if (contractServiceEnabled)
            return Optional.of(dtoToContract(contractFeignClient.getContractByNumber(contractNumber)));

        return contractRepository.findContractByContractNumber(contractNumber);
    }

    @Override
    public void updateContract(Contract contract) {
        if (contractServiceEnabled)
            contractFeignClient.updateContract(contract.toDTO());
        else
            contractRepository.save(contract);

    }

    @Override
    public Contract getContractByContractId(Long contractId) {
        if (contractServiceEnabled) {
            return dtoToContract(contractFeignClient.getContracts(contractId).get(0));
        }
        return contractRepository.findById(contractId).orElse(null);
    }

    //TODO replace Contract with ContractDTO
    private Contract dtoToContract(ContractDTO contractDTO) {
        Contract contract = new Contract(contractDTO.getContractNumber(), contractDTO.getContractName(), null, null, null);
        contract.setAttestedOn(contractDTO.getAttestedOn());
        contract.setContractType(contractDTO.getContractType());
        contract.setId(contractDTO.getId());
        return contract;
    }

}
