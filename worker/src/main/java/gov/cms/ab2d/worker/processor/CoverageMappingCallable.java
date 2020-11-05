package gov.cms.ab2d.worker.processor;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.CoverageMapping;
import gov.cms.ab2d.common.model.Identifiers;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.dstu3.model.*;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

@Slf4j
public class CoverageMappingCallable implements Callable<CoverageMapping> {

    static final String BENEFICIARY_ID = "https://bluebutton.cms.gov/resources/variables/bene_id";
    public static final String MBI_ID = "http://hl7.org/fhir/sid/us-mbi";
    static final String EXTRA_PAGE_EXCEPTION_MESSAGE = "could not extract ResultSet";

    private final CoverageMapping coverageMapping;
    private final BFDClient bfdClient;
    private final AtomicBoolean completed;
    private final int year;
    private final boolean skipBillablePeriodCheck;

    private int missingBeneId;
    private int missingMbi;
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

            try {
                while (bundle.getLink(Bundle.LINK_NEXT) != null) {
                    bundle = bfdClient.requestNextBundleFromServer(bundle);
                    patientIds.addAll(extractAndFilter(bundle));
                }
            } catch (InternalErrorException ie) {
                // Catch edge case bug where (number patients) mod (bundle size) == 0
                // Extra bundle link returned that has no data in it which causes exception
                // when attempting to retrieve
                if (!ie.getMessage().contains(EXTRA_PAGE_EXCEPTION_MESSAGE)) {
                    log.warn("exception caught not caused by pulling extra page, will be re-thrown");
                    throw ie;
                }

                log.warn("exception caught caused by extra page included as NEXT bundle, ignoring exception", ie);
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
            int total = patientIds.size() + pastYear + missingBeneId;
            log.info("Search discarded {} entries not meeting year filter criteria out of {}", pastYear, total);
            log.info("Search discarded {} entries missing a beneficiary identifier out of {}", missingBeneId, total);
            log.info("Search found {} entries missing an mbi out of {}", missingMbi, total);

            completed.set(true);
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
            missingBeneId += 1;
            return null;
        }

        if (mbiId.isEmpty()) {
            missingMbi += 1;
        }

        return new Identifiers(beneId.get(), mbiId.orElse(null));
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
