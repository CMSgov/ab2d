package gov.cms.ab2d.contracts.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.ab2d.contracts.hmsapi.HPMSAttestation;
import gov.cms.ab2d.contracts.hmsapi.HPMSEnrollment;
import gov.cms.ab2d.contracts.hmsapi.HPMSOrganizationInfo;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Service
public class HPMSFetcherImpl extends AbstractHPMSService implements HPMSFetcher {

    //https://confluence.cms.gov/display/HPMSMCTAPI/CDA+CY+2022+API+Data+Contract+and+Validations#CDACY2022APIDataContractandValidations-GetAttestationHistoryforContracts
    @Value("${hpms.base.path}")
    private String hpmsBasePath;

    @Value("${hpms.base.url}")
    private String hpmsBaseURI;

    private URI organizationBaseUri;

    private URI attestationBaseUri;

    private URI enrollmentBaseUri;

    private final HPMSAuthService authService;

    private final WebClient webClient;

    @Autowired
    public HPMSFetcherImpl(HPMSAuthService authService, WebClient webClient) {
        this.authService = authService;
        this.webClient = webClient;
    }

    @PostConstruct
    private void buildURI() {
        organizationBaseUri = UriComponentsBuilder.fromUriString(hpmsBaseURI + hpmsBasePath + "/orgs/info").build().toUri();
        attestationBaseUri = UriComponentsBuilder.fromUriString(hpmsBaseURI + hpmsBasePath + "/contracts/status").build().toUri();
        enrollmentBaseUri = UriComponentsBuilder.fromUriString(hpmsBaseURI + hpmsBasePath + "/enrollments").build().toUri();
    }

    @Override
    public List<HPMSOrganizationInfo> retrieveSponsorInfo() {
        Mono<Object[]> response = webClient
                .get().uri(organizationBaseUri)
                .headers(authService::buildAuthHeaders)
                .retrieve()
                .bodyToMono(Object[].class);
        Object[] objects = response.block();
        ObjectMapper mapper = new ObjectMapper();
        return Arrays.stream(objects)
                .map(object -> mapper.convertValue(object, HPMSOrganizationInfo.class))
                .toList();
    }

    @Override
    public Set<HPMSAttestation> retrieveAttestationInfo(List<String> contractIds) {
        Mono<Set<HPMSAttestation>> contractsMono = webClient
                .get().uri(buildAttestationURI(contractIds))
                .headers(authService::buildAuthHeaders)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<>() {
                });
        return contractsMono.block();
    }

    @Override
    public Set<HPMSEnrollment> retrieveEnrollmentInfo(List<String> contractIds) {
        Mono<Set<HPMSEnrollment>> contractsMono = webClient
                .get().uri(buildEnrollmentURI(contractIds))
                .headers(authService::buildAuthHeaders)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<>() {
                });
        return contractsMono.block();
    }

    private URI buildAttestationURI(List<String>  contractIds) {
        return UriComponentsBuilder
                .fromUri(attestationBaseUri)
                .queryParam("contractId", contractIds)
                .build().toUri();
    }

    private URI buildEnrollmentURI(List<String>  contractIds) {
        return UriComponentsBuilder
                .fromUri(enrollmentBaseUri)
                .queryParam("contractId", contractIds)
                .build().toUri();
    }
}
