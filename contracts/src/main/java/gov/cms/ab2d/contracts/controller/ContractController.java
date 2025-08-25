package gov.cms.ab2d.contracts.controller;

import gov.cms.ab2d.contracts.model.ContractAPI;
import gov.cms.ab2d.contracts.model.ContractDTO;
import gov.cms.ab2d.contracts.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


import static gov.cms.ab2d.contracts.config.SwaggerConstants.ALL_CONTRACTS;
import static gov.cms.ab2d.contracts.config.SwaggerConstants.CONTRACTS_MAIN;
import static gov.cms.ab2d.contracts.config.SwaggerConstants.GET_CONTRACT_BY_NUMBER;
import static gov.cms.ab2d.contracts.config.SwaggerConstants.UPDATE_CONTRACTS;


@RestController
@Tag(name = "Contracts", description = CONTRACTS_MAIN)
@AllArgsConstructor
public class ContractController implements ContractAPI {
    private final ContractService contractService;

    @Override
    @Operation(summary = ALL_CONTRACTS)
    public List<ContractDTO> getContracts(Long contractId) {
        if(contractId != null) {
            ArrayList<ContractDTO> contracts = new ArrayList<>();
            contracts.add(contractService.getContractByContractId(contractId).toDTO());
            return contracts;
        }
        return contractService.getAllContracts();
    }

    @Override
    @Operation(summary = UPDATE_CONTRACTS)
    public void updateContract(@RequestBody ContractDTO contract) {
        contractService.updateContract(contract);
    }

    @Override
    @Operation(summary = GET_CONTRACT_BY_NUMBER)
    public ContractDTO getContractByNumber(String contractNumber) {
        if (contractNumber != null) {
            return contractService.getContractByContractNumber(contractNumber).toDTO();
        }
        throw new InvalidContractParamException("Must supply contract information");

    }
}
