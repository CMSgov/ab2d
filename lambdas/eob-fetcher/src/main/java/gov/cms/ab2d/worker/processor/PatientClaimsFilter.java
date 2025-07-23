package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.fetcher.model.PatientCoverage;
import gov.cms.ab2d.fhir.BundleUtils;
import gov.cms.ab2d.fhir.EobUtils;
import gov.cms.ab2d.fhir.ExtensionUtils;
import gov.cms.ab2d.fhir.FhirVersion;
import gov.cms.ab2d.filter.ExplanationOfBenefitTrimmer;
import gov.cms.ab2d.filter.FilterEob;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Collect and filter claims based on AB2D business requirements and allow documenting the results of all actions.
 *
 * Relevant classes influencing filtering and behavior:
 *      - {@link ExplanationOfBenefitTrimmer#getBenefit} strip fields that AB2D should not provide based on {@link gov.cms.ab2d.fhir.FhirVersion}
 *      - {@link EobUtils#isPartD} remove claims that are PartD
 */
@Slf4j
public class PatientClaimsFilter {

    /**
     * Filter out EOBs not meeting requirements and add on MBIs to remaining claims
     *
     * This method implements business requirements for AB2D. Do not change this method without consulting
     * multiple people concerning the implications.
     *
     * Filters include:
     *      - filter out if billable period does not match a date range where contracts were enrolled
     *      - filter out fields that AB2D is not allowed to report with claims data
     *      - filter out if eob belongs to Part D
     *      - filter out if eob patient id does not match original request patient id
     *
     * Billable period filters are applied to all contract types except for
     *
     * @param bundle - response from BFD containing a list of claims for a specific requested patient
     * @param patient - the patient identifier information
     * @param attestationDate - the date the contract was attested
     * @param isSkipBillablePeriodCheck - for test data, do we need to skip any date filtering
     * @param since - The _since date specified in the api job creation request
     * @param version - the FHIR version to help with parsing the data
     */
    public static List<IBaseResource> filterEntries(IBaseBundle bundle, PatientCoverage patient, Date attestationDate,
                                                    boolean isSkipBillablePeriodCheck, OffsetDateTime since,
                                                    FhirVersion version) {

        // Skip if bundle is missing for some reason
        if (bundle == null) {
            return new ArrayList<>();
        }

        // Returns if bundle entries is null
        List<IBaseBackboneElement> bundleEntries = BundleUtils.getEntries(bundle);
        if (bundleEntries == null) {
            log.error("bundle entries not found for bundle");
            return new ArrayList<>();
        }

        // Perform filtering actions
        return BundleUtils.getEobResources(bundleEntries).stream()
                // Filter by date unless contract is an old synthetic data contract, part D or attestation time is null
                // Filter out data
                .filter(resource -> FilterEob.filter(resource, patient.getDateRanges(), getEarliest(),
                        attestationDate, isSkipBillablePeriodCheck).isPresent())
                // Filter out unnecessary fields
                .map(ExplanationOfBenefitTrimmer::getBenefit)
                // Make sure patients are the same
                .filter(resource -> matchingPatient(resource, patient.getBeneId()))
                // Make sure update date is after since date
                .filter(eob -> afterSinceDate(eob, since))
                .peek(eob -> addMbiIdsToEobs(eob, patient.getCurrentMBI(), patient.getHistoricMBIs(), version))
                .collect(Collectors.toList());
    }

    /**
     * The earliest date we are able to return data for is 1/1/2020
     *
     * @return that date
     */
    static Date getEarliest() {
        LocalDateTime d = LocalDateTime.of(2020, 1, 1, 0, 0, 0, 0);
        return new Date(d.toInstant(ZoneOffset.UTC).toEpochMilli());
    }

    /**
     * We want to make sure that the last updated date is not before the _since
     * date. This should never happen, but it's a sanity check on BFD in case they
     * want to do this to help people who've made missed out on data and ignore
     * the _since date
     *
     * @param resource - the EOB
     * @param sinceTime - the time the user specified as the _since date
     * @return true if the lastUpdated date is after the _since date
     */
    static boolean afterSinceDate(IBaseResource resource, OffsetDateTime sinceTime) {
        if (sinceTime == null) {
            return true;
        }
        Date lastUpdated = resource.getMeta().getLastUpdated();
        if (lastUpdated == null) {
            return false;
        }
        return sinceTime.toInstant().toEpochMilli() < lastUpdated.getTime();
    }

    /**
     * returns true if the patient is a valid member of a contract, false otherwise. If either value is empty,
     * it returns false
     *
     * @param benefit  - The benefit to check
     * @return true if this patient is a member of the correct contract
     */
    static boolean matchingPatient(IBaseResource benefit, long requestedPatientId) {

        Long patientId = EobUtils.getPatientId(benefit);
        if (patientId == null || requestedPatientId != patientId) {
            log.error(patientId + " returned in EOB object, but does not match beneficiary id passed to the search");
            return false;
        }
        return true;
    }

    /**
     * Add the patient MBIs as an extension to the ExplanationOfBenefit (since this is the only way the PDP
     * can do the mapping between their patients and ours)
     *
     * @param eob - the EOB
     * @param mbi - the current MBI
     * @param historicMbis - any historic MBI
     * @param version - the FHIR version
     */
    static void addMbiIdsToEobs(IBaseResource eob, String mbi, String[] historicMbis, FhirVersion version) {
        if (eob == null) {
            return;
        }

        // Add extensions only if beneficiary id is present and known to memberships
        Long benId = EobUtils.getPatientId(eob);
        if (benId != null) {

            // Add each mbi to each eob
            if (mbi != null) {
                IBase currentMbiExtension = ExtensionUtils.createMbiExtension(mbi, true, version);
                ExtensionUtils.addExtension(eob, currentMbiExtension, version);
            }

            if (historicMbis != null && historicMbis.length > 0) {
                for (String historicMbi : historicMbis) {
                    IBase mbiExtension = ExtensionUtils.createMbiExtension(historicMbi, false, version);
                    ExtensionUtils.addExtension(eob, mbiExtension, version);
                }
            }
        }
    }
}
