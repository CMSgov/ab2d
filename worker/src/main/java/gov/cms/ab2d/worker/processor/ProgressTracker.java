package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.CoverageSummary;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@Slf4j
public class ProgressTracker {

    private final String jobUuid;

    // Related to determining whether the beneficiary search is complete

    // The ratio between the beneficiary EOF search time in the job vs looking up contract beneficiaries
    public static final double EST_BEN_SEARCH_JOB_PERCENTAGE = 0.7;

    @Singular
    private final Map<String, CoverageSummary> patients = new HashMap<>();

    private int metadataProcessedCount;
    private int patientRequestQueuedCount;
    private int patientRequestProcessedCount;
    private int eobsProcessedCount;

    @Setter
    private int expectedBeneficiaries;

    private final int failureThreshold;
    private int failureCount;

    @Setter
    private int lastDbUpdateCount;

    @Setter
    private int lastLogUpdateCount;

    @Setter
    private int lastUpdatedPercentage;

    public void incrementPatientRequestQueuedCount() {
        ++patientRequestQueuedCount;
    }

    public void incrementPatientRequestProcessedCount() {
        ++patientRequestProcessedCount;
    }

    public void incrementEobProcessedCount() {
        ++eobsProcessedCount;
    }

    public void incrementFailureCount() {
        ++failureCount;
    }

    public void addPatients(List<CoverageSummary> coverage) {
        coverage.forEach(summary -> patients.put(summary.getIdentifiers().getBeneficiaryId(), summary));
        metadataProcessedCount += coverage.size();
    }

    /**
     * Get the total number of patients we're processing across all contracts
     *
     * @return number of patients
     */
    public int getTotalCount() {
        return patients.size();
    }

    /**
     * If it's been a long time (by frequency of processed patients) since we've updated the DB
     *
     * @param reportProgressFrequency - how many patients between updates
     * @return true if it's been long enough
     */
    public boolean isTimeToUpdateDatabase(int reportProgressFrequency) {
        return patientRequestProcessedCount - lastDbUpdateCount >= reportProgressFrequency;
    }

    /**
     * If it's been a long time (by frequency of processed patients) since we've updated the log
     *
     * @param reportProgressLogFrequency - how many patients between updates
     * @return true if it's  been long enough
     */
    public boolean isTimeToLog(int reportProgressLogFrequency) {
        return patientRequestProcessedCount - lastLogUpdateCount >= reportProgressLogFrequency;
    }

    /**
     * Return the percentage complete on the job by dividing the processed count by the total count of
     * patients and multiplying by 100 as an integer (0-100). This includes both the percentage of contract beneficiary
     * searches and beneficiary EOB searches. We're guestimating the ratio between the two tasks is reasonably constant.
     *
     * @return the percent complete
     */
    public int getPercentageCompleted() {

        double percentMetadataDone = getPercentMetadataCompleted();
        if (percentMetadataDone > 1.0) {
            log.error("Percent of contract beneficiaries done is more than 100%");
            percentMetadataDone = 1.0;
        }

        double percentBenesDone = getPercentEobsCompleted();

        double percentContractBeneSearchDone = percentMetadataDone * (1 - EST_BEN_SEARCH_JOB_PERCENTAGE);
        double amountCompleted = percentBenesDone + percentContractBeneSearchDone;

        final int percentCompleted = (int) Math.round(amountCompleted * 100);
        lastDbUpdateCount = patientRequestProcessedCount;
        if (percentCompleted > 100) {
            return 99;
        }
        return percentCompleted;
    }

    private double getPercentEobsCompleted() {

        if (expectedBeneficiaries == 0) {
            return 0;
        }

        double percentBenesDonePart = (double) patientRequestProcessedCount / expectedBeneficiaries;
        if (percentBenesDonePart > 1.0) {
            log.error("Percent of beneficiaries done is more than 100%");
            percentBenesDonePart = 1.0;
        }

        return  percentBenesDonePart * EST_BEN_SEARCH_JOB_PERCENTAGE;
    }

    public boolean isErrorCountBelowThreshold() {
        return (failureCount * 100) / getTotalCount() < failureThreshold;
    }

    /**
     * Return the percentage of the contract beneficiary mapping complete
     *
     * @return the percentage (values 0 to 1)
     */
    public double getPercentMetadataCompleted() {

        if (expectedBeneficiaries == 0) {
            return 0;
        }
        // This is the total completed threads done over the amount that needs to be done
        return ((double) this.metadataProcessedCount) / this.expectedBeneficiaries;
    }
}