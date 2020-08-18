package gov.cms.ab2d.bfd.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.param.DateRangeParam;
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
import java.util.Date;

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

    /*private ResponseHandler<Bundle> responseHandler = new ResponseHandler<>() {

        @Override
        public Bundle handleResponse(
                final HttpResponse response) throws IOException {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                InputStream instream = response.getEntity().getContent();
                try {
                    return jsonParser.parseResource(Bundle.class, instream);
                }  finally {
                    instream.close();
                }
            } else {
                throw new ClientProtocolException("Unexpected response status: " + status);
            }
        }

    };*/

    @Override
    public Bundle searchEOB(String patientId, OffsetDateTime since) throws IOException, InterruptedException {
        DateRangeParam updatedSince = null;
        if (since != null) {
            Date d = Date.from(since.toInstant());
            updatedSince = new DateRangeParam(d, null);
        }

        //https://prod-sbx.bfd.cms.gov/v1/fhir/ExplanationOfBenefit?patient=-19990000000213&excludeSAMHSA=true&_format=json
        String url = serverBaseUrl + "ExplanationOfBenefit?patient=" + patientId + "&excludeSAMHSA=true" +
                "&_format=json";

        HttpGet request = new HttpGet(url);
        try(CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(request)) {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                InputStream instream = response.getEntity().getContent();
                try {
                    return jsonParser.parseResource(Bundle.class, instream);
                }  finally {
                    instream.close();
                }
            } else {
                return null;
            }
        }
    }
}
