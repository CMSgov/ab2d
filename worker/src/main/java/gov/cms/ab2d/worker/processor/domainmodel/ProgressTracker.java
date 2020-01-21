package gov.cms.ab2d.worker.processor.domainmodel;

import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

@Getter
@Builder
public class ProgressTracker {

    private final String jobUuid;

    @Singular
    private final List<GetPatientsByContractResponse> patientsByContracts;
    private int processedCount;
    private int totalCount;

    public void incrementProcessedCount() {
        ++processedCount;
    }


    public int getTotalCount() {
        if (totalCount == 0) {
            totalCount = patientsByContracts.stream()
                    .mapToInt(patientsByContract -> patientsByContract.getPatients().size())
                    .sum();
        }

        return totalCount;
    }


}