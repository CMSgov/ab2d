package gov.cms.ab2d.worker.processor.domainmodel;

import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

@Getter
@Builder
public class WorkInProgress {

    @Singular
    private final List<GetPatientsByContractResponse> patientsByContracts;
    private int processedCount;

    public void incrementProcessedCount() {
        ++processedCount;
    }


}