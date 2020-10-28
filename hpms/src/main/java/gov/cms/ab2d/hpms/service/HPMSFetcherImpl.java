package gov.cms.ab2d.hpms.service;

import gov.cms.ab2d.hpms.hmsapi.HPMSAttestationsHolder;
import gov.cms.ab2d.hpms.hmsapi.HPMSOrganizations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class HPMSFetcherImpl extends AbstractHPMSService implements HPMSFetcher, HPMSCleanupTest {

    private static final String HPMS_BASE_PATH = "/api/cda";

    @Value("${hpms.base.url}")
    private String hpmsBaseURI;

    private URI organizationBaseUri;
    private URI attestationBaseUri;

    private final HPMSAuthService authService;

    @Autowired
    public HPMSFetcherImpl(HPMSAuthService authService) {
        this.authService = authService;
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    @PostConstruct
    private void buildURI() {
        organizationBaseUri = buildFullURI(hpmsBaseURI + HPMS_BASE_PATH + "/orgs/info");
        attestationBaseUri = buildFullURI(hpmsBaseURI + HPMS_BASE_PATH + "/contracts/status");
    }

    @Override
    public void retrieveSponsorInfo(Consumer<HPMSOrganizations> hpmsOrgCallback) {
        Flux<HPMSOrganizations> orgInfoFlux = WebClient.create()
                .get().uri(organizationBaseUri)
                .headers(authService::buildAuthHeaders)
                .retrieve()
                .bodyToFlux(HPMSOrganizations.class);

        orgInfoFlux.subscribe(hpmsOrgCallback);
    }

    @Override
    public void retrieveAttestationInfo(Consumer<HPMSAttestationsHolder> hpmsAttestationCallback, List<String> contractIds) {
        Flux<HPMSAttestationsHolder> contractsFlux = WebClient.create()
                .get().uri(buildAttestationURI(serializeContractIds(contractIds)))
                .headers(authService::buildAuthHeaders)
                .retrieve()
                .bodyToFlux(HPMSAttestationsHolder.class);

        contractsFlux.subscribe(hpmsAttestationCallback);
    }

    /*
     * Format for contracts variable defined by
     * https://confluence.cms.gov/pages/viewpage.action?spaceKey=HPMSMCTAPI&title=CDA+CY+2021+API+Data+Contract+and+Validations
     *
     * Simple example from that page: ["S0001","S0002"]
     *
     * This is a helper method to convert form a list of contract ids to that form.
     */
    private String serializeContractIds(List<String> contracts) {
        return contracts.stream().collect(Collectors.joining("\",\"", "[\"", "\"]"));
    }

    private URI buildAttestationURI(String contractIds) {
        return UriComponentsBuilder
                .fromUri(attestationBaseUri)
                .queryParam("contractIds", contractIds)
                .build().toUri();
    }

    @Override
    public void cleanup() {
        ((HPMSCleanupTest) authService).cleanup();
    }
}
