package gov.cms.ab2d.worker.adapter.bluebutton;

import gov.cms.ab2d.bfd.client.BFDClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BfdClientAdapterImpl implements BfdClientAdapter {

    @Autowired
    private BFDClient bfdClient;

    @Override
    public Future<List<Resource>> getEobBundleResources(String patientId) {

        final Bundle eobBundle = bfdClient.requestEOBFromServer(patientId);

        final List<BundleEntryComponent> entries = eobBundle.getEntry();
        final List<Resource> resources = extractResources(entries);

        /**
         * How to handle multiple pages??? figure out in the next iteration.
         */

        log.info("Bundle - Total: {} - Entries: {} ", eobBundle.getTotal(), entries.size());
        return new AsyncResult(resources);
    }

    private List<Resource> extractResources(List<BundleEntryComponent> entries) {
        return entries.stream()
                .map(entry -> entry.getResource())
                .filter(resource -> resource != null)
                .collect(Collectors.toList());
    }


}
