package gov.cms.ab2d.hpms.service;

import java.net.URI;
import org.springframework.web.util.UriComponentsBuilder;

public class AbstractHPMSService {

    /*
     * In Production, this most likely will be an empty map which is defined as {} in the property which results in
     * the collection actually not being initialized.
     */

    protected URI buildFullURI(String baseURI) {
        return UriComponentsBuilder.fromUriString(baseURI).build().toUri();
    }
}
