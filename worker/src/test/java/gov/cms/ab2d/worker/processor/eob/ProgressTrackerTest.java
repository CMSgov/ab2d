package gov.cms.ab2d.worker.processor.eob;

import gov.cms.ab2d.common.util.FilterOutByDate;
import gov.cms.ab2d.worker.TestUtil;
import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneficiaries;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static gov.cms.ab2d.worker.processor.eob.BundleUtils.createIdentifierWithoutMbi;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProgressTrackerTest {
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
        double expectedPercDone = (1.0 / (2 * month)) * (1 - tracker.getEST_BEN_SEARCH_JOB_PERCENTAGE()) * 100;
        assertEquals(percDone, (int) Math.round(expectedPercDone));

        tracker.incrementTotalContractBeneficiariesSearchFinished();
        expectedPercDone = (2.0 / (2 * month)) * (1 - tracker.getEST_BEN_SEARCH_JOB_PERCENTAGE()) * 100;
        percDone = tracker.getPercentageCompleted();
        assertEquals(percDone, (int) Math.round(expectedPercDone));

        for (int i=0; i<((month*2)-2); i++) {
            tracker.incrementTotalContractBeneficiariesSearchFinished();
        }
        expectedPercDone = 100 * (1 - tracker.getEST_BEN_SEARCH_JOB_PERCENTAGE());
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
        assertEquals((int) Math.round(100 * (1 - tracker.getEST_BEN_SEARCH_JOB_PERCENTAGE())), tracker.getPercentageCompleted());
        tracker.getPatientsByContracts().add(getAContractBeneficiary("CONTRACT1", "PAT1", "PAT2", "PAT3"));
        tracker.getPatientsByContracts().add(getAContractBeneficiary("CONTRACT2", "PAT4", "PAT5"));
        assertEquals((int) Math.round(100 * (1 - tracker.getEST_BEN_SEARCH_JOB_PERCENTAGE())), tracker.getPercentageCompleted());
        tracker.incrementProcessedCount();
        double eobComplete = tracker.getEST_BEN_SEARCH_JOB_PERCENTAGE()/5;
        double beneComplete = 1 - tracker.getEST_BEN_SEARCH_JOB_PERCENTAGE();
        assertEquals((int) Math.round(100 * (eobComplete + beneComplete)), tracker.getPercentageCompleted());
        tracker.incrementProcessedCount();
        tracker.incrementProcessedCount();
        tracker.incrementProcessedCount();
        tracker.incrementProcessedCount();
        assertEquals(100, tracker.getPercentageCompleted());
    }

    private ContractBeneficiaries getAContractBeneficiary(String contractId, String ... patientIds) {
        FilterOutByDate.DateRange dr = TestUtil.getOpenRange();
        ContractBeneficiaries beneficiaries = ContractBeneficiaries.builder()
                .contractNumber(contractId).build();
        Map<String, ContractBeneficiaries.PatientDTO> map = new HashMap<>();
        for (String pId : patientIds) {
            ContractBeneficiaries.PatientDTO patient = ContractBeneficiaries.PatientDTO.builder()
                    .identifiers(createIdentifierWithoutMbi(pId))
                    .dateRangesUnderContract(Collections.singletonList(dr))
                    .build();
            map.put(pId, patient);
        }
        beneficiaries.setPatients(map);
        return beneficiaries;
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
        ContractBeneficiaries cb1 = getAContractBeneficiary("C1", "A1", "A2", "A3", "A4");
        ContractBeneficiaries cb2 = getAContractBeneficiary("C2", "B1", "B2", "B3", "B4", "B5", "B6", "B7", "B8");
        tracker.addPatientsByContract(cb1);
        tracker.addPatientsByContract(cb2);
        assertEquals(6, tracker.getTotalAverageNumber());
        assertEquals(18, tracker.getTotalPossibleCount());
        assertEquals(0, tracker.getPercentageCompleted());
        tracker.incrementProcessedCount();
        for (int i=0; i<month; i++) {
            tracker.incrementTotalContractBeneficiariesSearchFinished();
        }
        tracker.incrementTotalContractBeneficiariesSearchFinished();
        double amountEobProc = (double) 1/18 * 100 * 0.7;
        double amountMappingProc = (double) tracker.getTotalContractBeneficiariesSearchFinished()/(3*month) * 100 * 0.3;
        assertEquals((int) Math.round(amountEobProc + amountMappingProc), tracker.getPercentageCompleted());
    }
}
