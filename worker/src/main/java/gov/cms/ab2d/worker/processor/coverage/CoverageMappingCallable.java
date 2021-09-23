package gov.cms.ab2d.worker.processor.coverage;

import com.newrelic.api.agent.Trace;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.CoverageMapping;
import gov.cms.ab2d.common.model.Identifiers;
import gov.cms.ab2d.fhir.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IDomainResource;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.stream.Collectors.*;

/**
 * Queries BFD for all of the members of a contract during a given month.
 */
@Slf4j
public class CoverageMappingCallable implements Callable<CoverageMapping> {

    public static final String CURRENT_MBI = "current";
    public static final String HISTORIC_MBI = "historic";
    public static final String MBI_ID = "http://hl7.org/fhir/sid/us-mbi";

    // Use year 3 by default since all synthetic data has this year on it
    // todo get rid of this when data is updated
    private static final int SYNTHETIC_DATA_YEAR = 3;

    private final CoverageMapping coverageMapping;
    private final BFDClient bfdClient;
    private final AtomicBoolean completed;

    private final int year;

    private final FhirVersion version;

    private int missingBeneId;
    private int missingCurrentMbi;
    private int hasHistoricalMbi;
    private int missingReferenceYear;
    private int pastReferenceYear;
    private final Map<Integer, Integer> referenceYears = new HashMap<>();

    public CoverageMappingCallable(FhirVersion version, CoverageMapping coverageMapping, BFDClient bfdClient) {
        this.coverageMapping = coverageMapping;
        this.bfdClient = bfdClient;
        this.completed = new AtomicBoolean(false);
        this.year = getCorrectedYear(coverageMapping.getContract().getContractNumber(), coverageMapping.getPeriod().getYear());
        this.version = version;
    }

    public boolean isCompleted() {
        return completed.get();
    }

    public CoverageMapping getCoverageMapping() {
        return coverageMapping;
    }

    @Trace(metricName = "EnrollmentRequest", dispatcher = true)
    @Override
    public CoverageMapping call() {

        int month = coverageMapping.getPeriod().getMonth();
        String contractNumber = coverageMapping.getContract().getContractNumber();

        final Set<Identifiers> patientIds = new HashSet<>();
        int bundleNo = 1;
        try {
            log.info("retrieving contract membership for Contract {}-{}-{} bundle #{}",
                    contractNumber, this.year, month, bundleNo);

            BFDClient.BFD_BULK_JOB_ID.set(coverageMapping.getJobId());

            IBaseBundle bundle = getBundle(contractNumber, month, this.year);
            patientIds.addAll(extractAndFilter(bundle));

            String availableLinks = BundleUtils.getAvailableLinks(bundle);
            log.info("retrieving contract membership for Contract {}-{}-{} bundle #{}, available links {}",
                    contractNumber, this.year, month, bundleNo, availableLinks);

            if (BundleUtils.getNextLink(bundle) == null) {
                log.info("retrieving contract membership for Contract {}-{}-{} bundle #{}, does not have a next link",
                        contractNumber, this.year, month, bundleNo);
            }

            while (BundleUtils.getNextLink(bundle) != null) {

                bundleNo += 1;

                log.info("retrieving contract membership for Contract {}-{}-{} bundle #{}",
                        contractNumber, this.year, month, bundleNo);

                bundle = bfdClient.requestNextBundleFromServer(version, bundle);

                availableLinks = BundleUtils.getAvailableLinksPretty(bundle);

                log.info("retrieving contract membership for Contract {}-{}-{} bundle #{}, available links {}",
                        contractNumber, this.year, month, bundleNo, availableLinks);

                if (BundleUtils.getNextLink(bundle) == null) {
                    log.info("retrieving contract membership for Contract {}-{}-{} bundle #{}, does not have a next link",
                            contractNumber, this.year, month, bundleNo);
                }

                patientIds.addAll(extractAndFilter(bundle));
            }

            log.info("retrieving contract membership for Contract {}-{}-{}, #{} bundles received.",
                    contractNumber, this.year, month, bundleNo);

            coverageMapping.addBeneficiaries(patientIds);
            log.debug("finished reading [{}] Set<String>resources", patientIds.size());

            coverageMapping.completed();
            return coverageMapping;
        } catch (Exception e) {
            log.error("Unable to get patient information for " +
                    contractNumber +
                    " for month " + month +
                    " and year " + this.year, e);
            coverageMapping.failed();
            throw e;
        } finally {
            int total = patientIds.size() + missingReferenceYear + missingBeneId + pastReferenceYear;
            log.info("Search {}-{}-{} found {} distribution of reference years over a total of {} benes",
                    contractNumber, this.year, month, referenceYears, total);
            log.info("Search {}-{}-{} discarded {} entries missing a reference year out of {}",
                    contractNumber, this.year, month, missingReferenceYear, total);
            log.info("Search {}-{}-{} discarded {} entries with a reference year in the past out of {}",
                    contractNumber, this.year, month, pastReferenceYear, total);
            log.info("Search {}-{}-{} discarded {} entries missing a beneficiary identifier out of {}",
                    contractNumber, this.year, month, missingBeneId, total);
            log.info("Search {}-{}-{} found {} entries missing a current mbi out of {}",
                    contractNumber, this.year, month, missingCurrentMbi, total);
            log.info("Search {}-{}-{} found {} entries with a historical mbi out of {}",
                    contractNumber, this.year, month, hasHistoricalMbi, total);

            completed.set(true);
            BFDClient.BFD_BULK_JOB_ID.remove();
        }

    }

    @Trace
    private Set<Identifiers> extractAndFilter(IBaseBundle bundle) {
        return BundleUtils.getPatientStream(bundle, version)
                .filter(this::filterByYear)
                .map(this::extractPatientId)
                .filter(Objects::nonNull)
                .collect(toSet());
    }

    private boolean filterByYear(IDomainResource patient) {
        int referenceYear = ExtensionUtils.getReferenceYear(patient);
        if (referenceYear < 0) {
            log.error("patient returned without reference year violating assumptions");
            missingReferenceYear++;
            return false;
        }

        referenceYears.computeIfPresent(referenceYear, (refYear, occurrences) -> occurrences + 1);
        referenceYears.putIfAbsent(referenceYear, 1);

        // Patient was last a member before this year and should not have been returned
        if (referenceYear < this.year) {
            pastReferenceYear++;
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
    Identifiers extractPatientId(IDomainResource patient) {
        List<PatientIdentifier> ids = IdentifierUtils.getIdentifiers(patient);
        // Get patient beneficiary id
        // if not found eobs cannot be looked up so do not return a meaningful list
        PatientIdentifier beneIdObj = IdentifierUtils.getBeneId(ids);
        if (beneIdObj == null || beneIdObj.getValue() == null) {
            missingBeneId += 1;
            return null;
        }

        // Get current mbi if present or else log not present
        PatientIdentifier currentMbi = IdentifierUtils.getCurrentMbi(ids);
        if (currentMbi == null) {
            missingCurrentMbi += 1;
        }

        LinkedHashSet<PatientIdentifier> historicMbis = IdentifierUtils.getHistoricMbi(ids);
        if (historicMbis == null || !historicMbis.isEmpty()) {
            hasHistoricalMbi += 1;
        }
        LinkedHashSet<String> historicalIds = new LinkedHashSet<>();
        if (historicMbis != null) {
            historicalIds = historicMbis.stream()
                    .map(PatientIdentifier::getValue).collect(toCollection(LinkedHashSet::new));
        }

        if (currentMbi == null) {
            int numOfHistorical = historicalIds.size();
            log.error("Beneficiary " + beneIdObj.getValue() + " has a null MBI and " + numOfHistorical + " historical");
            return new Identifiers(beneIdObj.getValueAsLong(), null, new LinkedHashSet<>(historicalIds));
        }
        return new Identifiers(beneIdObj.getValueAsLong(), currentMbi.getValue(), new LinkedHashSet<>(historicalIds));
    }

    /**
     * given a contractNumber & a month, calls BFD API to find all patients active in the contract during the month
     *
     * @param contractNumber - the PDP's contract number
     * @param searchMonth - the month to pull data for (1-12)
     * @param searchYear - the year to pull data for
     * @return a FHIR bundle of resources containing active patients
     */
    private IBaseBundle getBundle(String contractNumber, int searchMonth, int searchYear) {
        try {
            return bfdClient.requestPartDEnrolleesFromServer(version, contractNumber, searchMonth, searchYear);
        } catch (Exception e) {
            final Throwable rootCause = ExceptionUtils.getRootCause(e);
            log.error("Error while calling for Contract-2-Bene API : {}", e.getMessage(), rootCause);
            throw new RuntimeException(rootCause);
        }
    }

    /**
     * We want to find results in the sandbox but all the data in the sandbox is for an invalid
     * year so we're using this to prevent us from getting no beneficiaries.
     *
     * @param contract - the specified contract number
     * @param coverageYear - the specified coverage year in the coverage search
     * @return if we're in sandbox, return the synthetic data year unless it's the new Synthea data which can use
     * the correct year
     */
    int getCorrectedYear(String contract, int coverageYear) {
        // Use specific year for synthetic data if in a sandbox environment
        if (contract.startsWith("Z") && !contract.startsWith("Z1")) {
            return SYNTHETIC_DATA_YEAR;
        }
        return coverageYear;
    }
}
