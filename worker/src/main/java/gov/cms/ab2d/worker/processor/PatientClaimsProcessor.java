package gov.cms.ab2d.worker.processor;

import java.util.concurrent.Future;

public interface PatientClaimsProcessor {
    Future<EobSearchResult> process(PatientClaimsRequest request);
}
