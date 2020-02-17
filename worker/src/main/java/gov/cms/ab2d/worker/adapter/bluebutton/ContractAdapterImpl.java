package gov.cms.ab2d.worker.adapter.bluebutton;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.filter.FilterOutByDate;
import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse.PatientDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


@Slf4j
//@Primary  - once the BFD API starts returning data, change this to primary bean so spring injects this instead of the stub.
@Component
@RequiredArgsConstructor
public class ContractAdapterImpl implements ContractAdapter {

    private final BFDClient bfdClient;



    @Override
    public GetPatientsByContractResponse getPatients(String contractNumber) {

        var currentMonth = LocalDate.now().getMonthValue();
        log.info("Current Month : [{}]", currentMonth);

        var patientDTOs = new ArrayList<PatientDTO>();

        for (var month = 1; month <= currentMonth; month++) {
            var bfdPatientsIds = getPatientIdsForMonth(contractNumber, month);

            for (String bfdPatientId : bfdPatientsIds) {
                var optPatient = findPatient(patientDTOs, bfdPatientId);

                if (optPatient.isPresent()) {
                    // patient id was already active on this contract in previous month(s)
                    // So just add this month to the patient's datesUnderContract
                    var patientDTO = optPatient.get();
                    patientDTO.getDatesUnderContract().add(toDateRange(month));

                } else {
                    // new patient id.
                    // Create a new PatientDTO for this patient
                    // And then add this month to the patient's datesUnderContract
                    var patientDTO = PatientDTO.builder()
                            .patientId(bfdPatientId)
                            .datesUnderContract(toDateRange(month))
                            .build();

                    patientDTOs.add(patientDTO);
                }
            }
        }

        return GetPatientsByContractResponse.builder()
                .contractNumber(contractNumber)
                .patients(patientDTOs)
                .build();
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
    private List<String> getPatientIdsForMonth(String contractNumber, Integer month) {
        Bundle bundle = getBundle(contractNumber, month);
        final List<String> patientIDs = extractPatientIDs(bundle);

        while (bundle.getLink(Bundle.LINK_NEXT) != null) {
            bundle = bfdClient.requestNextBundleFromServer(bundle);
            patientIDs.addAll(extractPatientIDs(bundle));
        }

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
    private List<String> extractPatientIDs(Bundle bundle) {
        return bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> resource.getResourceType() == ResourceType.Patient)
                .map(resource -> (Patient) resource)
                .map(patient -> extractPatientId(patient))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
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
                .findFirst().orElse(null);
    }

    private boolean isBeneficiaryId(Identifier identifier) {
        return identifier.getSystem().toLowerCase().endsWith("bene_id");
    }



    /**
     * returns the PatientDTO, if the bfdPatientId was present in previous month(s)
     *
     * @param patientDTOs
     * @param bfdPatientId
     * @return PatientDTO is the same patientId was active on the contract in a previous month
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
    private FilterOutByDate.DateRange toDateRange(int month) {
        FilterOutByDate.DateRange dateRange = null;
        try {
            dateRange = FilterOutByDate.getDateRange(month, LocalDate.now().getYear());
        } catch (ParseException e) {
            log.error("unable to create Date Range ", e);
            //should I do something here???
            //ignore for now.
        }

        return dateRange;
    }

}
