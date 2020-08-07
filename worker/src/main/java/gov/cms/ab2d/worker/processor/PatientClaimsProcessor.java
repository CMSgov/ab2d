package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.worker.processor.domainmodel.EobSearchResponse;
import gov.cms.ab2d.worker.processor.domainmodel.PatientClaimsRequest;

import java.util.concurrent.Future;

public interface PatientClaimsProcessor {
    Future<EobSearchResponse> process(PatientClaimsRequest request);
}
