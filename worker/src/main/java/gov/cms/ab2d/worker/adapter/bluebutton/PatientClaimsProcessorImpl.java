package gov.cms.ab2d.worker.adapter.bluebutton;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.util.FHIRUtil;
import gov.cms.ab2d.filter.ExplanationOfBenefitsTrimmer;
import gov.cms.ab2d.worker.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static gov.cms.ab2d.common.util.Constants.FILE_LOG;
import static java.util.concurrent.TimeUnit.SECONDS;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

@Slf4j
@Component
@RequiredArgsConstructor
public class PatientClaimsProcessorImpl implements PatientClaimsProcessor {

    @Autowired
    private BFDClient bfdClient;

    @Autowired
    private FhirContext fhirContext;

    @Autowired
    private FileService fileService;

    @Value("${file.try.lock.timeout}")
    private int tryLockTimeout;

    @Async("bfd-client")
    public Future<Integer> process(String patientId, ReentrantLock lock, Path outputFile, Path errorFile) {
        int errorCount = 0;
        int resourceCount = 0;

        try {
            var resources = getEobBundleResources(patientId);

            var jsonParser = fhirContext.newJsonParser();

            var byteArrayOutputStream = new ByteArrayOutputStream();
            for (var resource : resources) {
                ++resourceCount;
                try {
                    final String payload = jsonParser.encodeResourceToString(resource) + System.lineSeparator();
                    byteArrayOutputStream.write(payload.getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    log.warn("Encountered exception while processing job resources: {}", e.getMessage());
                    ++errorCount;
                    handleException(errorFile, e, lock);
                }
            }

            appendToFile(outputFile, byteArrayOutputStream, lock);
        } catch (Exception e) {
            ++errorCount;
            try {
                handleException(errorFile, e, lock);
            } catch (IOException e1) {
                //should not happen - original exception will be thrown
                log.error("error during exception handling to write error record");
            }
            throw new RuntimeException(e);
        }

        log.info("finished writing [{}] resources", resourceCount);

        if (errorCount > 0) {
            log.warn("[{}] error records. Should create an error row in JobOutput table", errorCount);
        }

        return new AsyncResult<>(errorCount);
    }

    private void handleException(Path errorFile, Exception e, ReentrantLock lock) throws IOException {
        var errMsg = ExceptionUtils.getRootCauseMessage(e);
        var operationOutcome = FHIRUtil.getErrorOutcome(errMsg);

        var jsonParser = fhirContext.newJsonParser();
        var payload = jsonParser.encodeResourceToString(operationOutcome) + System.lineSeparator();

        var byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(payload.getBytes(StandardCharsets.UTF_8));
        appendToFile(errorFile, byteArrayOutputStream, lock);
    }

    /**
     * uses re-entrant lock to lock the shared file resource
     * @param outputFile
     * @param byteArrayOutputStream
     * @throws IOException
     */
    private void appendToFile(Path outputFile, ByteArrayOutputStream byteArrayOutputStream, ReentrantLock lock) throws IOException {

        tryLock(lock);

        try {
            log.info("Attempting to append to file", keyValue(FILE_LOG, outputFile.getFileName()));
            fileService.appendToFile(outputFile, byteArrayOutputStream);
        } finally {
            lock.unlock();
        }
    }

    private void tryLock(ReentrantLock lock) {
        final String errMsg = "Terminate processing. Unable to acquire lock";
        try {
            final boolean lockAcquired = lock.tryLock(tryLockTimeout, SECONDS);
            if (!lockAcquired) {
                final String errMsg1 = errMsg + " after waiting " + tryLockTimeout + " seconds.";
                throw new RuntimeException(errMsg1);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(errMsg);
        }
    }


    private List<Resource> getEobBundleResources(String patientId) {

        Bundle eobBundle = bfdClient.requestEOBFromServer(patientId);

        final List<BundleEntryComponent> entries = eobBundle.getEntry();
        final List<Resource> resources = extractResources(entries);

        while (eobBundle.getLink(Bundle.LINK_NEXT) != null) {
            eobBundle = bfdClient.requestNextBundleFromServer(eobBundle);
            final List<BundleEntryComponent> nextEntries = eobBundle.getEntry();
            resources.addAll(extractResources(nextEntries));
        }

        log.info("Bundle - Total: {} - Entries: {} ", eobBundle.getTotal(), entries.size());
        return resources;
    }

    private List<Resource> extractResources(List<BundleEntryComponent> entries) {
        return entries.stream()
                // Get the resource
                .map(BundleEntryComponent::getResource)
                // Get only the explanation of benefits
                .filter(resource -> resource.getResourceType() == ResourceType.ExplanationOfBenefit)
                // filter it
                .map(resource -> ExplanationOfBenefitsTrimmer.getBenefit((ExplanationOfBenefit) resource))
                // Remove any empty values
                .filter(Objects::nonNull)
                // compile the list
                .collect(Collectors.toList());
    }
}
