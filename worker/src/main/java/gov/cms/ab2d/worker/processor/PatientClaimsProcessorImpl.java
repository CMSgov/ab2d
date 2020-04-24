package gov.cms.ab2d.worker.processor;

import ca.uhn.fhir.context.FhirContext;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.util.FHIRUtil;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.BeneficiarySearchEvent;
import gov.cms.ab2d.filter.ExplanationOfBenefitTrimmer;
import gov.cms.ab2d.filter.FilterOutByDate;
import gov.cms.ab2d.worker.processor.domainmodel.PatientClaimsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static gov.cms.ab2d.filter.EOBLoadUtilities.isPartD;

@Slf4j
@Component
@RequiredArgsConstructor
public class PatientClaimsProcessorImpl implements PatientClaimsProcessor {

    private final BFDClient bfdClient;
    private final FhirContext fhirContext;
    private final LogManager logManager;

    /**
     * Process the retrieval of patient explanation of benefit objects and write them
     * to a file using the writer
     */
    @Trace(async = true)
    @Async("patientProcessorThreadPool")
    public Future<Void> process(PatientClaimsRequest request) {
        final Token token = request.getToken();
        token.link();

        int resourceCount = 0;

        String payload = "";
        try {
            // Retrieve the resource bundle of EOB objects
            var resources = getEobBundleResources(request);

            var jsonParser = fhirContext.newJsonParser();

            for (var resource : resources) {
                ++resourceCount;
                try {
                    payload = jsonParser.encodeResourceToString(resource) + System.lineSeparator();
                    request.getHelper().addData(payload.getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    log.warn("Encountered exception while processing job resources: {}", e.getMessage());
                    handleException(request.getHelper(), payload, e);
                }
            }
        } catch (Exception e) {
            try {
                handleException(request.getHelper(), payload, e);
            } catch (IOException e1) {
                //should not happen - original exception will be thrown
                log.error("error during exception handling to write error record");
            }

            token.expire();
            return AsyncResult.forExecutionException(e);
        }

        log.debug("finished writing [{}] resources", resourceCount);

        token.expire();
        return new AsyncResult<>(null);
    }

    private void handleException(StreamHelper helper, String data, Exception e) throws IOException {
        var errMsg = ExceptionUtils.getRootCauseMessage(e);
        var operationOutcome = FHIRUtil.getErrorOutcome(errMsg);

        var jsonParser = fhirContext.newJsonParser();
        var payload = jsonParser.encodeResourceToString(operationOutcome) + System.lineSeparator();

        var byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(payload.getBytes(StandardCharsets.UTF_8));
        helper.addError(data);
    }

    private List<Resource> getEobBundleResources(PatientClaimsRequest request) {
        var patient = request.getPatientDTO();
        var attTime = request.getAttTime();

        OffsetDateTime start = OffsetDateTime.now();
        Bundle eobBundle;
        try {
            eobBundle = bfdClient.requestEOBFromServer(patient.getPatientId(), request.getSinceTime());
            logManager.log(LogManager.LogType.KINESIS,
                    new BeneficiarySearchEvent(request.getUser(), request.getJob(), request.getContractNum(),
                            start, OffsetDateTime.now(),
                            request.getPatientDTO() != null ? request.getPatientDTO().getPatientId() : null,
                            "SUCCESS"));

        } catch (Exception ex) {
            logManager.log(LogManager.LogType.KINESIS,
                    new BeneficiarySearchEvent(request.getUser(), request.getJob(), request.getContractNum(),
                            start, OffsetDateTime.now(),
                            request.getPatientDTO() != null ? request.getPatientDTO().getPatientId() : null,
                            "ERROR: " + ex.getMessage()));
            throw ex;
        }

        final List<BundleEntryComponent> entries = eobBundle.getEntry();
        final List<Resource> resources = extractResources(entries, patient.getDateRangesUnderContract(), attTime);

        while (eobBundle.getLink(Bundle.LINK_NEXT) != null) {
            eobBundle = bfdClient.requestNextBundleFromServer(eobBundle);
            final List<BundleEntryComponent> nextEntries = eobBundle.getEntry();
            resources.addAll(extractResources(nextEntries, patient.getDateRangesUnderContract(), attTime));
        }

        log.debug("Bundle - Total: {} - Entries: {} ", eobBundle.getTotal(), entries.size());
        return resources;
    }

    private List<Resource> extractResources(List<BundleEntryComponent> entries, final List<FilterOutByDate.DateRange> dateRanges,
                                            OffsetDateTime attTime) {
        if (attTime == null) {
            return new ArrayList<>();
        }
        long epochMilli = attTime.toInstant().toEpochMilli();
        Date attDate = new Date(epochMilli);
        return entries.stream()
                // Get the resource
                .map(BundleEntryComponent::getResource)
                // Get only the explanation of benefits
                .filter(resource -> resource.getResourceType() == ResourceType.ExplanationOfBenefit)
                // Filter by date
                .filter(resource -> FilterOutByDate.valid((ExplanationOfBenefit) resource, attDate, dateRanges))
                // filter it
                .map(resource -> ExplanationOfBenefitTrimmer.getBenefit((ExplanationOfBenefit) resource))
                // Remove any empty values
                .filter(Objects::nonNull)
                // Remove Plan D
                .filter(resource -> !isPartD(resource))
                // compile the list
                .collect(Collectors.toList());
    }
}
