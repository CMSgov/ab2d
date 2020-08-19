package gov.cms.ab2d.bfd.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.hl7.fhir.dstu3.model.Bundle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

import java.io.InputStream;
import java.time.OffsetDateTime;

@Component
@Slf4j
public class BFDSearchImpl implements BFDSearch {

    private final HttpClient httpClient;

    private final IParser jsonParser;

    @Value("${bfd.serverBaseUrl}")
    private String serverBaseUrl;

    public BFDSearchImpl(HttpClient httpClient, FhirContext fhirContext) {
        this.httpClient = httpClient;
        this.jsonParser = fhirContext.newJsonParser();
    }

    @Override
    public Bundle searchEOB(String patientId, OffsetDateTime since, int pageSize) throws IOException {
        StringBuilder url = new StringBuilder(serverBaseUrl + "ExplanationOfBenefit?patient=" + patientId + "&excludeSAMHSA=true" +
                "");

        if (since != null) {
            url.append("&_lastUpdated=ge").append(since);
        }

        if (pageSize > 0) {
            url.append("&_count=").append(pageSize);
        }

        HttpGet request = new HttpGet(url.toString());
        //request.addHeader("Accept", "application/fhir+json;q=1.0, application/json+fhir;q=0.9");
        request.addHeader("Accept-Encoding", "gzip");
        request.addHeader("Accept-Charset", "utf-8");

        try (CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(request)) {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                InputStream instream = response.getEntity().getContent();
                try {
                    return jsonParser.parseResource(Bundle.class, instream);
                } finally {
                    instream.close();
                }
            } else if (status == 404) {
                throw new ResourceNotFoundException("Patient " + patientId + " was not found");
            } else {
                throw new RuntimeException("Server error occurred");
            }
        }
    }
}
