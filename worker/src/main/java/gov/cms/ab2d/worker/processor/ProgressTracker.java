package gov.cms.ab2d.worker.processor;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Builder
@Slf4j
public class ProgressTracker {


    // Related to determining whether the beneficiary search is complete

    // The ratio between the beneficiary EOF search time in the job vs looking up contract beneficiaries
    public static final double EST_BEN_SEARCH_JOB_PERCENTAGE = 0.7;

    private final String jobUuid;

    @Setter
    private int patientsExpected;
    private int patientsLoadedCount;
    private int patientRequestQueuedCount;
    private int patientRequestProcessedCount;
    private int patientFailureCount;
    private int eobsFetchedCount;
    private int eobsProcessedCount;

    @Setter
    private int failureThreshold;

    @Setter
    private int lastDbUpdateCount;

    @Setter
    private int lastLogUpdateCount;

    @Setter
    private int lastUpdatedPercentage;

    public void addPatientRequestQueuedCount(int numQueued) {
        patientRequestQueuedCount += numQueued;
    }

    public void addPatientProcessedCount(int value) {
        patientRequestProcessedCount += value;
    }

    public void addEobFetchedCount(int numFetched) {
        eobsFetchedCount += numFetched;
    }

    public void addEobProcessedCount(int numProcessed) {
        eobsProcessedCount += numProcessed;
    }

    public void addPatientFailureCount(int value) {
        patientFailureCount += value;
    }

    public void addPatientsLoadedCount(int numAdded) {
        patientsLoadedCount += numAdded;
    }

    /**
     * Get the total number of patients we're processing across all contracts
     *
     * @return number of patients
     */
    public int getTotalCount() {
        return patientsExpected;
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

        if (patientsExpected == 0) {
            return 0;
        }

        double percentBenesDonePart = (double) patientRequestProcessedCount / patientsExpected;
        if (percentBenesDonePart > 1.0) {
            log.error("Percent of beneficiaries done is more than 100%");
            percentBenesDonePart = 1.0;
        }

        return  percentBenesDonePart * EST_BEN_SEARCH_JOB_PERCENTAGE;
    }

    public boolean isErrorThresholdExceeded() {
        return (patientFailureCount * 100) / getTotalCount() >= failureThreshold;
    }

    /**
     * Return the percentage of the contract beneficiary mapping complete
     *
     * @return the percentage (values 0 to 1)
     */
    public double getPercentMetadataCompleted() {

        if (patientsExpected == 0) {
            return 0;
        }
        // This is the total completed threads done over the amount that needs to be done
        return ((double) this.patientsLoadedCount) / this.patientsExpected;
    }
}