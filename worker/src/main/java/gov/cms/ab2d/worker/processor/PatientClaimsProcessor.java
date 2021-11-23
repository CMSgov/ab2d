package gov.cms.ab2d.worker.processor;

import java.util.concurrent.Future;

/**
 * Process a single {@link PatientClaimsRequest} at a time.
 */
public interface PatientClaimsProcessor {
    Future<EobSearchResult> process(PatientClaimsRequest request);
}
