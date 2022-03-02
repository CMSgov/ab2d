package gov.cms.ab2d.hpms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

public class AbstractHPMSService {

    /*
     * In Production, this most likely will be an empty map which is defined as {} in the property which results in
     * the collection actually not being initialized.  Thus, added the explicit initialization to an empty collection
     * so params is never null.
     */
    @SuppressWarnings("FieldMayBeFinal")
    @Value("#{${hpms.api.params}}")
    private MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

    protected URI buildFullURI(String baseURI) {

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(baseURI);

        if (!params.isEmpty()) {
            uriBuilder.queryParams(params);
        }

        return uriBuilder.build().toUri();
    }
}
