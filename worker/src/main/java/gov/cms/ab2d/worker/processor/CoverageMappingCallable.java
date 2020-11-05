package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.CoverageMapping;
import gov.cms.ab2d.common.model.Identifiers;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.dstu3.model.*;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

@Slf4j
public class CoverageMappingCallable implements Callable<CoverageMapping> {

    static final String BENEFICIARY_ID = "https://bluebutton.cms.gov/resources/variables/bene_id";
    public static final String MBI_ID = "http://hl7.org/fhir/sid/us-mbi";


    private final CoverageMapping coverageMapping;
    private final BFDClient bfdClient;
    private final AtomicBoolean completed;
    private final int year;
    private final boolean skipBillablePeriodCheck;

    private int missingIdentifier;
    private int pastYear;

    public CoverageMappingCallable(CoverageMapping coverageMapping, BFDClient bfdClient, boolean skipBillablePeriodCheck) {
        this.coverageMapping = coverageMapping;
        this.bfdClient = bfdClient;
        this.completed = new AtomicBoolean(false);
        this.year = coverageMapping.getPeriod().getYear();
        this.skipBillablePeriodCheck = skipBillablePeriodCheck;
    }

    public boolean isCompleted() {
        return completed.get();
    }

    public CoverageMapping getCoverageMapping() {
        return coverageMapping;
    }

    @Override
    public CoverageMapping call() {

        int month = coverageMapping.getPeriod().getMonth();
        String contractNumber = coverageMapping.getContract().getContractNumber();

        final Set<Identifiers> patientIds = new HashSet<>();
        try {

            Bundle bundle = getBundle(contractNumber, month);
            patientIds.addAll(extractAndFilter(bundle));

            while (bundle.getLink(Bundle.LINK_NEXT) != null) {
                bundle = bfdClient.requestNextBundleFromServer(bundle);
                patientIds.addAll(extractAndFilter(bundle));
            }

            coverageMapping.addBeneficiaries(patientIds);
            log.debug("finished reading [{}] Set<String>resources", patientIds.size());

            coverageMapping.completed();
            return coverageMapping;
        } catch (Throwable e) {
            log.error("Unable to get patient information for " + contractNumber + " for month " + month, e);
            coverageMapping.failed();
            throw e;
        } finally {
            int total = patientIds.size() + pastYear + missingIdentifier;
            log.info("Search discarded {} entries not meeting year filter criteria out of {}", pastYear, total);
            log.info("Search discarded {} entries missing an identifier out of {}", missingIdentifier, total);

            completed.set(true);
        }

    }

    private Set<Identifiers> extractAndFilter(Bundle bundle) {
        return getPatientStream(bundle)
                .filter(patient -> skipBillablePeriodCheck || filterByYear(patient))
                .map(this::extractPatientId)
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
