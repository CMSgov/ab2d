package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.worker.processor.domainmodel.EobSearchResult;
import gov.cms.ab2d.worker.processor.domainmodel.PatientClaimsRequest;

import java.util.concurrent.Future;

public interface PatientClaimsProcessor {
    Future<EobSearchResult> process(PatientClaimsRequest request);
}
