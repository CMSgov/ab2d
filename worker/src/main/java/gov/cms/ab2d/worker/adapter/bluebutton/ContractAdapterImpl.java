package gov.cms.ab2d.worker.adapter.bluebutton;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.filter.FilterOutByDate;
import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse.PatientDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This is a stub implementation that we can use till the BFD API becomes available.
 * The rightmost 3 characters of the contractNumber being passed in must be numeric.
 */
@Slf4j
@Component
public class ContractAdapterImpl implements ContractAdapter {

    @Autowired
    private BFDClient bfdClient;

    @Override
    public GetPatientsByContractResponse getPatients(String contractNumber) {

        var currentMonth = LocalDate.now().getMonthValue();
        log.info("Current Month : [{}]", currentMonth);

        var patientDTOs = new ArrayList<PatientDTO>();

        for (var month = 1; month <= currentMonth; month++) {
            var patientsIds = getPatientIdsForMonth(contractNumber, month);

            processPatientIDs(patientDTOs, month, patientsIds);
        }

        return GetPatientsByContractResponse.builder()
                .contractNumber(contractNumber)
                .patients(patientDTOs)
                .build();
    }


    private List<String> getPatientIdsForMonth(String contractNumber, Integer month) {
        Bundle bundle = getBundle(contractNumber, month);
        final List<String> patientIDs = extractPatientIDs(bundle);

        while (bundle.getLink(Bundle.LINK_NEXT) != null) {
            bundle = bfdClient.requestNextBundleFromServer(bundle);
            patientIDs.addAll(extractPatientIDs(bundle));
        }

        return patientIDs;
    }

    private List<String> extractPatientIDs(Bundle bundle) {
        return bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> resource.getResourceType() == ResourceType.Patient)
                .map(resource -> (Patient) resource)
                .map(patient -> extractPatientId(patient))
                .collect(Collectors.toList());
    }

    private String extractPatientId(Patient patient) {
        return patient.getIdentifier().stream()
                .filter(c -> c.getSystem().toLowerCase().endsWith("bene_id"))
                .map(Identifier::getValue)
                .findFirst().orElse(null);
    }

    private Bundle getBundle(String contractNumber, int month) {
        try {
            final Bundle bundle = bfdClient.requestPartDEnrolleesFromServer(contractNumber, month);
            log.info("Bundle : {}", bundle);
            return bundle;
        } catch (Exception e) {
            final Throwable rootCause = ExceptionUtils.getRootCause(e);
            log.error("Error while calling for Contract-2-Bene API : {}", e.getMessage(), rootCause);
            throw new RuntimeException(rootCause);
        }
    }


    private void processPatientIDs(List<PatientDTO> patientDTOs, int month, List<String> patientsIds) {
        for (String patientId : patientsIds) {
            if (patientId == null) {
                continue;
            }

            var optPatient = patientDTOs.stream()
                    .filter(p -> p.getPatientId().equals(patientId))
                    .findAny();

            if (optPatient.isPresent()) {
                var patientDTO = optPatient.get();
                addDateRange(patientDTO, month);
            } else {
                var patientDTO = createPatientDTO();
                addDateRange(patientDTO, month);
                patientDTOs.add(patientDTO);
            }
        }
    }

    private PatientDTO createPatientDTO() {
        return PatientDTO
                .builder()
                .patientId("patientId")
                .build();
    }

    private void addDateRange(PatientDTO patientDTO, int month) {
        try {
            var dateRange = FilterOutByDate.getDateRange(month, LocalDate.now().getYear());
            patientDTO.getDatesUnderContract().add(dateRange);
        } catch (ParseException e) {
            e.printStackTrace();
            // should I do something here???
        }
    }

}
