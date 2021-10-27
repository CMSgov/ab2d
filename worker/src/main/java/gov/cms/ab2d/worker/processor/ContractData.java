package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.fhir.FhirVersion;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

@RequiredArgsConstructor
@Getter
public class ContractData {

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
