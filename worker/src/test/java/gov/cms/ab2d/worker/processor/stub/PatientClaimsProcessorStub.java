package gov.cms.ab2d.worker.processor.stub;

import gov.cms.ab2d.coverage.model.CoverageSummary;
import gov.cms.ab2d.worker.processor.PatientClaimsProcessor;
import gov.cms.ab2d.worker.processor.PatientClaimsRequest;
import gov.cms.ab2d.worker.processor.ProgressTrackerUpdate;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.scheduling.annotation.AsyncResult;

import java.util.List;
import java.util.concurrent.Future;

public class PatientClaimsProcessorStub implements PatientClaimsProcessor {

    @Override
    public Future<ProgressTrackerUpdate> process(PatientClaimsRequest request) {
        ProgressTrackerUpdate update = new ProgressTrackerUpdate();

        update.addEobFetchedCount(1);
        update.incPatientProcessCount();
        update.incPatientsWithEobsCount();
        update.addEobProcessedCount(1);
        return AsyncResult.forValue(update);
    }

    @Override
    public List<IBaseResource> getEobBundleResources(PatientClaimsRequest request, CoverageSummary patient){
        return null;
    }
}