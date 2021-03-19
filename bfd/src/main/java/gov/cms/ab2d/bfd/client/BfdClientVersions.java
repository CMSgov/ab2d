package gov.cms.ab2d.bfd.client;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.ab2d.fhir.FhirVersion;
import org.apache.http.client.HttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static gov.cms.ab2d.fhir.FhirVersion.R4;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;

/**
 *  Holds configuration items for the different versions of the BFD versions as well as caching different servers
 */
@Component
public class BfdClientVersions {
    private Map<FhirVersion, String> clientUrls = new HashMap();
    private Map<FhirVersion, IGenericClient> bfdServers = new HashMap();
    private final HttpClient httpClient;

    /**
     * The constructor takes the values for the supported FHIR version URLs
     *
     * @param stu3Url - FHIR STU3 properties value
     * @param r4Url - FHIR R4 properties value
     * @param httpClient the http client
     */
    public BfdClientVersions(@Value("${bfd.serverBaseUrlStu3}") String stu3Url,
                             @Value("${bfd.serverBaseUrlR4}") String r4Url,
                             HttpClient httpClient) {
        clientUrls.put(STU3, stu3Url);
        clientUrls.put(R4, r4Url);
        this.httpClient = httpClient;
    }

    /**
     * Retrieve the correct URL for the FHIR version
     *
     * @param version - FHIR version
     * @return the URL
     */
    public String getUrl(FhirVersion version) {
        return clientUrls.get(version);
    }

    /**
     * Return the correct HAPI FHIR Client for the version
     *
     * @param version - the FHIR version
     * @return - the client
     */
    public IGenericClient getClient(FhirVersion version) {
        IGenericClient client = bfdServers.get(version);
        if (client != null) {
            return client;
        }
        String url = getUrl(version);
        client = new FhirBfdServer(version).getGenericClient(httpClient, url);
        bfdServers.put(version, client);
        return client;
    }
}
