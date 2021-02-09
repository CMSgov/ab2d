package gov.cms.ab2d.bfd.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.RequestFormatParamStyleEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.ab2d.fhir.Versions;
import org.apache.http.client.HttpClient;

public class FhirBfdServer {
    private final FhirContext fhirContext;

    public FhirBfdServer(Versions.FhirVersions version) {
        fhirContext = Versions.getContextFromVersion(version);
    }

    public IGenericClient bfdFhirRestClient(HttpClient httpClient, String url) {
        fhirContext.getRestfulClientFactory().setHttpClient(httpClient);
        IGenericClient client = fhirContext.newRestfulGenericClient(url);
        client.setFormatParamStyle(RequestFormatParamStyleEnum.SHORT);
        return client;
    }

    public FhirContext fhirContext() {
        return fhirContext;
    }
}
