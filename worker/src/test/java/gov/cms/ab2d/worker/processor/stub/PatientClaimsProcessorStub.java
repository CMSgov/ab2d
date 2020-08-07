package gov.cms.ab2d.worker.processor.stub;

import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneficiaries;
import gov.cms.ab2d.worker.processor.PatientClaimsProcessor;
import gov.cms.ab2d.worker.processor.domainmodel.EobSearchResponse;
import gov.cms.ab2d.worker.processor.domainmodel.PatientClaimsRequest;
import org.springframework.scheduling.annotation.AsyncResult;

import java.util.ArrayList;
import java.util.concurrent.Future;

public class PatientClaimsProcessorStub implements PatientClaimsProcessor {

    @Override
    public Future<EobSearchResponse> process(PatientClaimsRequest request) {
        return new AsyncResult<>(new EobSearchResponse(request.getPatientDTO(), new ArrayList<>()));
    }
}