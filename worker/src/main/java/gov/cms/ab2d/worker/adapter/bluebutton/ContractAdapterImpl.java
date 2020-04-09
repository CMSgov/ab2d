package gov.cms.ab2d.worker.adapter.bluebutton;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.eventlogger.EventLogger;
import gov.cms.ab2d.eventlogger.events.ReloadEvent;
import gov.cms.ab2d.filter.FilterOutByDate;
import gov.cms.ab2d.filter.FilterOutByDate.DateRange;
import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse.PatientDTO;
import gov.cms.ab2d.worker.service.BeneficiaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static gov.cms.ab2d.common.util.Constants.CONTRACT_2_BENE_CACHING_ON;


@Slf4j
//@Primary // - once the BFD API starts returning data, change this to primary bean so spring injects this instead of the stub.
@Component
@RequiredArgsConstructor
public class ContractAdapterImpl implements ContractAdapter {

    private static final String BENEFICIARY_ID = "https://bluebutton.cms.gov/resources/variables/bene_id";

    @Value("${contract2bene.caching.threshold:1000}")
    private int cachingThreshold;

    private final BFDClient bfdClient;
    private final ContractRepository contractRepo;
    private final BeneficiaryService beneficiaryService;
    private final PropertiesService propertiesService;
    private final EventLogger eventLogger;

    @Override
    public GetPatientsByContractResponse getPatients(final String contractNumber, final int currentMonth) {

        var patientDTOs = new ArrayList<PatientDTO>();

        final boolean cachingOn = isContractToBeneCachingOn();

        var contract = contractRepo.findContractByContractNumber(contractNumber).get();

        for (var month = 1; month <= currentMonth; month++) {
            var bfdPatientsIds = getPatientsForMonth(contractNumber, contract, month, cachingOn);

            var monthDateRange = toDateRange(month);

            for (String bfdPatientId : bfdPatientsIds) {
                buildPatientDTOs(patientDTOs, monthDateRange, bfdPatientId);
            }
        }

        return toGetPatientsByContractResponse(contractNumber, patientDTOs);
    }

    private boolean isContractToBeneCachingOn() {
        return propertiesService.isToggleOn(CONTRACT_2_BENE_CACHING_ON);
    }

    private Set<String> getPatientsForMonth(String contractNumber, Contract contract, int month, boolean cachingOn) {
        Set<String> bfdPatientsIds = null;

        if (cachingOn) {
            bfdPatientsIds = beneficiaryService.findPatientIdsInDb(contract.getId(), month);
            if (!bfdPatientsIds.isEmpty()) {
                return bfdPatientsIds;
            }

            // patient ids were not found in local DB given the contractId and currentMonth
            // call BFD to fetch the data
        }

        bfdPatientsIds = getPatientIdsForMonth(contractNumber, month);

        if (cachingOn) {
            //if number of benes for this month exceeds cachingThreshold, cache it
            var beneficiaryCount = bfdPatientsIds.size();
            if (beneficiaryCount > cachingThreshold) {
                beneficiaryService.storeBeneficiaries(contract.getId(), bfdPatientsIds, month);
            }
        }

        return bfdPatientsIds;
    }

    /**
     * Given a contractNumber and month,
     * calls BFD api to get a bundle of resources from which the patientIDs are extracted.
     * If there are multiple pages available, fetches all pages.
     *
     * @param contractNumber
     * @param month
     * @return a list of PatientIds
     */
    private Set<String> getPatientIdsForMonth(String contractNumber, Integer month) {
        Bundle bundle = getBundle(contractNumber, month);
        final Set<String> patientIDs = extractPatientIDs(bundle);

        while (bundle.getLink(Bundle.LINK_NEXT) != null) {
            bundle = bfdClient.requestNextBundleFromServer(bundle);
            patientIDs.addAll(extractPatientIDs(bundle));
        }
        eventLogger.log(new ReloadEvent(null, ReloadEvent.FileType.CONTRACT_MAPPING,
                "Contract: " + contractNumber + " - month " + month, patientIDs.size()));

        return patientIDs;
    }

    /**
     * given a contractNumber & a month, calls BFD API to find all patients active in the contract during the month
     * @param contractNumber
     * @param month
     * @return a FHIR bundle of resources containing active patients
     */
    private Bundle getBundle(String contractNumber, int month) {
        try {
            return bfdClient.requestPartDEnrolleesFromServer(contractNumber, month);
        } catch (Exception e) {
            final Throwable rootCause = ExceptionUtils.getRootCause(e);
            log.error("Error while calling for Contract-2-Bene API : {}", e.getMessage(), rootCause);
            throw new RuntimeException(rootCause);
        }
    }

    /**
     * Given a Bundle, filters resources of type Patient and returns a list of patientIds
     * @param bundle
     * @return a list of patientIds
     */
    private Set<String> extractPatientIDs(Bundle bundle) {
        return bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> resource.getResourceType() == ResourceType.Patient)
                .map(resource -> (Patient) resource)
                .map(patient -> extractPatientId(patient))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
    }

    /**
     * Given a patient, extract the patientId
     * @param patient
     * @return patientId if present, null otherwise
     */
    private String extractPatientId(Patient patient) {
        return patient.getIdentifier().stream()
                .filter(identifier -> isBeneficiaryId(identifier))
                .map(Identifier::getValue)
                .findFirst()
                .orElse(null);
    }

    private boolean isBeneficiaryId(Identifier identifier) {
        return identifier.getSystem().equalsIgnoreCase(BENEFICIARY_ID);
    }

    private void buildPatientDTOs(ArrayList<PatientDTO> patientDTOs, DateRange monthDateRange, String bfdPatientId) {
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
     * @param patientDTOs
     * @param bfdPatientId
     * @return an optional PatientDTO
     */
    private Optional<PatientDTO> findPatient(List<PatientDTO> patientDTOs, String bfdPatientId) {
        return patientDTOs.stream()
                .filter(patientDTO -> patientDTO.getPatientId().equals(bfdPatientId))
                .findAny();
    }

    /**
     * Given the ordinal for a month,
     * creates a date range from the start of the month to the end of the month for the current year
     * @param month
     * @return a DateRange
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

    private GetPatientsByContractResponse toGetPatientsByContractResponse(String contractNumber, ArrayList<PatientDTO> patientDTOs) {
        return GetPatientsByContractResponse.builder()
                .contractNumber(contractNumber)
                .patients(patientDTOs)
                .build();
    }
}
