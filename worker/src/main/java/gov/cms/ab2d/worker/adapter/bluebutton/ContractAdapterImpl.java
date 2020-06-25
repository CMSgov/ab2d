package gov.cms.ab2d.worker.adapter.bluebutton;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.ReloadEvent;
import gov.cms.ab2d.filter.FilterOutByDate;
import gov.cms.ab2d.filter.FilterOutByDate.DateRange;
import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneficiaries.PatientDTO;
import gov.cms.ab2d.worker.processor.PatientContractProcessor;
import gov.cms.ab2d.worker.processor.domainmodel.ContractMapping;
import gov.cms.ab2d.worker.service.BeneficiaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static gov.cms.ab2d.common.util.Constants.CONTRACT_2_BENE_CACHING_ON;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class ContractAdapterImpl implements ContractAdapter {

    private static final int SLEEP_DURATION = 250;

    @Value("${contract2bene.caching.threshold:1000}")
    private int cachingThreshold;

    private final ContractRepository contractRepo;
    private final BeneficiaryService beneficiaryService;
    private final PropertiesService propertiesService;
    private final PatientContractProcessor patientContractProcessor;
    private final LogManager eventLogger;

    @Override
    public ContractBeneficiaries getPatients(final String contractNumber, final int currentMonth) {
        final boolean cachingOn = isContractToBeneCachingOn();
        if (cachingOn) {
            return doWithCachingOn(contractNumber, currentMonth);
        } else {
            return doWithCachingOff(contractNumber, currentMonth);
        }
    }

    /**
     * Go to BFD to get the mapping of contract to beneficiaries. Given the current month, get all the data for up to the
     * current month. For example, in June, we want data from Jan - June. This will be multi threaded through the
     * PatientContractProcessor
     *
     * @param contractNumber - The number of the contract
     * @param currentMonth   - The current month
     * @return the mapping of the contract to beneficiaries
     */
    private ContractBeneficiaries doWithCachingOff(final String contractNumber, final int currentMonth) {
        List<PatientDTO> patientDTOs = new ArrayList<>();
        List<Future<ContractMapping>> futureHandles = new ArrayList<>();
        for (var month = 1; month <= currentMonth; month++) {
            futureHandles.add(patientContractProcessor.process(contractNumber, month));
        }


        List<ContractMapping> results = getAllResults(futureHandles, contractNumber);

        for (ContractMapping mapping : results) {
            Set<String> patients = mapping.getPatients();
            if (patients != null && !patients.isEmpty()) {
                DateRange monthDateRange = toDateRange(mapping.getMonth());
                for (String bfdPatientId : patients) {
                    buildPatientDTOs(patientDTOs, monthDateRange, bfdPatientId);
                }
            }
            //if number of benes for this month exceeds cachingThreshold, cache it
            Optional<Contract> optContract = contractRepo.findContractByContractNumber(contractNumber);
            if (optContract.isEmpty()) {
                return null;
            }
            // TODO - Should we save the data to the DB
            beneficiaryService.storeBeneficiaries(optContract.get().getId(), patients, mapping.getMonth());
        }

        return toGetPatientsByContractResponse(contractNumber, patientDTOs);
    }

    /**
     * Give a list of Future objects, wait for all the finish and then return the results
     *
     * @param futureHandles  - the Future objects
     * @param contractNumber - the Contract Number for context
     * @return the list of results for all threads
     */
    private List<ContractMapping> getAllResults(List<Future<ContractMapping>> futureHandles, String contractNumber) {
        List<ContractMapping> results = new ArrayList<>();

        long startTime = System.currentTimeMillis();
        // Iterate through the futures
        var iterator = futureHandles.iterator();
        while (!futureHandles.isEmpty()) {
            // See if there are any threads left to finish
            while (iterator.hasNext()) {
                Future<ContractMapping> future = iterator.next();
                // If it's done, claim the data and add it to the results, then remove it from the active threads
                if (future.isDone()) {
                    results.add(getContractMapping(future, contractNumber));

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
        long timeDifference = endTime - startTime;
        int numMinutes = (int) timeDifference / 1000 / 60;
        int totalRecords = results.stream().mapToInt(c -> c.getPatients().size()).sum();
        eventLogger.log(new ReloadEvent(null, ReloadEvent.FileType.CONTRACT_MAPPING,
                "Contract: " + contractNumber + " retrieved " + totalRecords + " in " + numMinutes, totalRecords));
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
    private ContractMapping getContractMapping(Future<ContractMapping> future, String contractId) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            log.warn("InterruptedException while calling Future.get() - Getting Mapping for " + contractId);
        } catch (CancellationException e) {
            // This could happen in the rare event that a job was cancelled mid-process.
            // due to which the futures in the queue (that were not yet in progress) were cancelled.
            // Nothing to be done here
            log.warn("CancellationException while calling Future.get() - Getting Mapping for " + contractId);
        }
        return null;
    }

    /**
     * Get the contract mappings from the database. Given the current month, get all the data for up to the
     * current month. For example, in June, we want data from Jan - June.
     *
     * @param contractNumber - the contract number
     * @param currentMonth   - the current month
     * @return the contract patient mappings
     */
    private ContractBeneficiaries doWithCachingOn(final String contractNumber, final int currentMonth) {
        List<PatientDTO> patientDTOs = new ArrayList<>();
        Optional<Contract> optContract = contractRepo.findContractByContractNumber(contractNumber);
        if (optContract.isEmpty()) {
            return null;
        }
        Contract contract = optContract.get();

        for (var month = 1; month <= currentMonth; month++) {
            Set<String> bfdPatientsIds = getPatientsForMonth(contract, month);
            DateRange monthDateRange = toDateRange(month);

            for (String bfdPatientId : bfdPatientsIds) {
                buildPatientDTOs(patientDTOs, monthDateRange, bfdPatientId);
            }
        }
        return toGetPatientsByContractResponse(contractNumber, patientDTOs);
    }

    private boolean isContractToBeneCachingOn() {
        return propertiesService.isToggleOn(CONTRACT_2_BENE_CACHING_ON);
    }

    private Set<String> getPatientsForMonth(Contract contract, int month) {
        return beneficiaryService.findPatientIdsInDb(contract.getId(), month);
    }

    private void buildPatientDTOs(List<PatientDTO> patientDTOs, DateRange monthDateRange, String bfdPatientId) {
        var optPatient = findPatient(patientDTOs, bfdPatientId);
        if (optPatient.isPresent()) {
            // patient id was already active on this contract in previous month(s)
            // So just add this month to the patient's dateRangesUnderContract

            var patientDTO = optPatient.get();
            if (monthDateRange != null) {
                patientDTO.getDateRangesUnderContract().add(monthDateRange);
            }

        } else {
            // new patient id.
            // Create a new PatientDTO for this patient
            // And then add this month to the patient's dateRangesUnderContract

            var patientDTO = PatientDTO.builder()
                    .patientId(bfdPatientId)
                    .build();

            if (monthDateRange != null) {
                patientDTO.getDateRangesUnderContract().add(monthDateRange);
            }

            patientDTOs.add(patientDTO);
        }
    }

    /**
     * Given a patientId, searches for a PatientDTO in a list of PatientDTOs
     *
     * @param patientDTOs - the list of patients
     * @param bfdPatientId - the patient ID we're looking for
     * @return an optional found PatientDTO
     */
    private Optional<PatientDTO> findPatient(List<PatientDTO> patientDTOs, String bfdPatientId) {
        return patientDTOs.stream()
                .filter(patientDTO -> patientDTO.getPatientId().equals(bfdPatientId))
                .findAny();
    }

    /**
     * Given the ordinal for a month,
     * creates a date range from the start of the month to the end of the month for the current year
     *
     * @param month - the month of the year, 1-12
     * @return a DateRange - the date range created
     */
    private DateRange toDateRange(int month) {
        DateRange dateRange = null;
        try {
            dateRange = FilterOutByDate.getDateRange(month, LocalDate.now().getYear());
        } catch (ParseException e) {
            log.error("unable to create Date Range ", e);
            //ignore
        }

        return dateRange;
    }

    private ContractBeneficiaries toGetPatientsByContractResponse(String contractNumber, List<PatientDTO> patientDTOs) {
        return ContractBeneficiaries.builder()
                .contractNumber(contractNumber)
                .patients(patientDTOs)
                .build();
    }
}
