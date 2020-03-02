package gov.cms.ab2d.worker.processor.domainmodel;

import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class ProgressTracker {

    private final String jobUuid;

    @Singular
    private final List<GetPatientsByContractResponse> patientsByContracts;
    Map<String, Integer> amountDoneByContract = new HashMap<>();
    private int totalCount;

    private final int failureThreshold;
    private int failureCount;

    @Setter
    private int lastDbUpdateCount;

    @Setter
    private int lastLogUpdateCount;

    @Setter
    private int lastUpdatedPercentage;

    /**
     * Increment the number of patients processed
     */
    public void incrementProcessedCount(String contract) {
        Integer amountProcessed = amountDoneByContract.get(contract);
        if (amountProcessed == null) {
            amountDoneByContract.put(contract, 1);
        } else {
            amountDoneByContract.put(contract, 1 + amountProcessed);
        }
    }

    public void setProcessedCount(String contract, int amount) {
        amountDoneByContract.put(contract, amount);
    }

    public void incrementFailureCount() {
        ++failureCount;
    }

    /**
     * Get the total number of patients we're processing across all contracts
     *
     * @return number of patients
     */
    public int getTotalCount() {
        if (totalCount == 0) {
            totalCount = patientsByContracts.stream()
                    .mapToInt(patientsByContract -> patientsByContract.getPatients().size())
                    .sum();
        }

        return totalCount;
    }

    public int getProcessedCount() {
        int total = 0;
        for (Map.Entry<String, Integer> val : amountDoneByContract.entrySet()) {
            total += val.getValue();
        }
        return total;
    }

    /**
     * If it's been a long time (by frequency of processed patients) since we've updated the DB
     *
     * @param reportProgressFrequency - how many patients between updates
     * @return true if it's been long enough
     */
    public boolean isTimeToUpdateDatabase(int reportProgressFrequency) {
        return getProcessedCount() - lastDbUpdateCount >= reportProgressFrequency;
    }

    /**
     * If it's been a long time (by frequency of processed patients) since we've updated the log
     *
     * @param reportProgressLogFrequency - how many patients between updates
     * @return true if it's  been long enough
     */
    public boolean isTimeToLog(int reportProgressLogFrequency) {
        return getProcessedCount() - lastLogUpdateCount >= reportProgressLogFrequency;
    }

    /**
     * Return the percentage complete on the job by dividing the processed count by the total count of
     * patients and multiplying by 100 as an integer (0-100)
     *
     * @return the percent complete
     */
    public int getPercentageCompleted() {
        final int percentCompleted = (getProcessedCount() * 100) / getTotalCount();
        lastDbUpdateCount = getProcessedCount();
        return percentCompleted;
    }

    public boolean isErrorCountBelowThreshold() {
        return (failureCount * 100) / getTotalCount() < failureThreshold;
    }
}