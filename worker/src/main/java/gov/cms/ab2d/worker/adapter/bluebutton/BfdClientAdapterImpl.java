package gov.cms.ab2d.worker.adapter.bluebutton;

import gov.cms.ab2d.bfd.client.BFDClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
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
    public Future<List<Resource>> getResources(String patientId) {
        final Bundle bundle1 = bfdClient.requestEOBFromServer(patientId);
        final List<Bundle.BundleEntryComponent> entries = bundle1.getEntry();
        final List<Resource> resources = entries.stream()
//                .filter(e -> e.getResource().getResourceType().equals("ExplanationOfBenefits"))
                .map(e -> e.getResource())
                .filter(r -> r != null)
                .collect(Collectors.toList());

        /**
         * How to handle multiple pages??? figure out in the next iteration.
         */

        log.info("Bundle - Total: {} - Entries: {} ", bundle1.getTotal(), entries.size());
        return new AsyncResult(resources);
    }


}
