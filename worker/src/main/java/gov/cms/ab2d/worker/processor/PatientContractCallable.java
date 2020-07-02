package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.worker.processor.domainmodel.ContractMapping;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.ResourceType;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Slf4j
public class PatientContractCallable implements Callable<ContractMapping> {
    private static final String BENEFICIARY_ID = "https://bluebutton.cms.gov/resources/variables/bene_id";
    private int month;
    private String contractNumber;
    private BFDClient bfdClient;

    public PatientContractCallable(String contractNumber, int month, BFDClient bfdClient) {
        this.month = month;
        this.contractNumber = contractNumber;
        this.bfdClient = bfdClient;
    }

    @Override
    public ContractMapping call() throws Exception {
        System.out.println(Thread.currentThread().getName() + "-" + Thread.currentThread().getId());
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
            System.out.println("finished reading [" + patientIDs.size() + "] patients");
            return mapping;
        } catch (Exception e) {
            log.error("Unable to get patient information for " + contractNumber + " for month " + month, e);
            throw e;
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
                .map(this::extractPatientId)
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
