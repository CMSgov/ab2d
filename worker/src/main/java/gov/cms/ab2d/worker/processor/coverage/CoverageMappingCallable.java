package gov.cms.ab2d.worker.processor.coverage;

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

            IBaseBundle bundle = getBundle(contractNumber, month);
            patientIds.addAll(extractAndFilter(bundle));

            String availableLinks = BundleUtils.getAvailableLinks(bundle);
            log.info("retrieving contract membership for Contract {}-{}-{} bundle #{}, available links {}",
                    contractNumber, year, month, bundleNo, availableLinks);

            if (BundleUtils.getNextLink(bundle) == null) {
                log.info("retrieving contract membership for Contract {}-{}-{} bundle #{}, does not have a next link",
                        contractNumber, year, month, bundleNo);
            }

            while (BundleUtils.getNextLink(bundle) != null) {

                bundleNo += 1;

                log.info("retrieving contract membership for Contract {}-{}-{} bundle #{}",
                        contractNumber, year, month, bundleNo);

                bundle = bfdClient.requestNextBundleFromServer(bundle);

                availableLinks = BundleUtils.getAvailableLinksPretty(bundle);

                log.info("retrieving contract membership for Contract {}-{}-{} bundle #{}, available links {}",
                        contractNumber, year, month, bundleNo, availableLinks);

                if (BundleUtils.getNextLink(bundle) == null) {
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

    private Set<Identifiers> extractAndFilter(IBaseBundle bundle) {
        return getPatientStream(bundle)
                .filter(patient -> skipBillablePeriodCheck || filterByYear(patient))
                .map(this::extractPatientId)
                .filter(Objects::nonNull)
                .collect(toSet());
    }

    private Stream<IDomainResource> getPatientStream(IBaseBundle bundle) {
        try {
            return BundleUtils.getPatientStream(bundle, bfdClient.getVersion());
        } catch (Exception ex) {
            log.error("Unable to get patient stream", ex);
            return null;
        }
    }

    private boolean filterByYear(IDomainResource patient) {
        int year = ExtensionUtils.getReferenceYear(patient);
        if (year < 0) {
            log.error("patient returned without reference year violating assumptions");
            filteredByYear++;
            return false;
        }

        if (year != this.year) {
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
    private Identifiers extractPatientId(IDomainResource patient) {
        // Get patient beneficiary id
        // if not found eobs cannot be looked up so do not return a meaningful list
        String beneId = IdentifierUtils.getBeneId(patient);
        if (beneId == null || beneId.isEmpty()) {
            missingBeneId += 1;
            return null;
        }

        // Get current mbi if present or else log not present
        String currentMbi = IdentifierUtils.getCurrentMbi(patient);
        if (currentMbi == null) {
            missingCurrentMbi += 1;
        }

        Set<String> historicMbis = IdentifierUtils.getHistoricMbi(patient);
        if (!historicMbis.isEmpty()) {
            hasHistoricalMbi += 1;
        }

        return new Identifiers(beneId, currentMbi, historicMbis);
    }

    /**
     * given a contractNumber & a month, calls BFD API to find all patients active in the contract during the month
     *
     * @param contractNumber
     * @param month
     * @return a FHIR bundle of resources containing active patients
     */
    private IBaseBundle getBundle(String contractNumber, int month) {
        try {
            return bfdClient.requestPartDEnrolleesFromServer(contractNumber, month);
        } catch (Exception e) {
            final Throwable rootCause = ExceptionUtils.getRootCause(e);
            log.error("Error while calling for Contract-2-Bene API : {}", e.getMessage(), rootCause);
            throw new RuntimeException(rootCause);
        }
    }
}
