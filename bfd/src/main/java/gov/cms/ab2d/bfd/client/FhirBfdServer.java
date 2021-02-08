package gov.cms.ab2d.bfd.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.RequestFormatParamStyleEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.ab2d.fhir.Versions;
import org.apache.http.client.HttpClient;
import org.springframework.core.env.Environment;

public class FhirBfdServer {
    private final String serverBaseUrl;
    private final FhirContext fhirContext;

    public FhirBfdServer(Versions.FhirVersions version, Environment env) {
        String propertiesName = Versions.getEnvVariable(version);
        serverBaseUrl = env.getProperty(propertiesName);
        fhirContext = Versions.getContextFromVersion(version);
    }

    public IGenericClient bfdFhirRestClient(HttpClient httpClient) {
        fhirContext.getRestfulClientFactory().setHttpClient(httpClient);
        IGenericClient client = fhirContext.newRestfulGenericClient(serverBaseUrl);
        client.setFormatParamStyle(RequestFormatParamStyleEnum.SHORT);
        return client;
    }

    public FhirContext fhirContext() {
        return fhirContext;
    }
}
