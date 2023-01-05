package gov.cms.ab2d.worker.util;

import gov.cms.ab2d.contracts.model.ContractDTO;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.worker.service.ContractWorkerClient;
import java.time.OffsetDateTime;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class ContractWorkerClientMock extends ContractWorkerClient {

    public ContractWorkerClientMock() {
        super(null);
    }


    public ContractDTO getContractByContractNumber(String contractNumber) {
        return new ContractDTO(null, contractNumber, contractNumber, OffsetDateTime.now(), Contract.ContractType.NORMAL);
    }
}
