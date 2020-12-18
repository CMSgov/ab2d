package gov.cms.ab2d.worker.processor.eob;

import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneficiaries;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Collections;

@Getter
@Builder
@Slf4j
public class ProgressTracker {

    private final String jobUuid;

    // Related to determining whether the beneficiary search is complete

    // The ratio between the beneficiary EOF search time in the job vs looking up contract beneficiaries
    @Getter
    private static final double EST_BEN_SEARCH_JOB_PERCENTAGE = 0.7;
    // The number of contracts searched for this job
    private final int numContracts;
    // The number of contract beneficiary searches completed
    private int totalContractBeneficiariesSearchFinished;

    @Singular
    private final List<ContractBeneficiaries> patientsByContracts = new ArrayList<>();

    private int totalCount;
    private int processedCount;

    private final int failureThreshold;
    private int failureCount;

    @Setter
    private int currentMonth;

    @Setter
    private int lastDbUpdateCount;

    @Setter
    private int lastLogUpdateCount;

    @Setter
    private int lastUpdatedPercentage;

    /**
     * Increment the number of patients processed
     */
    public void incrementProcessedCount() {
        ++processedCount;
    }

    public void incrementFailureCount() {
        ++failureCount;
    }

    public void addPatientsByContract(ContractBeneficiaries bene) {
        this.patientsByContracts.add(bene);
    }

    public void incrementTotalContractBeneficiariesSearchFinished() {
        ++totalContractBeneficiariesSearchFinished;
    }

    public int getContractCount(String contractNumber) {
        ContractBeneficiaries response = patientsByContracts.stream()
                .filter(c -> contractNumber.equalsIgnoreCase(c.getContractNumber()))
                .findFirst().orElse(null);
        if (response == null || response.getPatients() == null) {
            return 0;
        }
        return response.getPatients().size();
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

    public int getTotalPossibleCount() {
        int remainingToBeDefined = numContracts - patientsByContracts.size();
        return getTotalCount() + (remainingToBeDefined * getTotalAverageNumber());
    }

    public int getTotalAverageNumber() {
        // If we don't have any contract mappings, assume that it's max value so the denominator will underestimate amount done
        if (getTotalCount() == 0) {
            return Integer.MAX_VALUE;
        }
        // return the average number of patients in known contracts
        IntSummaryStatistics stats = patientsByContracts.stream().mapToInt(c -> c.getPatients().size()).summaryStatistics();
        return (int) Math.round(stats.getAverage());
    }

    /**
     * If it's been a long time (by frequency of processed patients) since we've updated the DB
     *
     * @param reportProgressFrequency - how many patients between updates
     * @return true if it's been long enough
     */
    public boolean isTimeToUpdateDatabase(int reportProgressFrequency) {
        return processedCount - lastDbUpdateCount >= reportProgressFrequency;
    }

    /**
     * If it's been a long time (by frequency of processed patients) since we've updated the log
     *
     * @param reportProgressLogFrequency - how many patients between updates
     * @return true if it's  been long enough
     */
    public boolean isTimeToLog(int reportProgressLogFrequency) {
        return processedCount - lastLogUpdateCount >= reportProgressLogFrequency;
    }

    /**
     * Return the percentage complete on the job by dividing the processed count by the total count of
     * patients and multiplying by 100 as an integer (0-100). This includes both the percentage of contract beneficiary
     * searches and beneficiary EOB searches. We're guestimating the ratio between the two tasks is reasonably constant.
     *
     * @return the percent complete
     */
    public int getPercentageCompleted() {
        double percentBenesDone = 0;
        int totalPossibleCount = getTotalPossibleCount();
        if (totalPossibleCount != 0) {
            double percentBenesDonePart = (double) processedCount / totalPossibleCount;
            if (percentBenesDonePart > 1.0) {
                log.error("Percent of beneficiaries done is more than 100%");
                percentBenesDonePart = 1.0;
            }
            percentBenesDone =  percentBenesDonePart * EST_BEN_SEARCH_JOB_PERCENTAGE;
        }
        double percentContractBeneSearchDonePart = getPercentContractBeneSearchCompleted();
        if (percentContractBeneSearchDonePart > 1.0) {
            log.error("Percent of contract beneficiaries done is more than 100%");
            percentContractBeneSearchDonePart = 1.0;
        }
        double percentContractBeneSearchDone = percentContractBeneSearchDonePart * (1 - EST_BEN_SEARCH_JOB_PERCENTAGE);
        double amountCompleted = percentBenesDone + percentContractBeneSearchDone;

        final int percentCompleted = (int) Math.round(amountCompleted * 100);
        lastDbUpdateCount = processedCount;
        if (percentCompleted > 100) {
            return 99;
        }
        return percentCompleted;
    }

    public boolean isErrorCountBelowThreshold() {
        return (failureCount * 100) / getTotalCount() < failureThreshold;
    }

    /**
     * Return the percentage of the contract beneficiary mapping complete
     *
     * @return the percentage (values 0 to 1)
     */
    public double getPercentContractBeneSearchCompleted() {
        // If we haven't finished one thread of one search, we haven't started
        if (this.totalContractBeneficiariesSearchFinished == 0) {
            return 0.0;
        }

        // If the number of contracts complete (the values aren't added to patientsByContract until they are), it's all done
        if (this.numContracts == this.patientsByContracts.size()) {
            return 1.0;
        }

        // This is the total number of searches threads than need to be done across all contracts
        int totalToSearch = this.numContracts * currentMonth;

        // This is the total completed threads done over the amount that needs to be done
        return ((double) this.totalContractBeneficiariesSearchFinished) / totalToSearch;
    }

    public Map<String, ContractBeneficiaries.PatientDTO> getPatientsByContract(String contractNumber) {
        return getPatientsByContracts()
                .stream()
                .filter(byContract -> byContract.getContractNumber().equals(contractNumber))
                .findFirst()
                .map(ContractBeneficiaries::getPatients)
                .orElse(Collections.emptyMap());
    }
}