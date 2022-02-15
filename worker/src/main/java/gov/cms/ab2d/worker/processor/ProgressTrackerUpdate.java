package gov.cms.ab2d.worker.processor;

import lombok.Getter;

@Getter
public class ProgressTrackerUpdate {

    private int patientRequestProcessedCount;
    private int patientWithEobCount;
    private int patientFailureCount;
    private int eobsFetchedCount;
    private int eobsProcessedCount;


    public void incPatientProcessCount() {
        patientRequestProcessedCount++;
    }

    public void incPatientProcessCount(int num) {
        patientRequestProcessedCount += num;
    }

    public void incPatientFailureCount(int num) {
        patientFailureCount += num;
    }

    public void incPatientFailureCount() {
        patientFailureCount++;
    }

    public void incPatientsWithEobsCount() {
        patientWithEobCount++;
    }

    public void addEobFetchedCount(int numFetched) {
        eobsFetchedCount += numFetched;
    }

    public void addEobProcessedCount(int numProcessed) {
        eobsProcessedCount += numProcessed;
    }
}
