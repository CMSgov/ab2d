package gov.cms.ab2d.hpms.service;

import gov.cms.ab2d.hpms.hmsapi.HPMSAttestationsHolder;
import gov.cms.ab2d.hpms.hmsapi.HPMSOrganizations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Service
public class HPMSFetcherImpl implements HPMSFetcher {

    @Value("${hpms.base.url}/api/cda/orgs/info")
    private String organizationBaseUrl;

    @Value("${hpms.base.url}/api/cda/contracts/status")
    private String attestationBaseUrl;

    @Override
    public void retrieveSponsorInfo(Consumer<HPMSOrganizations> hpmsOrgCallback) {
        Flux<HPMSOrganizations> orgInfoFlux = WebClient.create(organizationBaseUrl)
                .get()
                .retrieve()
                .bodyToFlux(HPMSOrganizations.class);

        orgInfoFlux.subscribe(hpmsOrgCallback);
    }

    @Override
    public void retrieveAttestationInfo(Consumer<HPMSAttestationsHolder> hpmsAttestationCallback, String contractIds) {
        Flux<HPMSAttestationsHolder> contractsFlux = WebClient.create(attestationBaseUrl)
                .get().uri(uriBuilder -> uriBuilder.queryParam("contractIds", contractIds).build())
                .retrieve()
                .bodyToFlux(HPMSAttestationsHolder.class);

        contractsFlux.subscribe(hpmsAttestationCallback);
    }
}
