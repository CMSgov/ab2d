package gov.cms.ab2d.hpms.service;

import gov.cms.ab2d.hpms.hmsapi.HPMSAttestationsHolder;
import gov.cms.ab2d.hpms.hmsapi.HPMSOrganizations;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Service
public class HPMSFetcherImpl implements HPMSFetcher {

    @Override
    public void retrieveSponsorInfo(Consumer<HPMSOrganizations> hpmsOrgCallback) {

        // todo: move url into property
        Flux<HPMSOrganizations> orgInfoFlux = WebClient.create("http://localhost:8080/api/cda/orgs/info")
                .get()
                .retrieve()
                .bodyToFlux(HPMSOrganizations.class);

        orgInfoFlux.subscribe(hpmsOrgCallback);
    }

    @Override
    public void retrieveAttestationInfo(Consumer<HPMSAttestationsHolder> hpmsAttestationCallback, String contractIds) {
//        String contractIdStr = "[\"S1234\",\"S2341\"]";
        Flux<HPMSAttestationsHolder> contractsFlux = WebClient.create("http://localhost:8080/api/cda/contracts/status")
                .get().uri(uriBuilder -> uriBuilder.queryParam("contractIds", contractIds).build())
                .retrieve()
                .bodyToFlux(HPMSAttestationsHolder.class);

        contractsFlux.subscribe(hpmsAttestationCallback);
    }
}
