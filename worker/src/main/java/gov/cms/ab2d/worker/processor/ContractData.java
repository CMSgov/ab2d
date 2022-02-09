package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.fhir.FhirVersion;
import gov.cms.ab2d.worker.model.ContractWorkerDto;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class ContractData {

    private final ContractWorkerDto contract;
    private final Job job;
    private final StreamHelper streamHelper;
    private final List<Future<EobSearchResult>> eobRequestHandles = new LinkedList<>();

    public void addEobRequestHandle(Future<EobSearchResult> eobRequestHandle) {
        eobRequestHandles.add(eobRequestHandle);
    }

    public boolean remainingRequestHandles() {
        return !eobRequestHandles.isEmpty();
    }

    public FhirVersion getFhirVersion() {
        return job.getFhirVersion();
    }
}
