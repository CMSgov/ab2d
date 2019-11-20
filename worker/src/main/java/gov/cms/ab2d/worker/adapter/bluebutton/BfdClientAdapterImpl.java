package gov.cms.ab2d.worker.adapter.bluebutton;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.worker.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
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
    public Future<String> processPatient(String patientId, Path outputFile) {
        final var resources = getEobBundleResources(patientId);

        var jsonParser = fhirContext.newJsonParser();
        int resourceCount = 0;
        try {
            var byteArrayOutputStream = new ByteArrayOutputStream();
            for (var resource : resources) {
                ++resourceCount;
                final String payload = jsonParser.encodeResourceToString(resource) + System.lineSeparator();
                byteArrayOutputStream.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            appendToFile(outputFile, byteArrayOutputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        log.info("finished writing [{}] resources", resourceCount);

        return new AsyncResult<>(patientId);
    }

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
