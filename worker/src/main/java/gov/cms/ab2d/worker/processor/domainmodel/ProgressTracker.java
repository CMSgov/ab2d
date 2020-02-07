package gov.cms.ab2d.worker.processor.domainmodel;

import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;

import java.util.List;

@Getter
@Builder
public class ProgressTracker {

    private final String jobUuid;

    @Singular
    private final List<GetPatientsByContractResponse> patientsByContracts;
    private int totalCount;
    private int processedCount;

    private final int failureThreshold;
    private int failureCount;

    @Setter
    private int lastDbUpdateCount;

    @Setter
    private int lastLogUpdateCount;

    @Setter
    private int lastUpdatedPercentage;

    public void incrementProcessedCount() {
        ++processedCount;
    }

    public void incrementFailureCount() {
        ++failureCount;
    }


    public int getTotalCount() {
        if (totalCount == 0) {
            totalCount = patientsByContracts.stream()
                    .mapToInt(patientsByContract -> patientsByContract.getPatients().size())
                    .sum();
        }

        return totalCount;
    }

    public boolean isTimeToUpdateDatabase(int reportProgressFrequency) {
        return processedCount - lastDbUpdateCount >= reportProgressFrequency;
    }

    public boolean isTimeToLog(int reportProgressLogFrequency) {
        return processedCount - lastLogUpdateCount >= reportProgressLogFrequency;
    }

    public int getPercentageCompleted() {
        final int percentCompleted = (processedCount * 100) / getTotalCount();
        lastDbUpdateCount = processedCount;
        return percentCompleted;
    }

    public boolean failJob() {
        return (failureCount * 100) / processedCount >= failureThreshold;
    }


}