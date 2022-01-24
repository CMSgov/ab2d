package gov.cms.ab2d.worker.processor.coverage;

import com.newrelic.api.agent.Trace;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.coverage.model.CoverageMapping;
import gov.cms.ab2d.coverage.model.Identifiers;
import gov.cms.ab2d.eventlogger.Ab2dEnvironment;
import gov.cms.ab2d.fhir.BundleUtils;
import gov.cms.ab2d.fhir.ExtensionUtils;
import gov.cms.ab2d.fhir.FhirVersion;
import gov.cms.ab2d.fhir.IdentifierUtils;
import gov.cms.ab2d.fhir.PatientIdentifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IDomainResource;


import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;

/**
 * Perform queries to BFD to retrieve all coverage/enrollment related to a Part D contract
 * for a given month and year.
 *
 * The results of these queries are stored into a {@link CoverageMapping} object in-memory. Additionally,
 * any artifacts/issues with identifiers returned from BFD are documented and reported as statistics.
 *
 * The contract, month, and year are represented as an {@link gov.cms.ab2d.coverage.model.CoveragePeriod}
 *
 */
@Slf4j
public class CoverageMappingCallable implements Callable<CoverageMapping> {

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
    private final Ab2dEnvironment appEnv;

    public CoverageMappingCallable(FhirVersion version, CoverageMapping coverageMapping, BFDClient bfdClient, Ab2dEnvironment appEnv) {
        this.coverageMapping = coverageMapping;
        this.bfdClient = bfdClient;
        this.completed = new AtomicBoolean(false);
        this.appEnv = appEnv;
        this.year = getCorrectedYear(this.appEnv, coverageMapping.getPeriod().getYear());
        this.version = version;
    }

    public boolean isCompleted() {
        return completed.get();
    }

    public CoverageMapping getCoverageMapping() {
        return coverageMapping;
    }

    /**
     * Execute queries against BFD and collect enrollment result into a single object.
     *
     * In the past there have been significant issues related to the values of the enrollment returned by BFD.
     * These issues have led to adding a bunch of logs in here for most steps in the enrollment process.
     *
     * These logs include:
     *      - log each bundle received with the page number
     *      - listing all links to additional pages returned by BFD
     *      - log last page received
     *      - record statistics regarding potentially missing things like MBIs, beneficiary ids, distribution of reference
     *          years on the patient, and past reference years which are not expected.
     *
     * Steps
     *      - Set a unique id for the job as a header to BFD for monitoring purposes
     *      - Get the first page of enrollment results and process those results
     *      - Loop over the remaining pages of results and query until none are left
     *      - Add the results to the CoverageMapping object
     *      - Mark the search as completed
     *      - Log statistics concerning enrollment pulled
     *      - Remove the unique id header used for BFD
     *
     * @throws Exception on any failure, before the exception is thrown it will be logged
     */
    @Trace(metricName = "EnrollmentRequest", dispatcher = true)
    @Override
    public CoverageMapping call() {

        int month = coverageMapping.getPeriod().getMonth();
        String contractNumber = coverageMapping.getContractNumber();

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
            log.debug("finished reading [{}] Set<Identifiers>resources", patientIds.size());

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

    /**
     * Filter out patients with unknown reference years or unexpected reference years.
     *
     * The reference year on a patient should be equal to or greater than the year of enrollment we are searching for.
     * Sometimes the reference year will not match the {@link gov.cms.ab2d.coverage.model.CoveragePeriod}
     * but as long as it is greater than or equal it is okay. Patients with reference years before the
     * {@link gov.cms.ab2d.coverage.model.CoveragePeriod#getYear()} are discarded.
     */
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
     * Given a patient, extract patient ids and package into an {@link Identifiers} object.
     *
     * Record metrics on the identifiers found.
     *
     * Steps
     *      - Find all types of identifiers present on the patient
     *      - Check that an internal beneficiary id is present for the patient.
     *          - If not present do not add the patient to the list
     *      - Check that a current MBI is present
     *      - Check whether historical MBIs are present
     *      - Create identifiers using identifiers that were present in the patient object
     *
     * @param patient - the patient id
     * @return the identifiers present if a beneficiary id is present on the patient
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
     * Given a contract number, month, and year, call BFDs API to begin paging through all patients
     * associated with that contract for that {@link gov.cms.ab2d.coverage.model.CoveragePeriod}
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
     * @param contract - the specified contract
     * @param coverageYear - the specified coverage year in the coverage search
     * @return if we're in sandbox, return the synthetic data year unless it's the new Synthea data which can use
     * the correct year
     */
    int getCorrectedYear(Ab2dEnvironment appEnv, int coverageYear) {

        // Synthea contracts use realistic enrollment reference years so only original
        // synthetic contracts need to have the year modified
        if (appEnv == Ab2dEnvironment.SANDBOX) {
            return SYNTHETIC_DATA_YEAR;
        }

        return coverageYear;
    }
}
