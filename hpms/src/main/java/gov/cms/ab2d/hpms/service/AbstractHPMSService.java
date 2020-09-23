package gov.cms.ab2d.hpms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

public class AbstractHPMSService {

    @Value("#{${hpms.api.params}}")
    private MultiValueMap<String, String> params;

    protected URI buildFullURI(String baseURI) {

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(baseURI);

        if (!params.isEmpty()) {
            uriBuilder.queryParams(params);
        }

        return uriBuilder.build().toUri();
    }
}
