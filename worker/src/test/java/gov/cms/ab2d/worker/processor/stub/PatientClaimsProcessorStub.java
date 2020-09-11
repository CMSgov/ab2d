package gov.cms.ab2d.worker.processor.stub;

import gov.cms.ab2d.worker.processor.PatientClaimsProcessor;
import gov.cms.ab2d.worker.processor.domainmodel.EobSearchResult;
import gov.cms.ab2d.worker.processor.domainmodel.PatientClaimsRequest;
import org.springframework.scheduling.annotation.AsyncResult;

import java.util.ArrayList;
import java.util.concurrent.Future;

public class PatientClaimsProcessorStub implements PatientClaimsProcessor {

    @Override
    public Future<EobSearchResult> process(PatientClaimsRequest request) {
        EobSearchResult result = new EobSearchResult();
        result.setEobs(new ArrayList<>());
        result.setJobId(request.getJob());
        result.setContractNum(request.getContractNum());
        return new AsyncResult<>(result);
    }
}