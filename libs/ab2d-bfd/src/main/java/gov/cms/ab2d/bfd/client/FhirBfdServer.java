package gov.cms.ab2d.bfd.client;

import ca.uhn.fhir.rest.api.RequestFormatParamStyleEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.ab2d.fhir.FhirVersion;
import org.apache.http.client.HttpClient;

/**
 * Used to support different versions of FHIR BFD endpoints;
 */
public class FhirBfdServer {
    private final FhirVersion version;
    private IGenericClient iGenericClient;

    /**
     * Constructor that creates the correct context from the version
     *
     * @param version - the FHIR version
     */
    public FhirBfdServer(FhirVersion version) {
        this.version = version;
    }

    /**
     * Creates the rest client by passing in the Http Client and the URL of the endpoint
     *
     * @param httpClient - the client
     * @param url - the URL of the BFD endpoint
     * @return
     */
    public IGenericClient getGenericClient(HttpClient httpClient, String url) {
        if (iGenericClient == null) {
            version.getContext().getRestfulClientFactory().setHttpClient(httpClient);
            iGenericClient = version.getContext().newRestfulGenericClient(url);
            iGenericClient.setFormatParamStyle(RequestFormatParamStyleEnum.SHORT);
        }
        return iGenericClient;
    }

    /**
     * Return the FHIR version
     *
     * @return the version
     */
    public FhirVersion getVersion() {
        return version;
    }
}
