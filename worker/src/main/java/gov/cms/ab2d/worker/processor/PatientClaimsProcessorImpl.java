package gov.cms.ab2d.worker.processor;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.CoverageSummary;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.BeneficiarySearchEvent;
import gov.cms.ab2d.fhir.BundleUtils;
import gov.cms.ab2d.fhir.EobUtils;
import gov.cms.ab2d.filter.ExplanationOfBenefitTrimmer;
import gov.cms.ab2d.common.util.FilterOutByDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static gov.cms.ab2d.common.util.Constants.SINCE_EARLIEST_DATE;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

@Slf4j
@Component
@RequiredArgsConstructor
public class PatientClaimsProcessorImpl implements PatientClaimsProcessor {

    private final BFDClient bfdClient;
    private final LogManager logManager;

    @Value("${claims.skipBillablePeriodCheck}")
    private boolean skipBillablePeriodCheck;
    @Value("${bfd.earliest.data.date:01/01/2020}")
    private String startDate;
    @Value("${bfd.earliest.data.date.special.contracts}")
    private String startDateSpecialContracts;
    @Value("#{'${bfd.special.contracts}'.split(',')}")
    private List<String> specialContracts;

    private static final OffsetDateTime START_CHECK = OffsetDateTime.parse(SINCE_EARLIEST_DATE, ISO_DATE_TIME);

    private static final String EOB_ATTRIBUTE = "eobrequest.duration";
    private static final String EOB_REQUEST_EVENT = "EobBundleRequests";

    /**
     * Process the retrieval of patient explanation of benefit objects and write them
     * to a file using the writer
     */
    @Trace(metricName = "EOBRequests", async = true)
    @Async("patientProcessorThreadPool")
    public Future<EobSearchResult> process(PatientClaimsRequest request) {
        final Token token = request.getToken();
        token.link();
        EobSearchResult result = new EobSearchResult();
        result.setJobId(request.getJob());
        result.setContractNum(request.getContractNum());
        try {
            result.setEobs(getEobBundleResources(request));
        } catch (Exception ex) {
            return AsyncResult.forExecutionException(ex);
        }
        token.expire();
        return new AsyncResult<>(result);
    }

    private List<IBaseResource> getEobBundleResources(PatientClaimsRequest request) {
        CoverageSummary patient = request.getCoverageSummary();
        String beneficiaryId = patient.getIdentifiers().getBeneficiaryId();
        OffsetDateTime attTime = request.getAttTime();

        OffsetDateTime start = OffsetDateTime.now();
        IBaseBundle eobBundle;

        OffsetDateTime sinceTime = getSinceTime(request);

        Instant startEobRequest = Instant.now();

        try {

            BFDClient.BFD_BULK_JOB_ID.set(request.getJob());
            eobBundle = bfdClient.requestEOBFromServer(request.getVersion(), patient.getIdentifiers().getBeneficiaryId(), sinceTime);
            logSuccessful(request, beneficiaryId, start);

        } catch (Exception ex) {
            logError(request, beneficiaryId, start, ex);
            throw ex;
        } finally {
            BFDClient.BFD_BULK_JOB_ID.remove();
        }

        final List<IBaseBackboneElement> entries = BundleUtils.getEntries(eobBundle);
        int bundles = 1;

        final List<IBaseResource> resources = extractResources(request.getContractNum(), entries, patient.getDateRanges(), attTime);

        while (BundleUtils.getNextLink(eobBundle) != null) {
            eobBundle = bfdClient.requestNextBundleFromServer(request.getVersion(), eobBundle);

            final List<IBaseBackboneElement> nextEntries = BundleUtils.getEntries(eobBundle);
            bundles += 1;

            resources.addAll(extractResources(request.getContractNum(), nextEntries, patient.getDateRanges(), attTime));
        }

        Instant endEobRequest = Instant.now();

        // Record details of EOB request for analysis
        Map<String, Object> bundleEvent = bundleEvent(request, bundles,
                BundleUtils.getTotal(eobBundle), entries.size(), sinceTime);
        NewRelic.getAgent().getInsights().recordCustomEvent(EOB_REQUEST_EVENT, bundleEvent);

        // Record how long eob request took
        long requestDurationMillis = Duration.between(startEobRequest, endEobRequest).toMillis();
        NewRelic.getAgent().getTracedMethod().addCustomAttribute(EOB_ATTRIBUTE, requestDurationMillis);

        log.debug("Bundle - Total: {} - Entries: {} ", BundleUtils.getTotal(eobBundle), entries.size());

        return resources;
    }

    /**
     * Determine what since date to use if any. If attestation time is before the earliest date that we can use the
     * _since parameter for, then return null.
     *
     * @param request patient claims request which may contain a since time or attestation time to use
     */
    private OffsetDateTime getSinceTime(PatientClaimsRequest request) {
        OffsetDateTime sinceTime = null;
        if (request.getSinceTime() == null) {
            if (request.getAttTime().isAfter(START_CHECK)) {
                sinceTime = request.getAttTime();
            }
        } else {
            sinceTime = request.getSinceTime();
        }
        return sinceTime;
    }

    private void logSuccessful(PatientClaimsRequest request, String beneficiaryId, OffsetDateTime start) {
        logManager.log(LogManager.LogType.KINESIS,
                new BeneficiarySearchEvent(request.getOrganization(), request.getJob(), request.getContractNum(),
                        start, OffsetDateTime.now(),
                        beneficiaryId,
                        "SUCCESS"));
    }

    private void logError(PatientClaimsRequest request, String beneficiaryId, OffsetDateTime start, Exception ex) {
        logManager.log(LogManager.LogType.KINESIS,
                new BeneficiarySearchEvent(request.getOrganization(), request.getJob(), request.getContractNum(),
                        start, OffsetDateTime.now(),
                        beneficiaryId,
                        "ERROR: " + ex.getMessage()));
    }

    private Date getStartDate(String contract) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

        String dateToUse = startDate;
        if (isContractSpecial(contract)) {
            dateToUse = startDateSpecialContracts;
        }
        Date date;
        try {
            date = sdf.parse(dateToUse);
        } catch (ParseException e) {
            LocalDateTime d = LocalDateTime.of(2020, 1, 1, 0, 0, 0, 0);
            date = new Date(d.toInstant(ZoneOffset.UTC).toEpochMilli());
        }
        return date;
    }

    private boolean isContractSpecial(String contract) {
        return this.specialContracts != null && !this.specialContracts.isEmpty() && specialContracts.contains(contract);
    }

    List<IBaseResource> extractResources(String contractNum,
                                     List<IBaseBackboneElement> entries,
                                     final List<FilterOutByDate.DateRange> dateRanges,
                                     OffsetDateTime attTime) {
        if (attTime == null) {
            return new ArrayList<>();
        }
        long epochMilli = attTime.toInstant().toEpochMilli();
        Date attDate = new Date(epochMilli);
        final Date earliestDate = getStartDate(contractNum);
        return BundleUtils.getEobResources(entries).stream()
                // Filter by date
                .filter(resource -> skipBillablePeriodCheck || FilterOutByDate.valid(resource, attDate, earliestDate, dateRanges))
                // filter it
                .map(resource -> ExplanationOfBenefitTrimmer.getBenefit(resource))
                // Remove any empty values
                .filter(Objects::nonNull)
                // Remove Plan D
                .filter(resource -> !EobUtils.isPartD(resource))
                // compile the list
                .collect(Collectors.toList());
    }

    /**
     * Create custom NewRelic event
     * @param request patient claims request
     * @param bundles number of bundles pulled from BFD
     * @param rawEobs number of eobs returned before filtering
     * @param filteredEobs number of eobs returned after filtering
     * @param since since date used if in use
     * @return custom NewRelic event as a map
     */
    private Map<String, Object> bundleEvent(PatientClaimsRequest request, int bundles, int rawEobs, int filteredEobs, OffsetDateTime since) {
        Map<String, Object> event = new HashMap<>();
        event.put("organization", request.getOrganization());
        event.put("contract", request.getContractNum());
        event.put("since", since);
        event.put("jobid", request.getJob());
        event.put("bundles", bundles);
        event.put("raweobs", rawEobs);
        event.put("eobs", filteredEobs);

        return event;
    }
}
