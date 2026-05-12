package gov.cms.ab2d.worker.processor;

import ca.uhn.fhir.parser.IParser;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import gov.cms.ab2d.aggregator.ClaimsStream;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.coverage.model.CoverageSummary;
import gov.cms.ab2d.eventclient.clients.EventClient;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.eventclient.events.BeneficiarySearchEvent;
import gov.cms.ab2d.eventclient.events.FileEvent;
import gov.cms.ab2d.eventclient.events.MetricsEvent;
import gov.cms.ab2d.fhir.BundleUtils;
import gov.cms.ab2d.fhir.FhirVersion;
import gov.cms.ab2d.worker.config.SearchConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;

import static gov.cms.ab2d.aggregator.FileOutputType.DATA;
import static gov.cms.ab2d.aggregator.FileOutputType.ERROR;
import static gov.cms.ab2d.common.util.Constants.SINCE_EARLIEST_DATE_TIME;
import static gov.cms.ab2d.common.util.PropertyConstants.EOB_V3_IN_PLACE;
import static gov.cms.ab2d.worker.processor.BfdRequestTracking.BfdRequestType.REQUEST_EOB;
import static gov.cms.ab2d.worker.processor.BfdRequestTracking.BfdRequestType.REQUEST_NEXT_BUNDLE;

@Slf4j
@Component
@RequiredArgsConstructor
public class PatientClaimsProcessorImpl implements PatientClaimsProcessor {

    // Set to false by default to prevent excess logging; enable explicitly for testing
    // TODO revert back to false after testing in ephemeral environment
    public static final boolean TIME_BFD_REQUESTS = true;

    // Null when the JVM does not support per-thread allocation tracking
    private static final com.sun.management.ThreadMXBean THREAD_BEAN;
    static {
        com.sun.management.ThreadMXBean bean = null;
        try {
            java.lang.management.ThreadMXBean tb = ManagementFactory.getThreadMXBean();
            if (tb instanceof com.sun.management.ThreadMXBean sunBean
                    && sunBean.isThreadAllocatedMemorySupported()
                    && sunBean.isThreadAllocatedMemoryEnabled()) {
                bean = sunBean;
            }
        } catch (Exception ignored) { }
        THREAD_BEAN = bean;
    }

    private final BFDClient bfdClient;
    private final SQSEventClient logManager;
    private final SearchConfig searchConfig;
    private final PropertiesService propertiesService;

    @Value("${bfd.earliest.data.date:01/01/2020}")
    private String earliestDataDate;

    @Value("${bfd.retry.backoffDelay:250}")
    private int bfdTimeout;

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
            if (anyErrors != null && !anyErrors.isEmpty()) {
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
        val bfdRequestTracking = TIME_BFD_REQUESTS
                ? new BfdRequestTracking(request.getJob())
                : BfdRequestTracking.NOOP;

        boolean useInPlace   = propertiesService.isToggleOn(EOB_V3_IN_PLACE, false);
        long    allocBefore  = threadAllocBytes();
        long[]  gcBefore     = gcSnapshot();
        long    nsBefore     = System.nanoTime();

        File file = null;
        String anyErrors = null;
        try (ClaimsStream stream = new ClaimsStream(request.getJob(), request.getEfsMount(), DATA,
                searchConfig.getStreamingDir(), searchConfig.getFinishedDir(), searchConfig.getBufferSize())) {
            file = stream.getFile();
            logManager.sendLogs(new FileEvent(request.getOrganization(), request.getJob(), stream.getFile(), FileEvent.FileStatus.OPEN));
            for (CoverageSummary patient : request.getCoverageSummary()) {
                List<IBaseResource> eobs = getEobBundleResources(request, patient, bfdRequestTracking);
                anyErrors = writeOutResource(fhirVersion, update, eobs, stream);
                update.incPatientProcessCount();
            }
        } finally {
            logManager.sendLogs(new FileEvent(request.getOrganization(), request.getJob(), file, FileEvent.FileStatus.CLOSE));
            bfdRequestTracking.summarizeResponseTimes();
        }

        long   elapsedMs        = (System.nanoTime() - nsBefore) / 1_000_000;
        long   allocDeltaKB     = allocBefore >= 0 ? (threadAllocBytes() - allocBefore) / 1024 : -1;
        long[] gcAfter          = gcSnapshot();
        long   gcEvents         = gcAfter[0] - gcBefore[0];
        long   gcTimeDeltaMs    = gcAfter[1] - gcBefore[1];
        int    patients         = request.getCoverageSummary().size();
        long   heapUsedMB       = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);

        log.info("eob.processing.metrics job={} contract={} patients={} inPlace={} "
                        + "allocKB={} allocKBPerPatient={} gcEvents={} gcTimeDeltaMs={} heapUsedMB={} elapsedMs={}",
                request.getJob(), request.getContractNum(), patients, useInPlace,
                allocDeltaKB,
                patients > 0 && allocDeltaKB >= 0 ? allocDeltaKB / patients : -1,
                gcEvents, gcTimeDeltaMs, heapUsedMB, elapsedMs);

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
    private List<IBaseResource> getEobBundleResources(
            PatientClaimsRequest request,
            CoverageSummary patient,
            BfdRequestTracking bfdRequestTracking
    ) {
        OffsetDateTime requestStartTime = OffsetDateTime.now();

        Date earliestDate = getEarliestDataDate();

        // Aggregate claims into a single list
        boolean useInPlace = propertiesService.isToggleOn(EOB_V3_IN_PLACE, false);
        PatientClaimsCollector collector = new PatientClaimsCollector(request, earliestDate, useInPlace);

        final long patientIdentifier = (request.getVersion() == FhirVersion.R4V3)
            ? patient.getIdentifiers().getPatientIdV3()
            : patient.getIdentifiers().getBeneficiaryId();

        IBaseBundle eobBundle;

        // Guarantee that since and until dates provided with job don't violate AB2D requirements
        OffsetDateTime sinceTime = getSinceTime(request);
        OffsetDateTime untilTime = getUntilTime(request, sinceTime);
        List<String> serviceDates = request.getServiceDates();


        boolean verboseBfdLogging = propertiesService.isToggleOn("bfd.logging.verbose", false);
        try {

            // Set header for requests so BFD knows where this request originated from
            BFDClient.BFD_BULK_JOB_ID.set(request.getJob());

            // Make first request and begin looping over remaining pages
            eobBundle = bfdRequestTracking.executeRequest(
                REQUEST_EOB,
                () -> {

                    if (verboseBfdLogging) {
                        var rowNumber = -1L;
                        if (patient.getIdentifiers().isV3()) {
                            rowNumber = patient.getIdentifiers().getRowNumberV3();
                        }

                        final Segment bfdSegment = NewRelic.getAgent().getTransaction().startSegment("BFD Call for patient with patient ID " + patientIdentifier +
                                " using since " + sinceTime + " and until " + untilTime + " and serviceDates " + serviceDates);
                        bfdSegment.setMetricName("RequestEOB");

                        val bfdStart = System.nanoTime();
                        final byte[] response = bfdClient.requestEOBFromServerWithoutParseBundle(request.getVersion(), patientIdentifier, sinceTime, untilTime, serviceDates, request.getContractNum());
                        val bfdEnd = System.nanoTime();
                        final IBaseBundle bundle = bfdClient.parseBundle(request.getVersion(), response);
                        val parseBundleEnd = System.nanoTime();
                        logBfdVerbose(bfdStart, bfdEnd, parseBundleEnd, rowNumber, request.getJob());
                        bfdSegment.end();
                        return bundle;
                    } else {
                        return bfdClient.requestEOBFromServer(request.getVersion(), patientIdentifier, sinceTime, untilTime, serviceDates, request.getContractNum());
                    }
                }
            );
            collector.filterAndAddEntries(eobBundle, patient);

            while (BundleUtils.getNextLink(eobBundle) != null) {
                val currentEobBundle = eobBundle;
                eobBundle = bfdRequestTracking.executeRequest(
                    REQUEST_NEXT_BUNDLE,
                    () -> bfdClient.requestNextBundleFromServer(request.getVersion(), currentEobBundle, request.getContractNum())
                );
                collector.filterAndAddEntries(eobBundle, patient);
            }

            // Log request to Kinesis and NewRelic
            logSuccessful(request, patientIdentifier, requestStartTime);

            collector.logBundleEvent(sinceTime, untilTime);

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
            logError(request, patientIdentifier, requestStartTime, ex);
            throw ex;
        } finally {
            BFDClient.BFD_BULK_JOB_ID.remove();
        }
    }

    private static void logBfdVerbose(long bfdStart, long bfdEnd, long parseBundleEnd, long rowNumber, String jobId) {
        val bfdResponseMs = (bfdEnd - bfdStart) / 1_000_000;
        val parseBundleMs = (parseBundleEnd - bfdEnd) / 1_000_000;
        if (rowNumber > 0) {
            log.info("requestEOBFromServer stats; Job: {}; Request: {}ms; parseBundle: {}ms; rowNumber: {}", jobId, bfdResponseMs, parseBundleMs, rowNumber);
        } else {
            log.info("requestEOBFromServer stats; Job: {}; Request: {}ms; parseBundle: {}ms", jobId, bfdResponseMs, parseBundleMs);
        }
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
            if (request.getAttTime().isAfter(SINCE_EARLIEST_DATE_TIME) || request.getAttTime().isEqual(SINCE_EARLIEST_DATE_TIME)) {
                sinceTime = request.getAttTime();
            }
        } else {

            if (sinceTime.isBefore(request.getAttTime())) {
                sinceTime = request.getAttTime();
            }

            // Should not be possible but just in case
            if (sinceTime.isBefore(SINCE_EARLIEST_DATE_TIME)) {
                sinceTime = null;
            }
        }

        return sinceTime;
    }

    private OffsetDateTime getUntilTime(PatientClaimsRequest request, OffsetDateTime updatedSinceTime) {
        OffsetDateTime untilTime = request.getUntilTime();
        if (untilTime == null || untilTime.isAfter(OffsetDateTime.now()))
            return null;

        if (updatedSinceTime != null && untilTime.isBefore(updatedSinceTime)) {
            return null;
        }

        return untilTime;
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

    private static long threadAllocBytes() {
        return THREAD_BEAN != null
                ? THREAD_BEAN.getThreadAllocatedBytes(Thread.currentThread().getId())
                : -1;
    }

    private static long[] gcSnapshot() {
        long count = 0;
        long timeMs = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            long c = gc.getCollectionCount();
            long t = gc.getCollectionTime();
            if (c >= 0) count  += c;
            if (t >= 0) timeMs += t;
        }
        return new long[]{count, timeMs};
    }

}
