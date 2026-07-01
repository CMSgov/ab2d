package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.coverage.model.CoverageSummary;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.List;
import java.util.concurrent.Future;

/**
 * Process a single {@link PatientClaimsRequest} at a time.
 */
public interface PatientClaimsProcessor {
    Future<ProgressTrackerUpdate> process(PatientClaimsRequest request);
    List<IBaseResource> getEobBundleResources(PatientClaimsRequest request, CoverageSummary patient);
}
