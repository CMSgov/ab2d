package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.feign.ContractFeignClient;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.contracts.model.ContractDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ContractServiceImpl implements ContractService {

    private final ContractFeignClient contractFeignClient;

    public ContractServiceImpl(ContractRepository contractRepository, ContractFeignClient contractFeignClient) {
        this.contractFeignClient = contractFeignClient;
    }


    public List<Contract> getAllAttestedContracts() {
        // There are about 100 contracts including test contracts so this call has minimal cost.
        // This call is only made once a day as well.
        return contractFeignClient.getContracts(null).stream().filter(contractDTO -> contractDTO.getAttestedOn() != null)
                    .map(this::dtoToContract).toList();

    }

    public Optional<Contract> getContractByContractNumber(String contractNumber) {
        return Optional.of(dtoToContract(contractFeignClient.getContractByNumber(contractNumber)));

    }

    @Override
    public void updateContract(Contract contract) {
        contractFeignClient.updateContract(contract.toDTO());
    }

    @Override
    public Contract getContractByContractId(Long contractId) {
        return dtoToContract(contractFeignClient.getContracts(contractId).get(0));

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
