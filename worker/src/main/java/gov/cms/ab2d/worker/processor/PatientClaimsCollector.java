package gov.cms.ab2d.worker.processor;

import com.newrelic.api.agent.NewRelic;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.util.FilterOutByDate;
import gov.cms.ab2d.worker.util.FhirUtils;
import gov.cms.ab2d.fhir.BundleUtils;
import gov.cms.ab2d.fhir.EobUtils;
import gov.cms.ab2d.filter.ExplanationOfBenefitTrimmer;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * Collect and filter claims based on AB2D business requirements and allow documenting the results of all actions.
 *
 * Relevant classes influencing filtering and behavior:
 *      - {@link gov.cms.ab2d.common.model.CoverageSummary} dates that beneficiary is a member of the contract and list of MBIs
 *      - {@link FilterOutByDate#valid} method filtering out claims from periods when beneficiary was not a member
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
    public void filterAndAddEntries(IBaseBundle bundle) {

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
                // Filter by date unless contract is an old synthetic data contract
                .filter(resource -> claimsRequest.getContractType() == Contract.ContractType.CLASSIC_TEST || FilterOutByDate.valid(resource, attestationDate, earliestDate, claimsRequest.getCoverageSummary().getDateRanges()))
                // filter it
                .map(ExplanationOfBenefitTrimmer::getBenefit)
                // Remove any empty values
                .filter(Objects::nonNull)
                // Remove Plan D
                .filter(resource -> !EobUtils.isPartD(resource))
                .filter(this::matchingPatient)
                .peek(eob -> FhirUtils.addMbiIdsToEobs(eob, claimsRequest.getCoverageSummary(), claimsRequest.getVersion()))
                // compile the list
                .forEach(eobs::add);
    }

    /**
     * returns true if the patient is a valid member of a contract, false otherwise. If either value is empty,
     * it returns false
     *
     * @param benefit  - The benefit to check
     * @return true if this patient is a member of the correct contract
     */
    private boolean matchingPatient(IBaseResource benefit) {

        Long patientId = EobUtils.getPatientId(benefit);
        if (patientId == null || claimsRequest.getCoverageSummary().getIdentifiers().getBeneficiaryId() != patientId) {
            log.error(patientId + " returned in EOB, but does not match eob of");
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
