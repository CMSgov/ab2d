package gov.cms.ab2d.worker.processor;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.CoverageSummary;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.BeneficiarySearchEvent;
import gov.cms.ab2d.fhir.BundleUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;

import static gov.cms.ab2d.common.util.Constants.SINCE_EARLIEST_DATE;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

@Slf4j
@Component
@RequiredArgsConstructor
public class PatientClaimsProcessorImpl implements PatientClaimsProcessor {

    private final BFDClient bfdClient;
    private final LogManager logManager;

    @Value("${bfd.earliest.data.date:01/01/2020}")
    private String earliestDataDate;

    private static final OffsetDateTime START_CHECK = OffsetDateTime.parse(SINCE_EARLIEST_DATE, ISO_DATE_TIME);

    /**
     * Process the retrieval of patient explanation of benefit objects and return the result
     * for further post-processing
     */
    @Trace(metricName = "EOBRequest", dispatcher = true)
    @Async("patientProcessorThreadPool")
    public Future<EobSearchResult> process(PatientClaimsRequest request) {
        final Token token = request.getToken();
        token.link();

        try {
            List<IBaseResource> eobs = getEobBundleResources(request);
            EobSearchResult result = new EobSearchResult(request.getJob(), request.getContractNum(), eobs);
            return new AsyncResult<>(result);
        } catch (Exception ex) {
            return AsyncResult.forExecutionException(ex);
        } finally {
            token.expire();
        }
    }

    /**
     * Begin requesting claims from BFD using the provided {@link PatientClaimsRequest}, page through
     * the resulting claims until none are left, filter claims not meeting requirements, and filter out fields
     * in claims that AB2D cannot provide.
     *
     * @param request request for claims from a single patient
     * @return list of matching claims after filtering claims not meeting requirements and stripping fields that AB2D
     * cannot provide
     */
    private List<IBaseResource> getEobBundleResources(PatientClaimsRequest request) {

        OffsetDateTime requestStartTime = OffsetDateTime.now();

        Date earliestDate = getEarliestDataDate();

        // Aggregate claims into a single list
        PatientClaimsCollector collector = new PatientClaimsCollector(request, earliestDate);

        CoverageSummary patient = request.getCoverageSummary();
        long beneficiaryId = patient.getIdentifiers().getBeneficiaryId();

        IBaseBundle eobBundle;

        // Guarantee that since date provided with job doesn't violate AB2D requirements
        OffsetDateTime sinceTime = getSinceTime(request);

        try {

            // Set header for requests so BFD knows where this request originated from
            BFDClient.BFD_BULK_JOB_ID.set(request.getJob());

            // Make first request and begin looping over remaining pages
            eobBundle = bfdClient.requestEOBFromServer(request.getVersion(), patient.getIdentifiers().getBeneficiaryId(), sinceTime);
            collector.filterAndAddEntries(eobBundle);

            while (BundleUtils.getNextLink(eobBundle) != null) {
                eobBundle = bfdClient.requestNextBundleFromServer(request.getVersion(), eobBundle);
                collector.filterAndAddEntries(eobBundle);
            }

            // Log request to Kinesis and NewRelic
            logSuccessful(request, beneficiaryId, requestStartTime);
            collector.logBundleEvent(sinceTime);

            return collector.getEobs();
        } catch (Exception ex) {
            logError(request, beneficiaryId, requestStartTime, ex);
            throw ex;
        } finally {
            BFDClient.BFD_BULK_JOB_ID.remove();
        }
    }

    /**
     * Determine what since date to use if any.
     *
     * If since provided by user is null and attestation time is before the earliest date that we can use since for then return null.
     *
     * If since provided is not null, check that since date is not before attestation time and that since date is not
     * before AB2D epoch.
     *
     * @param request patient claims request which may contain a since time or attestation time to use
     */
    private OffsetDateTime getSinceTime(PatientClaimsRequest request) {
        OffsetDateTime sinceTime = request.getSinceTime();

        if (sinceTime == null) {
            if (request.getAttTime().isAfter(START_CHECK) || request.getAttTime().isEqual(START_CHECK)) {
                sinceTime = request.getAttTime();
            }
        } else {

            if (sinceTime.isBefore(request.getAttTime())) {
                sinceTime = request.getAttTime();
            }

            // Should not be possible but just in case
            if (sinceTime.isBefore(START_CHECK)) {
                sinceTime = null;
            }
        }

        return sinceTime;
    }

    private void logSuccessful(PatientClaimsRequest request, long beneficiaryId, OffsetDateTime start) {
        logManager.log(LogManager.LogType.KINESIS,
                new BeneficiarySearchEvent(request.getOrganization(), request.getJob(), request.getContractNum(),
                        start, OffsetDateTime.now(),
                        beneficiaryId,
                        "SUCCESS"));
    }

    private void logError(PatientClaimsRequest request, long beneficiaryId, OffsetDateTime start, Exception ex) {
        logManager.log(LogManager.LogType.KINESIS,
                new BeneficiarySearchEvent(request.getOrganization(), request.getJob(), request.getContractNum(),
                        start, OffsetDateTime.now(),
                        beneficiaryId,
                        "ERROR: " + ex.getMessage()));
    }

    private Date getEarliestDataDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

        Date date;
        try {
            date = sdf.parse(earliestDataDate);
        } catch (ParseException e) {
            LocalDateTime d = LocalDateTime.of(2020, 1, 1, 0, 0, 0, 0);
            date = new Date(d.toInstant(ZoneOffset.UTC).toEpochMilli());
        }
        return date;
    }
}
