package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.worker.model.ContractWorkerDto;

public interface ContractWorkerService {
    ContractWorkerDto getContractByContractNumber(String contractNumber);
}
