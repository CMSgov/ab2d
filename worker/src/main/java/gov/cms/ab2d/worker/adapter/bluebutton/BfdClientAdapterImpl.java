package gov.cms.ab2d.worker.adapter.bluebutton;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.util.FHIRUtil;
import gov.cms.ab2d.worker.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.hl7.fhir.dstu3.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BfdClientAdapterImpl implements BfdClientAdapter {

    private final ReentrantLock lock = new ReentrantLock();

    @Autowired
    private BFDClient bfdClient;

    @Autowired
    private FhirContext fhirContext;

    @Autowired
    private FileService fileService;

    @Async("bfd-client")
    public Future<String> processPatient(String patientId, Path outputFile, Path errorFile) {
        boolean hasError = false;
        final var resources = getEobBundleResources(patientId);

        var jsonParser = fhirContext.newJsonParser();
        int resourceCount = 0;
        try {
            var byteArrayOutputStream = new ByteArrayOutputStream();
            for (var resource : resources) {
                ++resourceCount;
                try {
                    final String payload = jsonParser.encodeResourceToString(resource) + System.lineSeparator();
                    byteArrayOutputStream.write(payload.getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    hasError = true;
                    handleException(errorFile, new ByteArrayOutputStream(), e);
                }
            }

            appendToFile(outputFile, byteArrayOutputStream);
        } catch (Exception e) {
            try {
                handleException(errorFile, new ByteArrayOutputStream(), e);
            } catch (IOException e1) {
                //should not happen.
                log.error("error during exception handling to wrote error record.");
                //ignore. the original exception will be thrown
            }
            throw new RuntimeException(e);
        }

        if (hasError) {
            throw new RuntimeException("At least one of the resources had an error. Need to create an error JobOutput");
        }
        log.info("finished writing [{}] resources", resourceCount);

        return new AsyncResult<>(patientId);
    }

    private void handleException(Path errorFile, ByteArrayOutputStream byteArrayOutputStream, Exception e) throws IOException {
        String msg = ExceptionUtils.getRootCauseMessage(e);
        OperationOutcome operationOutcome = FHIRUtil.getErrorOutcome(msg);
        var jsonParser = fhirContext.newJsonParser();
        final String payload = jsonParser.encodeResourceToString(operationOutcome) + System.lineSeparator();
        byteArrayOutputStream.write(payload.getBytes(StandardCharsets.UTF_8));
        appendToFile(errorFile, byteArrayOutputStream);
    }

    /**
     * users re-entrant lock to lock the shared file resource
     * @param outputFile
     * @param byteArrayOutputStream
     * @throws IOException
     */
    private void appendToFile(Path outputFile, ByteArrayOutputStream byteArrayOutputStream) throws IOException {
        lock.lock();
        try {
            fileService.appendToFile(outputFile, byteArrayOutputStream);
        } finally {
            lock.unlock();
        }
    }


    private List<Resource> getEobBundleResources(String patientId) {

        final Bundle eobBundle = bfdClient.requestEOBFromServer(patientId);

        final List<BundleEntryComponent> entries = eobBundle.getEntry();
        final List<Resource> resources = extractResources(entries);

        /**
         * How to handle multiple pages??? figure out in the next iteration.
         */

        log.info("Bundle - Total: {} - Entries: {} ", eobBundle.getTotal(), entries.size());
        return resources;
    }

    private List<Resource> extractResources(List<BundleEntryComponent> entries) {
        return entries.stream()
                .map(entry -> entry.getResource())
                .filter(resource -> resource != null)
                .collect(Collectors.toList());
    }





}
