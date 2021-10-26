package gov.cms.ab2d.worker.processor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProgressTrackerTest {


    @DisplayName("Percentages are calculated as expected with correct arguments")
    @Test
    void testPercentDone() {
        ProgressTracker tracker = ProgressTracker.builder()
                .jobUuid("JOBID")
                .failureThreshold(3)
                .patientsExpected(12)
                .build();

        // When no work done respond with zero percent completed
        int percDone = tracker.getPercentageCompleted();
        assertEquals(0, percDone);

        tracker.addPatientProcessedCount(6);
        percDone = tracker.getPercentageCompleted();
        assertEquals(50, percDone);

        tracker.addPatientProcessedCount(6);
        percDone = tracker.getPercentageCompleted();
        assertEquals(100, percDone);
    }

    @DisplayName("Don't provide percentage if expected beneficiaries is not set")
    @Test
    void expectedBeneficiariesMissing() {

        ProgressTracker tracker = ProgressTracker.builder()
                .jobUuid("JOBID")
                .failureThreshold(3)
                .build();

        int percDone = tracker.getPercentageCompleted();
        tracker.addPatientProcessedCount(6);
        assertEquals(0, percDone);

        tracker.addPatientProcessedCount(6);
        percDone = tracker.getPercentageCompleted();
        assertEquals(0, percDone);

    }

    @DisplayName("Percentages are calculated as expected when metadata searches are completed and eob searches are completed")
    @Test
    void testCompleteJob() {
        ProgressTracker tracker = ProgressTracker.builder()
                .jobUuid("JOBID")
                .failureThreshold(3)
                .patientsExpected(10)
                .build();

        int percDone = tracker.getPercentageCompleted();
        assertEquals(0, percDone);

        // Pretend all metadata loading is done but no eobs are done
        tracker.addPatientsLoadedCount(10);
        percDone = tracker.getPercentageCompleted();
        assertEquals(0, percDone);

        tracker.addPatientProcessedCount(10);
        assertEquals(100, tracker.getPercentageCompleted());
    }

    @Test
    @DisplayName("Use more interesting numbers to test rounding behavior")
    void testPercentageDoneWithMoreInterestingNumbers() {
        ProgressTracker tracker = ProgressTracker.builder()
                .jobUuid("JOBID")
                .failureThreshold(3)
                .patientsExpected(12)
                .patientsLoadedCount(12)
                .build();

        // Loading patients does not contribute
        tracker.addPatientsLoadedCount(12);
        assertEquals(0, tracker.getPercentageCompleted());

        tracker.addPatientProcessedCount(1);

        double amountEobProc = (double) 1/12;

        int expectedPercentage = asPercent(amountEobProc);
        assertEquals(expectedPercentage, tracker.getPercentageCompleted());
    }

    @Test
    @DisplayName("Completion percentage cannot go over 100%")
    void testPercentageDoneNeverMoreThan100() {

        ProgressTracker tracker = ProgressTracker.builder()
                .jobUuid("JOBID")
                .failureThreshold(3)
                .patientsExpected(12)
                .patientsLoadedCount(12)
                .build();

        // Loading patients does not contribute
        tracker.addPatientProcessedCount(13);
        assertEquals(99, tracker.getPercentageCompleted());
    }

    private static int asPercent(double num) {
        return (int) Math.round(100.0 * num);
    }
}
