package gov.cms.ab2d.worker.processor;

import ca.uhn.fhir.parser.IParser;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import gov.cms.ab2d.aggregator.ClaimsStream;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.coverage.model.CoverageSummary;
import gov.cms.ab2d.eventclient.clients.EventClient;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.eventclient.events.BeneficiarySearchEvent;
import gov.cms.ab2d.eventclient.events.FileEvent;
import gov.cms.ab2d.eventclient.events.MetricsEvent;
import gov.cms.ab2d.fhir.BundleUtils;
import gov.cms.ab2d.fhir.FhirVersion;
import gov.cms.ab2d.worker.config.SearchConfig;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;


import static gov.cms.ab2d.aggregator.FileOutputType.DATA;
import static gov.cms.ab2d.aggregator.FileOutputType.ERROR;
import static gov.cms.ab2d.common.util.Constants.SINCE_EARLIEST_DATE;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

@Slf4j
@Component
@RequiredArgsConstructor
public class PatientClaimsProcessorImpl implements PatientClaimsProcessor {

    private final BFDClient bfdClient;
    private final SQSEventClient logManager;
    private final SearchConfig searchConfig;

    @Value("${bfd.earliest.data.date:01/01/2020}")
    private String earliestDataDate;

    @Value("${bfd.retry.backoffDelay:250}")
    private int bfdTimeout;

    private static final OffsetDateTime START_CHECK = OffsetDateTime.parse(SINCE_EARLIEST_DATE, ISO_DATE_TIME);

    /**
     * Process the retrieval of patient explanation of benefit objects and return the result
     * for further post-processing
     */
    @Async("patientProcessorThreadPool")
    public Future<ProgressTrackerUpdate> process(PatientClaimsRequest request) {
        ProgressTrackerUpdate update = new ProgressTrackerUpdate();
        final Token token = request.getToken();
        token.link();
        FhirVersion fhirVersion = request.getVersion();
        try {
            String anyErrors = writeOutData(request, fhirVersion, update);
            if (anyErrors != null && anyErrors.length() > 0) {
                writeOutErrors(anyErrors, request);
            }
        } catch (Exception ex) {
            return AsyncResult.forExecutionException(ex);
        } finally {
            token.expire();
        }
        return AsyncResult.forValue(update);
    }

    String writeOutData(PatientClaimsRequest request, FhirVersion fhirVersion, ProgressTrackerUpdate update) throws IOException {
        File file = null;
        String anyErrors = null;
        try (ClaimsStream stream = new ClaimsStream(request.getJob(), request.getEfsMount(), DATA,
                searchConfig.getStreamingDir(), searchConfig.getFinishedDir(), searchConfig.getBufferSize())) {
            file = stream.getFile();
            logManager.sendLogs(new FileEvent(request.getOrganization(), request.getJob(), stream.getFile(), FileEvent.FileStatus.OPEN));
            for (CoverageSummary patient : request.getCoverageSummary()) {
                List<IBaseResource> eobs = getEobBundleResources(request, patient);
                anyErrors = writeOutResource(fhirVersion, update, eobs, stream);
                update.incPatientProcessCount();
            }
        } finally {
            logManager.sendLogs(new FileEvent(request.getOrganization(), request.getJob(), file, FileEvent.FileStatus.CLOSE));
        }
        return anyErrors;
    }

    void writeOutErrors(String anyErrors, PatientClaimsRequest request) {
        File errorFile = null;
        try (ClaimsStream stream = new ClaimsStream(request.getJob(), request.getEfsMount(), ERROR,
                searchConfig.getStreamingDir(), searchConfig.getFinishedDir(), searchConfig.getBufferSize())) {
            errorFile = stream.getFile();
            logManager.sendLogs(new FileEvent(request.getOrganization(), request.getJob(), stream.getFile(), FileEvent.FileStatus.OPEN));
            stream.write(anyErrors);
        } catch (IOException e) {
            log.error("Cannot log error to error file");
        } finally {
            logManager.sendLogs(new FileEvent(request.getOrganization(), request.getJob(), errorFile, FileEvent.FileStatus.CLOSE));
        }
    }

    @Trace(metricName = "EOBWriteToFile", dispatcher = true)
    private String writeOutResource(FhirVersion version, ProgressTrackerUpdate update, List<IBaseResource> eobs, ClaimsStream stream) {
        IParser parser = version.getJsonParser().setPrettyPrint(false);
        if (eobs == null) {
            log.debug("ignoring empty results because pulling eobs failed");
            return null;
        }
        if (eobs.isEmpty()) {
            return null;
        }
        int eobsWritten = 0;
        int eobsError = 0;

        update.incPatientsWithEobsCount();
        update.addEobFetchedCount(eobs.size());

        StringBuilder errorPayload = new StringBuilder();
        for (IBaseResource resource : eobs) {
            try {
                stream.write(parser.encodeResourceToString(resource) + System.lineSeparator());
                eobsWritten++;
            } catch (Exception ex) {
                log.warn("Encountered exception while processing job resources: {}", ex.getClass());
                String errMsg = ExceptionUtils.getRootCauseMessage(ex);
                IBaseResource operationOutcome = version.getErrorOutcome(errMsg);
                errorPayload.append(parser.encodeResourceToString(operationOutcome)).append(System.lineSeparator());
                eobsError++;
            }
        }

        update.addEobProcessedCount(eobsWritten);

        if (eobsError != 0) {
            return errorPayload.toString();
        }

        return errorPayload.toString();
    }

    /**
     * Begin requesting claims from BFD using the provided {@link PatientClaimsRequest}, page through
     * the resulting claims until none are left, filter claims not meeting requirements, and filter out fields
     * in claims that AB2D cannot provide.
     *
     * @param request request for claims from a single patient
     * @param patient a single patient
     * @return list of matching claims after filtering claims not meeting requirements and stripping fields that AB2D
     * cannot provide
     */
    @Trace(metricName = "EOBRequest", dispatcher = true)
    private List<IBaseResource> getEobBundleResources(PatientClaimsRequest request, CoverageSummary patient) {

        OffsetDateTime requestStartTime = OffsetDateTime.now();

        Date earliestDate = getEarliestDataDate();

        // Aggregate claims into a single list
        PatientClaimsCollector collector = new PatientClaimsCollector(request, earliestDate);

        long beneficiaryId = patient.getIdentifiers().getBeneficiaryId();

        IBaseBundle eobBundle;

        // Guarantee that since date provided with job doesn't violate AB2D requirements
        OffsetDateTime sinceTime = getSinceTime(request);

        try {

            // Set header for requests so BFD knows where this request originated from
            BFDClient.BFD_BULK_JOB_ID.set(request.getJob());

            // Make first request and begin looping over remaining pages
            eobBundle = bfdClient.requestEOBFromServer(request.getVersion(), patient.getIdentifiers().getBeneficiaryId(), sinceTime, request.getContractNum());
            collector.filterAndAddEntries(eobBundle, patient);

            // Only for S4802 and S5884 contracts (Centene and Humana support)

            while (BundleUtils.getNextLink(eobBundle) != null && isContinue(eobBundle, request)) {
                eobBundle = bfdClient.requestNextBundleFromServer(request.getVersion(), eobBundle, request.getContractNum());
                collector.filterAndAddEntries(eobBundle, patient);
            }

            // Log request to Kinesis and NewRelic
            logSuccessful(request, beneficiaryId, requestStartTime);
            collector.logBundleEvent(sinceTime);

            return collector.getEobs();
        } catch (Exception ex) {
            //When bfd call fails (excluding 404s) send a metrics event
            if (RuntimeException.class.equals(ex.getClass())) {
                logManager.sendLogs(MetricsEvent.builder()
                        .service("BFD")
                        .timeOfEvent(OffsetDateTime.now())
                        .eventDescription(String.format("BFD request failed after retyping %d times", bfdTimeout))
                        .stateType(MetricsEvent.State.CONTINUE)
                        .build());
            }
            logError(request, beneficiaryId, requestStartTime, ex);
            throw ex;
        } finally {
            BFDClient.BFD_BULK_JOB_ID.remove();
        }
    }

    //Centene Support
    boolean isContinue(IBaseResource resource, PatientClaimsRequest request) {
        OffsetDateTime sinceTime = request.getSinceTime();
        if (sinceTime == null) {
            return true;
        }
        Date lastUpdated = resource.getMeta().getLastUpdated();
        if (lastUpdated == null) {
            return false;
        }
        //AB2D-5892 (Sprint 3)Centene customer support to provide 2 year data
        if (request.getContractNum().equals("S4802") || request.getContractNum().equals("Z1001")) {
            return lastUpdated.getTime() < sinceTime.plusMonths(1).toInstant().toEpochMilli();
        }
        //AB2D-6167 Create customized script for Humana
        if (request.getContractNum().equals("S5884")){
            return lastUpdated.getTime() < sinceTime.plusMonths(4).toInstant().toEpochMilli();
        }
        return true;
    }

    /**
     * Determine what since date to use if any.
     * <p>
     * If since provided by user is null and attestation time is before the earliest date that we can use since for then return null.
     * <p>
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
        logManager.log(EventClient.LogType.KINESIS,
                new BeneficiarySearchEvent(request.getOrganization(), request.getJob(), request.getContractNum(),
                        start, OffsetDateTime.now(),
                        beneficiaryId,
                        "SUCCESS"));
    }

    private void logError(PatientClaimsRequest request, long beneficiaryId, OffsetDateTime start, Exception ex) {
        logManager.log(EventClient.LogType.KINESIS,
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
