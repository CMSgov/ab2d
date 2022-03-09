package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.dto.ContractDTO;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.fhir.FhirVersion;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class ContractData {

    private final ContractDTO contract;
    private final Job job;
    private final List<Future<ProgressTrackerUpdate>> eobRequestHandles = new LinkedList<>();
    private Future<Integer> aggregatorHandle;
    private Map<Integer, Integer> hashBeneMapper = new HashMap<>();

    public void addEobRequestHandle(Future<ProgressTrackerUpdate> eobRequestHandle, int numBenes) {
        eobRequestHandles.add(eobRequestHandle);
        hashBeneMapper.put(eobRequestHandle.hashCode(), numBenes);
    }

    public void addAggregatorHandle(Future<Integer> aggregatorHandle) {
        this.aggregatorHandle = aggregatorHandle;
    }

    public boolean remainingRequestHandles() {
        return !eobRequestHandles.isEmpty();
    }

    public FhirVersion getFhirVersion() {
        return job.getFhirVersion();
    }

    public int getNumberBenes(Future<ProgressTrackerUpdate> agg) {
        return hashBeneMapper.getOrDefault(agg.hashCode(), 0);
    }
}
