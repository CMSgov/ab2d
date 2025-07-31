package gov.cms.ab2d.worker.processor;

import ca.uhn.fhir.parser.IParser;
import com.newrelic.api.agent.Trace;
import gov.cms.ab2d.aggregator.ClaimsStream;
import gov.cms.ab2d.aggregator.FileOutputType;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.eventclient.clients.EventClient;
import gov.cms.ab2d.eventclient.events.BeneficiarySearchEvent;
import gov.cms.ab2d.eventclient.events.FileEvent;
import gov.cms.ab2d.fetcher.model.JobFetchPayload;
import gov.cms.ab2d.fetcher.model.PatientCoverage;
import gov.cms.ab2d.fhir.BundleUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static gov.cms.ab2d.aggregator.FileOutputType.DATA;
import static gov.cms.ab2d.aggregator.FileOutputType.ERROR;

@Slf4j
@Component
public class PatientClaimsProcessorImpl {

    private final BFDClient bfdClient;
    private final EventClient eventClient;

    private final String efsMount;

    private final String streamingDir;

    private final String finishedDir;

    @Autowired
    public PatientClaimsProcessorImpl(BFDClient bfdClient,
                                      @Value("${efs.mount}") String efsMount,
                                      @Value("${aggregator.directory.streaming:streaming}") String streamingDir,
                                      @Value("${aggregator.directory.finished:finished}") String finishedDir,
                                      EventClient eventClient) {
        this.bfdClient = bfdClient;
        this.efsMount = efsMount;
        this.streamingDir = streamingDir;
        this.finishedDir = finishedDir;
        this.eventClient = eventClient;
    }

    public void processBeneficiaries(JobFetchPayload jobFetchPayload, ProgressTrackerUpdate update) throws IOException {
        File file = null;
        try (ClaimsStream stream = buildClaimStream(jobFetchPayload.getJobId(), DATA)) {
            file = stream.getFile();
            eventClient.sendLogs(new FileEvent(jobFetchPayload.getOrganization(), jobFetchPayload.getJobId(),
                    stream.getFile(), FileEvent.FileStatus.OPEN));
            for (PatientCoverage coverage : jobFetchPayload.getBeneficiaries()) {
                List<IBaseResource> eobs = getEobBundleResources(jobFetchPayload, coverage);
                writeOutResource(jobFetchPayload, update, eobs, stream);
                update.incPatientProcessCount();
            }
        } finally {
            eventClient.sendLogs(new FileEvent(jobFetchPayload.getOrganization(), jobFetchPayload.getJobId(),
                    file, FileEvent.FileStatus.CLOSE));
        }
    }

    void writeOutErrors(String anyErrors, JobFetchPayload jobFetchPayload) {
        File errorFile = null;
        try (ClaimsStream stream = buildClaimStream(jobFetchPayload.getJobId(), ERROR)) {
            errorFile = stream.getFile();
            eventClient.sendLogs(new FileEvent(jobFetchPayload.getOrganization(), jobFetchPayload.getJobId(),
                    stream.getFile(), FileEvent.FileStatus.OPEN));
            stream.write(anyErrors);
        } catch (IOException e) {
            log.error("Cannot log error to error file");
        } finally {
            eventClient.sendLogs(new FileEvent(jobFetchPayload.getOrganization(), jobFetchPayload.getJobId(),
                    errorFile, FileEvent.FileStatus.CLOSE));
        }
    }

    ClaimsStream buildClaimStream(String jobId, FileOutputType outputType) throws IOException {
        return new ClaimsStream(jobId, efsMount, outputType,
                streamingDir, finishedDir, (int) FileUtils.ONE_MB);
    }

    @Trace(metricName = "EOBWriteToFile", dispatcher = true)
    private void writeOutResource(JobFetchPayload jobFetchPayload, ProgressTrackerUpdate update,
                                  List<IBaseResource> eobs, ClaimsStream stream) {
        IParser parser = jobFetchPayload.getVersion().getJsonParser().setPrettyPrint(false);
        if (eobs == null) {
            log.debug("ignoring empty results because pulling eobs failed");
            return;
        }

        if (eobs.isEmpty()) {
            return;
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
                IBaseResource operationOutcome = jobFetchPayload.getVersion().getErrorOutcome(errMsg);
                errorPayload.append(parser.encodeResourceToString(operationOutcome)).append(System.lineSeparator());
                eobsError++;
            }
        }

        update.addEobProcessedCount(eobsWritten);

        if (eobsError != 0) {
            writeOutErrors(errorPayload.toString(), jobFetchPayload);
        }
    }

    /**
     * Begin requesting claims from BFD using the provided, page through
     * the resulting claims until none are left, filter claims not meeting requirements, and filter out fields
     * in claims that AB2D cannot provide.
     *
     * @return list of matching claims after filtering claims not meeting requirements and stripping fields that AB2D
     * cannot provide
     */
    @Trace(metricName = "EOBRequest", dispatcher = true)
    List<IBaseResource> getEobBundleResources(JobFetchPayload payload, PatientCoverage patientCoverage) {

        OffsetDateTime requestStartTime = OffsetDateTime.now();

        IBaseBundle eobBundle;

        try {

            // Set header for requests so BFD knows where this request originated from
            BFDClient.BFD_BULK_JOB_ID.set(payload.getJobId());

            // Make first request and begin looping over remaining pages
            eobBundle = bfdClient.requestEOBFromServer(payload.getVersion(),
                    patientCoverage.getBeneId(), payload.getSince(), payload.getContract());
            List<IBaseResource> eobs = new ArrayList<>(PatientClaimsFilter.filterEntries(eobBundle, patientCoverage, payload.getAttestationDate(),
                    payload.isSkipBillablePeriodCheck(), payload.getSince(), payload.getVersion()));

            while (BundleUtils.getNextLink(eobBundle) != null) {
                eobBundle = bfdClient.requestNextBundleFromServer(payload.getVersion(), eobBundle, payload.getContract());
                eobs.addAll(PatientClaimsFilter.filterEntries(eobBundle, patientCoverage, payload.getAttestationDate(),
                        payload.isSkipBillablePeriodCheck(), payload.getSince(), payload.getVersion()));
            }

            return eobs;
        } catch (Exception ex) {
            logError(payload, requestStartTime, ex, patientCoverage.getBeneId());
            throw ex;
        } finally {
            BFDClient.BFD_BULK_JOB_ID.remove();
        }
    }

    void logError(JobFetchPayload eobFetchParams, OffsetDateTime start, Exception ex, Long beneId) {
        eventClient.sendLogs(
                new BeneficiarySearchEvent(eobFetchParams.getOrganization(), eobFetchParams.getJobId(), eobFetchParams.getContract(),
                        start, OffsetDateTime.now(),
                        beneId,
                        "ERROR: " + ex.getMessage()));
    }
}
