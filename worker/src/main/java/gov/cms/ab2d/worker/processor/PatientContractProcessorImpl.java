package gov.cms.ab2d.worker.processor;

import com.newrelic.api.agent.Trace;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.worker.processor.domainmodel.ContractMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.dstu3.model.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PatientContractProcessorImpl implements PatientContractProcessor {
    private static final String BENEFICIARY_ID = "https://bluebutton.cms.gov/resources/variables/bene_id";

    private final BFDClient bfdClient;

    /**
     * Process the retrieval of patient to contract mapping
     */
    @Trace(async = true)
    @Async("patientContractThreadPool")
    public Future<ContractMapping> process(String contractNumber, Integer month) {
        try {
            ContractMapping mapping = new ContractMapping();
            mapping.setMonth(month);
            Bundle bundle = getBundle(contractNumber, month);
            final Set<String> patientIDs = extractPatientIDs(bundle);

            while (bundle.getLink(Bundle.LINK_NEXT) != null) {
                bundle = bfdClient.requestNextBundleFromServer(bundle);
                patientIDs.addAll(extractPatientIDs(bundle));
            }
            mapping.setPatients(patientIDs);
            log.debug("finished reading [{}] Set<String>resources", patientIDs.size());
            return new AsyncResult<>(mapping);
        } catch (Exception e) {
            log.error("Unable to get patient information for " + contractNumber + " for month " + month, e);
            return AsyncResult.forExecutionException(e);
        }
    }

    /**
     * Given a Bundle, filters resources of type Patient and returns a list of patientIds
     *
     * @param bundle the bundle to extract data from
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
     *
     * @param patient - the patient id
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

    /**
     * given a contractNumber & a month, calls BFD API to find all patients active in the contract during the month
     *
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
}
