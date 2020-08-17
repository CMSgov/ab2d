package gov.cms.ab2d.bfd.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import com.google.common.base.Charsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.instance.model.api.IBaseBinary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class BFDSearchImpl implements BFDSearch {

    private final HttpClient httpClient;

    private final FhirContext fhirContext;

    private final IParser jsonParser;

    @Value("${bfd.serverBaseUrl}")
    private String serverBaseUrl;

    public BFDSearchImpl(HttpClient httpClient, FhirContext fhirContext) {
        this.httpClient = httpClient;
        this.fhirContext = fhirContext;
        this.jsonParser = fhirContext.newJsonParser();
    }

    private ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

        @Override
        public String handleResponse(
                final HttpResponse response) throws IOException {
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                HttpEntity entity = response.getEntity();
                if(entity != null) {
                    try {
                        return IOUtils.toString(entity.getContent(), Charsets.UTF_8);
                    } finally {
                        entity.getContent().close();
                    }
                } else {
                    return null;
                }
            } else {
                throw new ClientProtocolException("Unexpected response status: " + status);
            }
        }

    };

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

        HttpGet httpget = new HttpGet(url);
        // Create a custom response handler
        String body = httpClient.execute(httpget, responseHandler);

        return jsonParser.parseResource(Bundle.class, body);
    }
}
