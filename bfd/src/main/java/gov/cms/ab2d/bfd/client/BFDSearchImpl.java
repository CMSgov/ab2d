package gov.cms.ab2d.bfd.client;

import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
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

    @Autowired
    HttpClient httpClient;

    @Value("${bfd.serverBaseUrl}")
    private String serverBaseUrl;

    @Override
    public String searchEOB(String patientId, OffsetDateTime since) throws IOException, InterruptedException {
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
        ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

            @Override
            public String handleResponse(
                    final HttpResponse response) throws ClientProtocolException, IOException {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            }

        };
        return httpClient.execute(httpget, responseHandler);
    }
}
