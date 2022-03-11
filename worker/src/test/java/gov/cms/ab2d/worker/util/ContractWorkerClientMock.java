package gov.cms.ab2d.worker.util;

import gov.cms.ab2d.common.dto.ContractDTO;
import gov.cms.ab2d.common.model.Contract;
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
        return new ContractDTO(contractNumber, contractNumber, OffsetDateTime.now(), Contract.ContractType.NORMAL);
    }
}
