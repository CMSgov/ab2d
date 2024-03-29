package gov.cms.ab2d.worker.processor;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Builder
@Slf4j
public class ProgressTracker {

    private final String jobUuid;

    @Setter
    private int patientsExpected;
    private int patientsLoadedCount;
    private int patientRequestQueuedCount;
    private int patientRequestProcessedCount;
    private int patientFailureCount;
    private int patientsWithEobsCount;
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

    public void addPatientsWithEobsCount(int value) {
        patientsWithEobsCount += value;
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

        if (patientsExpected == 0) {
            return 0;
        }

        double percentBenesDonePart = (double) patientRequestProcessedCount / patientsExpected;

        final int percentCompleted = (int) Math.round(percentBenesDonePart * 100);
        lastDbUpdateCount = patientRequestProcessedCount;
        if (percentCompleted > 100) {
            log.error("Percent of beneficiaries done is more than 100%");
            return 99;
        }
        return percentCompleted;
    }

    public boolean isErrorThresholdExceeded() {
        return (patientFailureCount * 100) / getTotalCount() >= failureThreshold;
    }
}