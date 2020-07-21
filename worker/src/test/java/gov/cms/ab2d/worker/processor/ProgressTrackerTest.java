package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.filter.FilterOutByDate;
import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneficiaries;
import gov.cms.ab2d.worker.processor.domainmodel.ProgressTracker;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProgressTrackerTest {
    @Test
    void testPercentDone() {
        ProgressTracker tracker = ProgressTracker.builder()
                .jobUuid("JOBID")
                .numContracts(2)
                .failureThreshold(3)
                .currentMonth(2)
                .build();
        int percDone = tracker.getPercentageCompleted();
        assertEquals(0, percDone);

        tracker.incrementTotalContractBeneficiariesSearchFinished();
        percDone = tracker.getPercentageCompleted();
        int month = LocalDate.now().getMonthValue();
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

    private ContractBeneficiaries getAContractBeneficiary(String contractId, String ... patientIds) throws ParseException {
        FilterOutByDate.DateRange dr = new FilterOutByDate.DateRange(new Date(0), new Date());
        ContractBeneficiaries beneficiaries = ContractBeneficiaries.builder()
                .contractNumber(contractId).build();
        Map<String, ContractBeneficiaries.PatientDTO> map = new HashMap<>();
        for (String pId : patientIds) {
            ContractBeneficiaries.PatientDTO patient = ContractBeneficiaries.PatientDTO.builder()
                    .patientId(pId)
                    .dateRangesUnderContract(Collections.singletonList(dr))
                    .build();
            map.put(pId, patient);
        }
        beneficiaries.setPatients(map);
        return beneficiaries;
    }
}
