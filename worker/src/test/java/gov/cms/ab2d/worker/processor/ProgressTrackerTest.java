package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.CoverageSummary;
import gov.cms.ab2d.filter.FilterOutByDate;
import gov.cms.ab2d.worker.TestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static gov.cms.ab2d.worker.processor.BundleUtils.createIdentifierWithoutMbi;
import static gov.cms.ab2d.worker.processor.ProgressTracker.EST_BEN_SEARCH_JOB_PERCENTAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProgressTrackerTest {


    @DisplayName("Percentages are calculated as expected with correct arguments")
    @Test
    void testPercentDone() {
        ProgressTracker tracker = ProgressTracker.builder()
                .jobUuid("JOBID")
                .failureThreshold(3)
                .expectedBeneficiaries(12)
                .build();

        int percDone = tracker.getPercentageCompleted();
        int expected = 0;
        assertEquals(expected, percDone);

        ReflectionTestUtils.setField(tracker, "metadataProcessedCount", 6);
        percDone = tracker.getPercentageCompleted();
        expected = asPercent((1.0 - EST_BEN_SEARCH_JOB_PERCENTAGE) * 0.5);
        assertEquals(expected, percDone);

        ReflectionTestUtils.setField(tracker, "metadataProcessedCount", 12);
        percDone = tracker.getPercentageCompleted();
        expected = asPercent(1.0 - EST_BEN_SEARCH_JOB_PERCENTAGE);
        assertEquals(expected, percDone);


        ReflectionTestUtils.setField(tracker, "patientRequestProcessedCount", 6);
        percDone = tracker.getPercentageCompleted();
        expected = asPercent((1 - EST_BEN_SEARCH_JOB_PERCENTAGE)
                + (EST_BEN_SEARCH_JOB_PERCENTAGE * 0.5));
        assertEquals(expected, percDone);

        ReflectionTestUtils.setField(tracker, "patientRequestProcessedCount", 12);
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
        assertEquals(0, percDone);

        ReflectionTestUtils.setField(tracker, "metadataProcessedCount", 6);
        percDone = tracker.getPercentageCompleted();
        assertEquals(0, percDone);

    }

    @DisplayName("Percentages are calculated as expected when metadata searches are completed and eob searches are completed")
    @Test
    void testCompleteJob() {
        ProgressTracker tracker = ProgressTracker.builder()
                .jobUuid("JOBID")
                .failureThreshold(3)
                .expectedBeneficiaries(10)
                .build();

        int percDone = tracker.getPercentageCompleted();
        assertEquals(0, percDone);

        // Pretend all metadata loading is done
        Contract contract = new Contract();
        tracker.addPatients(createPatientsByContractResponse(contract, 10).size());

        percDone = tracker.getPercentageCompleted();
        assertEquals(asPercent(1.0 - EST_BEN_SEARCH_JOB_PERCENTAGE), percDone);

        tracker.addPatientProcessedCount(10);

        assertEquals(100, tracker.getPercentageCompleted());
    }

    @Test
    @DisplayName("Use more interesting numbers to test rounding behavior")
    void testPercentageDoneWithMoreInterestingNumbers() {
        ProgressTracker tracker = ProgressTracker.builder()
                .jobUuid("JOBID")
                .failureThreshold(3)
                .expectedBeneficiaries(12)
                .metadataProcessedCount(12)
                .build();

        List<CoverageSummary> cb1 = createPatientsByContractResponse(new Contract(), 12);
        tracker.addPatients(cb1.size());

        assertEquals(asPercent(1 - EST_BEN_SEARCH_JOB_PERCENTAGE), tracker.getPercentageCompleted());

        tracker.addPatientProcessedCount(1);

        double amountEobProc = (double) 1/12 * EST_BEN_SEARCH_JOB_PERCENTAGE;
        double amountMetadataCompleted = 1 - EST_BEN_SEARCH_JOB_PERCENTAGE;

        int expectedPercentage = asPercent(amountMetadataCompleted + amountEobProc);
        assertEquals(expectedPercentage, tracker.getPercentageCompleted());
    }

    private static int asPercent(double num) {
        return (int) Math.round(100.0 * num);
    }

    private List<CoverageSummary> createPatientsByContractResponse(Contract contract, int num) {
        List<CoverageSummary> summaries = new ArrayList<>();

        FilterOutByDate.DateRange dateRange = TestUtil.getOpenRange();
        for (long idx = 0; idx < num; idx++) {
            CoverageSummary summary = new CoverageSummary(
                    createIdentifierWithoutMbi(idx),
                    contract,
                    List.of(dateRange)
            );
            summaries.add(summary);
        }
        return summaries;
    }
}
