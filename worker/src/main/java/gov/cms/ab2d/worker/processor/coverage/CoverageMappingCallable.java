package gov.cms.ab2d.worker.processor.coverage;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.CoverageMapping;
import gov.cms.ab2d.common.model.Identifiers;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.dstu3.model.*;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;

/**
 * Queries BFD for all of the members of a contract during a given month.
 *
 * todo remove PatientContractCallable and replace with this class which will load the data
 *      before a job runs.
 */
@Slf4j
public class CoverageMappingCallable implements Callable<CoverageMapping> {

    static final String BENEFICIARY_ID = "https://bluebutton.cms.gov/resources/variables/bene_id";
    public static final String CURRENCY_IDENTIFIER =
            "https://bluebutton.cms.gov/resources/codesystem/identifier-currency";
    public static final String CURRENT_MBI = "current";
    public static final String HISTORIC_MBI = "historic";
    public static final String MBI_ID = "http://hl7.org/fhir/sid/us-mbi";

    private final CoverageMapping coverageMapping;
    private final BFDClient bfdClient;
    private final AtomicBoolean completed;
    private final int year;
    private final boolean skipBillablePeriodCheck;


    private int missingBeneId;
    private int missingCurrentMbi;
    private int hasHistoricalMbi;
    private int filteredByYear;

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
        int bundleNo = 1;
        try {
            log.info("retrieving contract membership for Contract {}-{}-{} bundle #{}",
                    contractNumber, year, month, bundleNo);

            BFDClient.BFD_BULK_JOB_ID.set(coverageMapping.getJobId());

            Bundle bundle = getBundle(contractNumber, month);
            patientIds.addAll(extractAndFilter(bundle));

            String availableLinks = bundle.getLink().stream()
                    .map(Bundle.BundleLinkComponent::getRelation)
                    .collect(joining(" , "));
            log.info("retrieving contract membership for Contract {}-{}-{} bundle #{}, available links {}",
                    contractNumber, year, month, bundleNo, availableLinks);

            if (bundle.getLink(Bundle.LINK_NEXT) == null) {
                log.info("retrieving contract membership for Contract {}-{}-{} bundle #{}, does not have a next link",
                        contractNumber, year, month, bundleNo);
            }

            while (bundle.getLink(Bundle.LINK_NEXT) != null) {

                bundleNo += 1;

                log.info("retrieving contract membership for Contract {}-{}-{} bundle #{}",
                        contractNumber, year, month, bundleNo);

                bundle = bfdClient.requestNextBundleFromServer(bundle);

                availableLinks = bundle.getLink().stream()
                        .map(link -> link.getRelation() + " -> " + link.getUrl())
                        .collect(joining(" , "));

                log.info("retrieving contract membership for Contract {}-{}-{} bundle #{}, available links {}",
                        contractNumber, year, month, bundleNo, availableLinks);

                if (bundle.getLink(Bundle.LINK_NEXT) == null) {
                    log.info("retrieving contract membership for Contract {}-{}-{} bundle #{}, does not have a next link",
                            contractNumber, year, month, bundleNo);
                }

                patientIds.addAll(extractAndFilter(bundle));
            }

            log.info("retrieving contract membership for Contract {}-{}-{}, #{} bundles received.",
                    contractNumber, year, month, bundleNo);

            coverageMapping.addBeneficiaries(patientIds);
            log.debug("finished reading [{}] Set<String>resources", patientIds.size());

            coverageMapping.completed();
            return coverageMapping;
        } catch (Exception e) {
            log.error("Unable to get patient information for " + contractNumber + " for month " + month, e);
            coverageMapping.failed();
            throw e;
        } finally {
            int total = patientIds.size() + filteredByYear + missingBeneId;
            log.info("Search discarded {} entries not meeting year filter criteria out of {}", filteredByYear, total);
            log.info("Search discarded {} entries missing a beneficiary identifier out of {}", missingBeneId, total);
            log.info("Search found {} entries missing a current mbi out of {}", missingCurrentMbi, total);
            log.info("Search found {} entries with a historical mbi out of {}", hasHistoricalMbi, total);

            completed.set(true);
            BFDClient.BFD_BULK_JOB_ID.remove();
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
            filteredByYear++;
            return false;
        }

        DateType refYear = (DateType) (referenceYearList.get(0).getValue());

        if (refYear.getYear() != this.year) {
            filteredByYear++;
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

        // Get patient beneficiary id
        // if not found eobs cannot be looked up so do not return a meaningful list
        Optional<String> beneId = getBeneficiaryId(identifiers);
        if (beneId.isEmpty()) {
            missingBeneId += 1;
            return null;
        }

        // Get current mbi if present or else log not present
        String currentMbi = getCurrentMbi(identifiers);
        if (currentMbi == null) {
            missingCurrentMbi += 1;
        }

        LinkedHashSet<String> historicMbis = getHistoricMbis(identifiers, currentMbi);
        if (!historicMbis.isEmpty()) {
            hasHistoricalMbi += 1;
        }

        return new Identifiers(beneId.get(), currentMbi, historicMbis);
    }

    private Optional<String> getBeneficiaryId(List<Identifier> identifiers) {
        return identifiers.stream()
                .filter(this::isBeneficiaryId)
                .map(Identifier::getValue)
                .findFirst();
    }

    private String getCurrentMbi(List<Identifier> identifiers) {
        return identifiers.stream()
                .filter(this::isCurrentMbi)
                .map(Identifier::getValue)
                .findFirst().orElse(null);
    }

    private LinkedHashSet<String> getHistoricMbis(List<Identifier> identifiers, String currentMbi) {
        return identifiers.stream()
                .filter(this::isHistoricalMbi)
                .map(Identifier::getValue)
                .filter(historicMbi -> !historicMbi.equals(currentMbi))
                .collect(toCollection(LinkedHashSet::new));
    }

    private boolean isBeneficiaryId(Identifier identifier) {
        if (StringUtils.isAnyBlank(identifier.getSystem(), identifier.getValue())) {
            return false;
        }

        return identifier.getSystem().equalsIgnoreCase(BENEFICIARY_ID);
    }

    private boolean isCurrentMbi(Identifier identifier) {
        return isMatchingMbi(identifier, CURRENT_MBI);
    }

    private boolean isHistoricalMbi(Identifier identifier) {
        return isMatchingMbi(identifier, HISTORIC_MBI);
    }

    private boolean isMatchingMbi(Identifier identifier, String historic) {

        if (StringUtils.isAnyBlank(identifier.getSystem(), identifier.getValue())) {
            return false;
        }

        if (!identifier.getSystem().equals(MBI_ID)) {
            return false;
        }

        Optional<Extension> currencyExtension = getCurrencyExtension(identifier);

        // Assume historical if no extension found
        if (currencyExtension.isEmpty()) {
            return false;
        }

        Coding code = (Coding) currencyExtension.get().getValue();
        return code.getCode().equals(historic);
    }

    private Optional<Extension> getCurrencyExtension(Identifier identifier) {
        if (identifier.getExtension().isEmpty()) {
            return Optional.empty();
        }

        return identifier.getExtension().stream().filter(
                extension -> extension.getUrl().equals(CURRENCY_IDENTIFIER)
        ).findFirst();
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
