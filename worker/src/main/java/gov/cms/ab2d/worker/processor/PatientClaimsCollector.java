package gov.cms.ab2d.worker.processor;

import com.newrelic.api.agent.NewRelic;
import gov.cms.ab2d.common.util.FilterOutByDate;
import gov.cms.ab2d.common.util.fhir.FhirUtils;
import gov.cms.ab2d.fhir.BundleUtils;
import gov.cms.ab2d.fhir.EobUtils;
import gov.cms.ab2d.filter.ExplanationOfBenefitTrimmer;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
public class PatientClaimsCollector {

    private static final String EOB_REQUEST_EVENT = "EobBundleRequests";

    private final PatientClaimsRequest claimsRequest;
    private final boolean skipBillablePeriodCheck;
    private final Date attestationDate;
    private final Date earliestDate;

    private int bundles;
    private int rawEobs;

    private final List<IBaseResource> eobs;

    public PatientClaimsCollector(PatientClaimsRequest claimsRequest, boolean skipBillablePeriodCheck, Date earliestDate) {
        this.claimsRequest = claimsRequest;
        this.skipBillablePeriodCheck = skipBillablePeriodCheck;

        long epochMilli = claimsRequest.getAttTime().toInstant().toEpochMilli();
        this.attestationDate = new Date(epochMilli);
        this.earliestDate = earliestDate;

        this.eobs = new ArrayList<>();
    }

    public List<IBaseResource> getEobs() {
        return eobs;
    }

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
                // Filter by date
                .filter(resource -> skipBillablePeriodCheck || FilterOutByDate.valid(resource, attestationDate, earliestDate, claimsRequest.getCoverageSummary().getDateRanges()))
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
