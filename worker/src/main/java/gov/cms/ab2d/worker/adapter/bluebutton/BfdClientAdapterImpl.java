package gov.cms.ab2d.worker.adapter.bluebutton;

import gov.cms.ab2d.bfd.client.BFDClient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Future;

@Slf4j
@Component
@RequiredArgsConstructor
public class BfdClientAdapterImpl implements BfdClientAdapter {

    @Autowired
    private BFDClient bfdClient;


    @Override
    @Async
    public Future<EobBundleDTO> getEobBundle(String patientId) {
        final Bundle bundle1 = bfdClient.requestEOBFromServer(patientId);
        final List<Bundle.BundleEntryComponent> entries = bundle1.getEntry();

        log.info("BundleId:{} - Total: {} - Entries: {} ", bundle1.getId(), bundle1.getTotal(), entries.size());

        final EobBundleDTO bundle = EobBundleDTO.builder().patientId(patientId).build();
        return new AsyncResult(bundle);
    }



    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EobBundleDTO {
        private String patientId;
        private String yadayadayada;
    }
}
