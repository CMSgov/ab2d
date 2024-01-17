package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.feign.ContractFeignClient;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.contracts.model.ContractDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
@Transactional
public class ContractServiceImpl implements ContractService {

    private final ContractFeignClient contractFeignClient;

    public ContractServiceImpl(ContractFeignClient contractFeignClient) {
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
        log.info("ContractServiceImpl-dtoToContract: Retrieving contract information...");
        log.info("ContractServiceImpl-dtoToContract: contractDTO = " + contractDTO.toString());

        Contract contract = new Contract(contractDTO.getContractNumber(), contractDTO.getContractName(),
                null, null, null,
                contractDTO.getTotalEnrollment(), contractDTO.getMedicareEligible());
        log.info("ContractServiceImpl-dtoToContract: Contract created from DTO.");
        log.info("ContractServiceImpl-dtoToContract: Setting attestation date...");
        log.info("ContractServiceImpl-dtoToContract: attestedOn = " + contractDTO.getAttestedOn().toString());
        contract.setAttestedOn(contractDTO.getAttestedOn());

        log.info("ContractServiceImpl-dtoToContract: Setting contract type...");
        log.info("ContractServiceImpl-dtoToContract: contractType = " + contractDTO.getContractType().toString());
        contract.setContractType(contractDTO.getContractType());

        log.info("ContractServiceImpl-dtoToContract: Setting ID...");
        log.info("ContractServiceImpl-dtoToContract: ID = " + Long.toString(contractDTO.getId()));
        contract.setId(contractDTO.getId());

        log.info("ContractServiceImpl-dtoToContract: contract = " + contract.toString());
        log.info("ContractServiceImpl-dtoToContract: Returning contract...");
        return contract;
    }
}
