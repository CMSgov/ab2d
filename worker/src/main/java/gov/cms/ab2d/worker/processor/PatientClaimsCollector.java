package gov.cms.ab2d.worker.processor;

import com.newrelic.api.agent.NewRelic;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.coverage.model.CoverageSummary;
import gov.cms.ab2d.fhir.BundleUtils;
import gov.cms.ab2d.fhir.EobUtils;
import gov.cms.ab2d.filter.ExplanationOfBenefitTrimmer;
import gov.cms.ab2d.filter.FilterEob;
import gov.cms.ab2d.worker.util.FhirUtils;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * Collect and filter claims based on AB2D business requirements and allow documenting the results of all actions.
 *
 * Relevant classes influencing filtering and behavior:
 *      - {@link gov.cms.ab2d.coverage.model.CoverageSummary} dates that beneficiary is a member of the contract and list of MBIs
 *      - {@link ExplanationOfBenefitTrimmer#getBenefit} strip fields that AB2D should not provide based on {@link gov.cms.ab2d.fhir.FhirVersion}
 *      - {@link EobUtils#isPartD} remove claims that are PartD
 *      - {@link FhirUtils} add MBIs to a claim
 */
@Slf4j
public class PatientClaimsCollector {

    private static final String EOB_REQUEST_EVENT = "EobBundleRequests";

    private final PatientClaimsRequest claimsRequest;
    private final Date attestationDate;
    private final Date earliestDate;

    private int bundles;
    private int rawEobs;

    private final List<IBaseResource> eobs;

    public PatientClaimsCollector(PatientClaimsRequest claimsRequest, Date earliestDate) {
        this.claimsRequest = claimsRequest;

        long epochMilli = claimsRequest.getAttTime().toInstant().toEpochMilli();
        this.attestationDate = new Date(epochMilli);
        this.earliestDate = earliestDate;

        this.eobs = new ArrayList<>();
    }

    public List<IBaseResource> getEobs() {
        return eobs;
    }

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
     * {@link gov.cms.ab2d.common.model.Contract.ContractType#CLASSIC_TEST}
     *
     * @param bundle response from BFD containing a list of claims for a specific requested patient
     */
    public void filterAndAddEntries(IBaseBundle bundle, CoverageSummary patient) {

        // Skip if bundle is missing for some reason
        if (bundle == null) {
            return;
        }

        bundles += 1;

        // Returns null if bundle is null
        List<IBaseBackboneElement> bundleEntries = BundleUtils.getEntries(bundle);
        if (bundleEntries == null) {
            log.error("bundle entries not found for bundle");
            return;
        }

        rawEobs += bundleEntries.size();

        // Perform filtering actions
        BundleUtils.getEobResources(bundleEntries).stream()
                // Filter by date unless contract is an old synthetic data contract, part D or attestation time is null
                // Filter out data
                .filter(resource -> FilterEob.filter(resource, patient.getDateRanges(), earliestDate,
                        attestationDate, claimsRequest.getContractType() == Contract.ContractType.CLASSIC_TEST).isPresent())
                // Filter out unnecessary fields
                .map(resource -> ExplanationOfBenefitTrimmer.getBenefit(resource))
                // Make sure patients are the same
                .filter(resource -> matchingPatient(resource, patient))
                // Make sure update date is after since date
                .filter(this::afterSinceDate)
                // Add MBIs to the claim
                .peek(eob -> FhirUtils.addMbiIdsToEobs(eob, patient, claimsRequest.getVersion()))
                // compile the list
                .forEach(eobs::add);
    }

    /**
     * We want to make sure that the last updated date is not before the _since
     * date. This should never happen, but it's a sanity check on BFD in case they
     * want to do this to help people who've made missed out on data and ignore
     * the _since date
     *
     * @param resource - the EOB
     * @return true if the lastUpdated date is after the since date
     */
    boolean afterSinceDate(IBaseResource resource) {
        OffsetDateTime sinceTime = claimsRequest.getSinceTime();
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
    private boolean matchingPatient(IBaseResource benefit, CoverageSummary patient) {

        Long patientId = EobUtils.getPatientId(benefit);
        if (patientId == null || patient.getIdentifiers().getBeneficiaryId() != patientId) {
            log.error(patientId + " returned in EOB object, but does not match beneficiary id passed to the search");
            return false;
        }
        return true;
    }

    /**
     * Create custom NewRelic event and log it
     * @param since since date used if in use
     */
    public void logBundleEvent(OffsetDateTime since) {
        Map<String, Object> event = new HashMap<>();
        event.put("organization", claimsRequest.getOrganization());
        event.put("contract", claimsRequest.getContractNum());
        event.put("since", since);
        event.put("jobid", claimsRequest.getJob());
        event.put("bundles", bundles);
        event.put("raweobs", rawEobs);
        event.put("eobs", eobs.size());

        NewRelic.getAgent().getInsights().recordCustomEvent(EOB_REQUEST_EVENT, event);

        log.debug("Bundle - Total EOBs Received: {} - Results Returned After Filtering: {} ", rawEobs, eobs.size());
    }
}
