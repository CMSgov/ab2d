package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneficiaries;
import gov.cms.ab2d.worker.processor.domainmodel.PatientClaimsRequest;

import java.util.Map;
import java.util.concurrent.Future;

public interface PatientClaimsProcessor {
    Future<Void> process(PatientClaimsRequest request, Map<String, ContractBeneficiaries.PatientDTO> map);
}
