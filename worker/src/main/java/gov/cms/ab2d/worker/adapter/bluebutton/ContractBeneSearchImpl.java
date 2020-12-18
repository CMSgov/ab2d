package gov.cms.ab2d.worker.adapter.bluebutton;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.Identifiers;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.ReloadEvent;
import gov.cms.ab2d.common.util.FilterOutByDate;
import gov.cms.ab2d.common.util.FilterOutByDate.DateRange;
import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneficiaries.PatientDTO;
import gov.cms.ab2d.worker.processor.eob.PatientContractCallable;
import gov.cms.ab2d.worker.processor.coverage.ContractMapping;
import gov.cms.ab2d.worker.processor.eob.ProgressTracker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Slf4j
@Primary
@Component
public class ContractBeneSearchImpl implements ContractBeneSearch {

    private static final int SLEEP_DURATION = 250;
    private static final int YEAR = LocalDate.now().getYear();

    private final BFDClient bfdClient;
    private final LogManager eventLogger;
    private final ThreadPoolTaskExecutor patientContractThreadPool;
    private final boolean skipBillablePeriodCheck;

    public ContractBeneSearchImpl(BFDClient bfdClient, LogManager eventLogger,
                                  @Qualifier("patientContractThreadPool") ThreadPoolTaskExecutor patientPool,
                                  @Value("${claims.skipBillablePeriodCheck}") boolean skipBillablePeriodCheck) {
        this.bfdClient = bfdClient;
        this.eventLogger = eventLogger;
        this.patientContractThreadPool = patientPool;
        this.skipBillablePeriodCheck = skipBillablePeriodCheck;
    }

    /**
     * Go to BFD to get the mapping of contract to beneficiaries. Given the current month, get all the data for up to the
     * current month. For example, in June, we want data from Jan - June. This will be multi threaded through the
     * PatientContractCallable
     *
     * @param contractNumber - The number of the contract
     * @param currentMonth   - The current month
     * @param tracker        - Updates the progress of loading the beneficiary data
     * @return the mapping of the contract to beneficiaries
     */
    @Override
    public ContractBeneficiaries getPatients(final String contractNumber, final int currentMonth, ProgressTracker tracker) throws ExecutionException, InterruptedException {
        final Segment patientSegment = NewRelic.getAgent().getTransaction().startSegment("Start of gathering patients for contract " +
                contractNumber + " for months up to " + currentMonth);
        patientSegment.setMetricName("GatherPatients");

        List<Future<ContractMapping>> futureHandles = new ArrayList<>();

        tracker.setCurrentMonth(currentMonth);

        for (var month = 1; month <= currentMonth; month++) {
            PatientContractCallable callable = new PatientContractCallable(contractNumber, month, YEAR, bfdClient,
                    skipBillablePeriodCheck, tracker.getJobUuid());
            futureHandles.add(patientContractThreadPool.submit(callable));
        }

        List<ContractMapping> results = getAllResults(futureHandles, contractNumber, tracker);

        ContractBeneficiaries contractBeneficiaries = new ContractBeneficiaries();
        contractBeneficiaries.setContractNumber(contractNumber);
        contractBeneficiaries.setPatients(new HashMap<>());

        patientSegment.end();
        log.info("Found {} beneficiaries for contract {} for all months up to {}", results.size(), contractNumber, currentMonth);

        final Segment dateRangePatientSegment = NewRelic.getAgent().getTransaction().startSegment("Adding date range to existing/new " +
                "patients for contract " + contractNumber + " for months up to " + currentMonth);
        patientSegment.setMetricName("DateRangePatients");

        for (ContractMapping mapping : results) {
            Set<Identifiers> patients = mapping.getPatients();
            if (patients != null && !patients.isEmpty()) {
                DateRange monthDateRange = toDateRange(mapping.getMonth());
                for (Identifiers patient : patients) {
                    addDateRangeToExistingOrNewPatient(contractBeneficiaries, monthDateRange, patient);
                }
            }
        }

        dateRangePatientSegment.end();

        return contractBeneficiaries;
    }

    /**
     * Give a list of Future objects, wait for all the finish and then return the results
     *
     * @param futureHandles  - the Future objects
     * @param contractNumber - the Contract Number for context
     * @return the list of results for all threads
     */
    private List<ContractMapping> getAllResults(List<Future<ContractMapping>> futureHandles, String contractNumber, ProgressTracker tracker) throws ExecutionException, InterruptedException {
        List<ContractMapping> results = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        // Iterate through the futures
        while (!futureHandles.isEmpty()) {
            // See if there are any threads left to finish
            Iterator<Future<ContractMapping>> iterator = futureHandles.iterator();
            while (iterator.hasNext()) {
                Future<ContractMapping> future = iterator.next();
                // If it's done, claim the data and add it to the results, then remove it from the active threads
                if (future.isDone()) {
                    results.add(getContractMapping(future, contractNumber));
                    tracker.incrementTotalContractBeneficiariesSearchFinished();
                    iterator.remove();
                }
            }
            // If we haven't removed all items from the running futures list, sleep for a bit
            if (!futureHandles.isEmpty()) {
                try {
                    Thread.sleep(SLEEP_DURATION);
                } catch (InterruptedException e) {
                    log.warn("interrupted exception in thread.sleep(). Ignoring");
                }
            }
        }
        long endTime = System.currentTimeMillis();
        double timeDif = (double) (endTime - startTime);
        int numMinutes = (int) Math.round(timeDif / 1000.0 / 60.0);
        int totalRecords = results.stream().mapToInt(c -> c.getPatients().size()).sum();
        eventLogger.log(new ReloadEvent(null, ReloadEvent.FileType.CONTRACT_MAPPING,
                "Contract: " + contractNumber + " retrieved " + totalRecords + " in " + numMinutes + " minutes", totalRecords));
        // Return the finished results
        return results;
    }

    /**
     * Retrieve the data from the future after it's "done"
     *
     * @param future     - the future
     * @param contractId - the contract number
     * @return - the mapping
     */
    private ContractMapping getContractMapping(Future<ContractMapping> future, String contractId) throws ExecutionException, InterruptedException {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            log.warn("InterruptedException while calling Future.get() - Getting Mapping for " + contractId);
            throw e;
        } catch (CancellationException e) {
            // This could happen in the rare event that a job was cancelled mid-process.
            // due to which the futures in the queue (that were not yet in progress) were cancelled.
            // Nothing to be done here
            log.warn("CancellationException while calling Future.get() - Getting Mapping for " + contractId);
        }
        return null;
    }

    private void addDateRangeToExistingOrNewPatient(ContractBeneficiaries beneficiaries,
                                                    DateRange monthDateRange, Identifiers patientIds) {

        if (beneficiaries == null) {
            return;
        }

        Map<String, PatientDTO> patientDTOMap = beneficiaries.getPatients();
        PatientDTO patientDTO = patientDTOMap.get(patientIds.getBeneficiaryId());

        if (patientDTO != null) {
            // patient id was already active on this contract in previous month(s)
            // So just add this month to the patient's dateRangesUnderContract
            if (monthDateRange != null) {
                patientDTO.getDateRangesUnderContract().add(monthDateRange);
            }
        } else {
            // new patient id.
            // Create a new PatientDTO for this patient
            // And then add this month to the patient's dateRangesUnderContract

            patientDTO = PatientDTO.builder()
                    .identifiers(patientIds)
                    .dateRangesUnderContract(new ArrayList<>())
                    .build();

            if (monthDateRange != null) {
                patientDTO.getDateRangesUnderContract().add(monthDateRange);
            }

            beneficiaries.getPatients().put(patientIds.getBeneficiaryId(), patientDTO);
        }
    }

    /**
     * Given the ordinal for a month,
     * creates a date range from the start of the month to the end of the month for the current year
     *
     * @param month - the month of the year, 1-12
     * @return a DateRange - the date range created
     */
    private DateRange toDateRange(int month) {
        return FilterOutByDate.getDateRange(month, LocalDate.now().getYear());
    }
}
