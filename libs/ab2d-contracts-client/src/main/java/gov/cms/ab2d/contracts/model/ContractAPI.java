package gov.cms.ab2d.contracts.model;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

public interface ContractAPI {
    @GetMapping("contracts")
    List<ContractDTO> getContracts(@RequestParam(value = "contractId", required = false) Long contractId);

    @PutMapping("contracts")
    void updateContract(@RequestBody ContractDTO contract);

    @GetMapping("contracts/{contractNumber}")
    ContractDTO getContractByNumber(@PathVariable("contractNumber") String contractNumber);
}

