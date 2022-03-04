package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.worker.model.ContractWorker;

public interface ContractWorkerService {
    ContractWorker getContractByContractNumber(String contractNumber);
}
