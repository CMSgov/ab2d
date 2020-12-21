package gov.cms.ab2d.worker.processor.eob;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.CoverageSummary;
import gov.cms.ab2d.common.util.FilterOutByDate;
import gov.cms.ab2d.worker.TestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.time.LocalDate;
import java.util.*;

import static gov.cms.ab2d.worker.processor.eob.BundleUtils.createIdentifierWithoutMbi;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProgressTrackerTest {
    @Test
    void testPercentDone() {
        int month = LocalDate.now().getMonthValue();
        ProgressTracker tracker = ProgressTracker.builder()
                .jobUuid("JOBID")
                .numContracts(2)
                .failureThreshold(3)
                .currentMonth(month)
                .build();
        int percDone = tracker.getPercentageCompleted();
        assertEquals(0, percDone);

        tracker.incrementTotalContractBeneficiariesSearchFinished();
        percDone = tracker.getPercentageCompleted();
        double expectedPercDone = (1.0 / (2 * month)) * (1 - ProgressTracker.EST_BEN_SEARCH_JOB_PERCENTAGE) * 100;
        assertEquals(percDone, (int) Math.round(expectedPercDone));

        tracker.incrementTotalContractBeneficiariesSearchFinished();
        expectedPercDone = (2.0 / (2 * month)) * (1 - ProgressTracker.EST_BEN_SEARCH_JOB_PERCENTAGE) * 100;
        percDone = tracker.getPercentageCompleted();
        assertEquals(percDone, (int) Math.round(expectedPercDone));

        for (int i=0; i<((month*2)-2); i++) {
            tracker.incrementTotalContractBeneficiariesSearchFinished();
        }
        expectedPercDone = 100 * (1 - ProgressTracker.EST_BEN_SEARCH_JOB_PERCENTAGE);
        percDone = tracker.getPercentageCompleted();
        assertEquals(percDone, (int) Math.round(expectedPercDone));
    }

    @Test
    void testCompleteJob() throws ParseException {
        int month = LocalDate.now().getMonthValue();
        ProgressTracker tracker = ProgressTracker.builder()
                .jobUuid("JOBID")
                .numContracts(2)
                .failureThreshold(3)
                .currentMonth(month)
                .build();
        int percDone = tracker.getPercentageCompleted();
        assertEquals(0, percDone);
        for (int i=0; i<(month*2); i++) {
            tracker.incrementTotalContractBeneficiariesSearchFinished();
        }
        assertEquals((int) Math.round(100 * (1 - ProgressTracker.EST_BEN_SEARCH_JOB_PERCENTAGE)), tracker.getPercentageCompleted());
        tracker.addPatients(getBeneficiaries("CONTRACT1", "PAT1", "PAT2", "PAT3"));
        tracker.addPatients(getBeneficiaries("CONTRACT2", "PAT4", "PAT5"));
        assertEquals((int) Math.round(100 * (1 - ProgressTracker.EST_BEN_SEARCH_JOB_PERCENTAGE)), tracker.getPercentageCompleted());
        tracker.incrementProcessedCount();
        double eobComplete = ProgressTracker.EST_BEN_SEARCH_JOB_PERCENTAGE/5;
        double beneComplete = 1 - ProgressTracker.EST_BEN_SEARCH_JOB_PERCENTAGE;
        assertEquals((int) Math.round(100 * (eobComplete + beneComplete)), tracker.getPercentageCompleted());
        tracker.incrementProcessedCount();
        tracker.incrementProcessedCount();
        tracker.incrementProcessedCount();
        tracker.incrementProcessedCount();
        assertEquals(100, tracker.getPercentageCompleted());
    }

    private List<CoverageSummary> getBeneficiaries(String contractId, String ... patientIds) {
        FilterOutByDate.DateRange dr = TestUtil.getOpenRange();

        Contract contract = new Contract();
        contract.setContractNumber(contractId);

        return Arrays.stream(patientIds)
                .map(pId -> new CoverageSummary(createIdentifierWithoutMbi(pId), contract, singletonList(dr)))
                .collect(toList());
    }

    @Test
    @DisplayName("When you don't have all the contract mappings, how can you estimate percent done")
    void testPercentageDoneWithoutAllContractMappings() throws ParseException {
        int month = LocalDate.now().getMonthValue();
        ProgressTracker tracker = ProgressTracker.builder()
                .jobUuid("JOBID")
                .numContracts(3)
                .failureThreshold(3)
                .currentMonth(month)
                .build();

        List<CoverageSummary> cb1 = getBeneficiaries("C1", "A1", "A2", "A3", "A4",
                "A5", "A6", "A7", "A8", "A9", "A10", "A11", "A12");
        tracker.addPatients(cb1);

        assertEquals(0, tracker.getPercentageCompleted());

        tracker.incrementProcessedCount();
        for (int i=0; i<month; i++) {
            tracker.incrementTotalContractBeneficiariesSearchFinished();
        }

        tracker.incrementTotalContractBeneficiariesSearchFinished();

        double amountEobProc = (double) 1/12 * 100 * 0.7;
        double amountMappingProc = (double) tracker.getTotalContractBeneficiariesSearchFinished()/(3*month) * 100 * 0.3;

        assertEquals((int) Math.round(amountEobProc + amountMappingProc), tracker.getPercentageCompleted());
    }
}
