package gov.cms.ab2d.bfd.client;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.newrelic.api.agent.Trace;
import gov.cms.ab2d.fhir.FhirVersion;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;

@Component
@Slf4j
public class BFDSearchImpl implements BFDSearch {

    private final HttpClient httpClient;
    private final Environment environment;
    private final BfdClientVersions bfdClientVersions;

    public BFDSearchImpl(HttpClient httpClient, Environment environment, BfdClientVersions bfdClientVersions) {
        this.httpClient = httpClient;
        this.environment = environment;
        this.bfdClientVersions = bfdClientVersions;
    }

    /**
     * Search for ebos using the provided parameters
     *
     * AB2D does not report SAMHSA claims (https://www.samhsa.gov/).
     *      - excludeSAMHSA must be set to true to maintain this exclusion
     *
     * @param patientId internal beneficiary id
     * @param since the minimum lastUpdated date which may be null
     * @param pageSize maximum number of records that can be returned
     * @param bulkJobId header to uniquely identify what job this is coming from within BFD logs
     * @param version the version of FHIR that we need from BFD
     * @return a bundle of eobs for the patient.
     * @throws IOException on failure to retrieve claims from BFD
     */
    @Trace
    @Override
    public IBaseBundle searchEOB(long patientId, OffsetDateTime since, int pageSize, String bulkJobId, FhirVersion version) throws IOException {

        String urlLocation = bfdClientVersions.getUrl(version);
        StringBuilder url = new StringBuilder(urlLocation + "ExplanationOfBenefit?patient=" + patientId + "&excludeSAMHSA=true");

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

        byte[] responseBytes = getEOBSFromBFD(patientId, request);

        return parseBundle(version, responseBytes);
    }

    @Trace
    private byte[] getEOBSFromBFD(long patientId, HttpGet request) throws IOException {
        byte[] responseBytes;
        try (CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(request)) {
            int status = response.getStatusLine().getStatusCode();
            if (status >= HttpStatus.SC_OK && status < HttpStatus.SC_MULTIPLE_CHOICES) {

                try (InputStream instream = response.getEntity().getContent()) {
                    responseBytes = instream.readAllBytes();
                }

            } else if (status == HttpStatus.SC_NOT_FOUND) {
                throw new ResourceNotFoundException("Patient " + patientId + " was not found");
            } else {
                throw new RuntimeException("Server error occurred");
            }
        }
        return responseBytes;
    }

    @Trace
    private IBaseBundle parseBundle(FhirVersion version, byte[] responseBytes) {
        return version.getJsonParser().parseResource(version.getBundleClass(), new ByteArrayInputStream(responseBytes));
    }
}
