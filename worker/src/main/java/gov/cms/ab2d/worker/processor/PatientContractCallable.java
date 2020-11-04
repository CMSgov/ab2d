package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.worker.processor.domainmodel.ContractMapping;
import gov.cms.ab2d.worker.processor.domainmodel.Identifiers;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.dstu3.model.*;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

@Slf4j
public class PatientContractCallable implements Callable<ContractMapping> {
    private static final String BENEFICIARY_ID = "https://bluebutton.cms.gov/resources/variables/bene_id";
    public static final String MBI_ID = "http://hl7.org/fhir/sid/us-mbi";

    private final int month;
    private final int year;
    private final boolean skipBillablePeriodCheck;
    private final String contractNumber;
    private final BFDClient bfdClient;

    private int missingIdentifier;
    private int pastYear;

    public PatientContractCallable(String contractNumber, int month, int year, BFDClient bfdClient, boolean skipBillablePeriodCheck) {
        this.contractNumber = contractNumber;
        this.month = month;
        this.year = year;
        this.bfdClient = bfdClient;
        this.skipBillablePeriodCheck = skipBillablePeriodCheck;
    }

    @Override
    public ContractMapping call() throws Exception {

        final Set<Identifiers> patientIds = new HashSet<>();
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

    private Set<Identifiers> extractAndFilter(Bundle bundle) {
        return getPatientStream(bundle)
                .filter(patient -> skipBillablePeriodCheck || filterByYear(patient))
                .map(this::extractPatientId)
                .filter(Objects::nonNull)
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

    /**
     * Given a patient, extract the patientId
     *
     * @param patient - the patient id
     * @return patientId if present, null otherwise
     */
    private Identifiers extractPatientId(Patient patient) {
        List<Identifier> identifiers = patient.getIdentifier();

        Optional<String> beneId =  identifiers.stream()
                .filter(this::isBeneficiaryId)
                .map(Identifier::getValue)
                .findFirst();

        Optional<String> mbiId = identifiers.stream()
                .filter(this::isMbiId)
                .map(Identifier::getValue)
                .findFirst();

        if (beneId.isEmpty()) {
            log.warn("missing a beneficiary id on a patient so patient will not be searched");
            missingIdentifier += 1;
            return null;
        }

        if (mbiId.isEmpty()) {
            log.warn("missing an mbi id on a patient so patient will not be searched");
            missingIdentifier += 1;
            return null;
        }

        return new Identifiers(beneId.get(), mbiId.get());
    }

    private boolean isBeneficiaryId(Identifier identifier) {
        return identifier.getSystem().equalsIgnoreCase(BENEFICIARY_ID);
    }

    private boolean isMbiId(Identifier identifier) {
        return identifier.getSystem().equalsIgnoreCase(MBI_ID);
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
