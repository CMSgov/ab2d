package gov.cms.ab2d.bfd.client;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;

import java.io.InputStream;
import java.time.OffsetDateTime;

@Component
@Slf4j
public class BFDSearchImpl implements BFDSearch {

    private final HttpClient httpClient;

    private final IParser parser;

    private final Environment environment;

    @Value("${bfd.serverBaseUrl}")
    private String serverBaseUrl;

    public BFDSearchImpl(HttpClient httpClient, IParser iParser, Environment environment) {
        this.httpClient = httpClient;
        this.parser = iParser;
        this.environment = environment;
    }

    @Override
    public org.hl7.fhir.dstu3.model.Bundle searchEOB(String patientId, OffsetDateTime since, int pageSize, String bulkJobId) throws IOException {
        StringBuilder url = new StringBuilder(serverBaseUrl + "ExplanationOfBenefit?patient=" + patientId + "&excludeSAMHSA=true");

        if (since != null) {
            url.append("&_lastUpdated=ge").append(since);
        }

        if (pageSize > 0) {
            url.append("&_count=").append(pageSize);
        }

        HttpGet request = new HttpGet(url.toString());
        // No active profiles means use JSON
        if (environment.getActiveProfiles().length == 0) {
            request.addHeader("Accept", "application/fhir+json;q=1.0, application/json+fhir;q=0.9");
        }

        request.addHeader(HttpHeaders.ACCEPT, "gzip");
        request.addHeader(HttpHeaders.ACCEPT_CHARSET, "utf-8");
        request.addHeader(BFDClient.BFD_HDR_BULK_CLIENTID, BFDClient.BFD_CLIENT_ID);
        request.addHeader(BFDClient.BFD_HDR_BULK_JOBID, bulkJobId);

        try (CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(request)) {
            int status = response.getStatusLine().getStatusCode();
            if (status >= HttpStatus.SC_OK && status < HttpStatus.SC_MULTIPLE_CHOICES) {
                try (InputStream instream = response.getEntity().getContent()) {
                    return parser.parseResource(org.hl7.fhir.dstu3.model.Bundle.class, instream);
                }
            } else if (status == HttpStatus.SC_NOT_FOUND) {
                throw new ResourceNotFoundException("Patient " + patientId + " was not found");
            } else {
                throw new RuntimeException("Server error occurred");
            }
        }
    }
}
