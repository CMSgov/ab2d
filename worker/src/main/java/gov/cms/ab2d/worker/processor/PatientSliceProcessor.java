package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse;
import gov.cms.ab2d.worker.processor.domainmodel.ContractData;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface PatientSliceProcessor {

    CompletableFuture<Void> process(Map.Entry<Integer, List<GetPatientsByContractResponse.PatientDTO>> slice, ContractData contractData);
}
