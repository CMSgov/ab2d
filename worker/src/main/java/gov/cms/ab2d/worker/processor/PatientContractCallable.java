package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.worker.processor.domainmodel.ContractMapping;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.dstu3.model.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

@Slf4j
public class PatientContractCallable implements Callable<ContractMapping> {
    private static final String BENEFICIARY_ID = "https://bluebutton.cms.gov/resources/variables/bene_id";

    private final int month;
    private final int year;
    private final String contractNumber;
    private final BFDClient bfdClient;

    private int missingIdentifier;
    private int pastYear;

    public PatientContractCallable(String contractNumber, int month, int year, BFDClient bfdClient) {
        this.contractNumber = contractNumber;
        this.month = month;
        this.year = year;
        this.bfdClient = bfdClient;
    }

    @Override
    public ContractMapping call() throws Exception {

        final Set<String> patientIds = new HashSet<>();
        try {
            ContractMapping mapping = new ContractMapping();
            mapping.setMonth(month);
            Bundle bundle = getBundle(contractNumber, month);
            patientIds.addAll(extractAndFilter(bundle));

            while (bundle.getLink(Bundle.LINK_NEXT) != null) {
                bundle = bfdClient.requestNextBundleFromServer(bundle);
                patientIds.addAll(extractAndFilter(bundle));
            }
            mapping.setPatients(patientIds);
            log.debug("finished reading [{}] Set<String>resources", patientIds.size());
            return mapping;
        } catch (Exception e) {
            log.error("Unable to get patient information for " + contractNumber + " for month " + month, e);
            throw e;
        } finally {
            int total = patientIds.size() + pastYear + missingIdentifier;
            log.info("Search discarded {} entries not meeting year filter criteria out of {}", pastYear, total);
            log.info("Search discarded {} entries missing an identifier out of {}", missingIdentifier, total);
        }
    }

    private Set<String> extractAndFilter(Bundle bundle) {
        return getPatientStream(bundle)
                .filter(this::filterByYear)
                .map(this::extractPatientId)
                .filter(this::isValidIdentifier)
                .collect(toSet());
    }

    private Stream<Patient> getPatientStream(Bundle bundle) {
        return bundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> resource.getResourceType() == ResourceType.Patient)
                .map(resource -> (Patient) resource);
    }

    private boolean filterByYear(Patient patient) {
        List<Extension> referenceYearList = patient.getExtensionsByUrl("https://bluebutton.cms.gov/resources/variables/rfrnc_yr");

        if (referenceYearList.isEmpty()) {
            log.error("patient returned without reference year violating assumptions");
            pastYear++;
            return false;
        }

        DateType refYear = (DateType) (referenceYearList.get(0).getValue());

        if (refYear.getYear() != this.year) {
            pastYear++;
            return false;
        }
        return true;
    }

    private boolean isValidIdentifier(String id) {
        boolean blankId = StringUtils.isBlank(id);

        // If blank increment count to log issues
        if (blankId) {
            missingIdentifier++;
        }

        return !blankId;
    }

    /**
     * Given a patient, extract the patientId
     *
     * @param patient - the patient id
     * @return patientId if present, null otherwise
     */
    private String extractPatientId(Patient patient) {
        return patient.getIdentifier().stream()
                .filter(this::isBeneficiaryId)
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
