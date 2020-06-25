package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.worker.processor.domainmodel.ContractMapping;

import java.util.concurrent.Future;

public interface PatientContractProcessor {
    Future<ContractMapping> process(String contractNumber, Integer month);
}
