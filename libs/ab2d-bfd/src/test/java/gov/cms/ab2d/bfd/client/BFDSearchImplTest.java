package gov.cms.ab2d.bfd.client;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.ab2d.fhir.FhirVersion;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
@Disabled
class BFDSearchImplTest {

    private static final int PATIENT_ID = 1;
    private static final OffsetDateTime SINCE = OffsetDateTime.now();
    private static final OffsetDateTime UNTIL = OffsetDateTime.now();
    private static final int PAGE_SIZE = 10;
    private static final String BULK_JOB_ID = "bulkJobId";
    private static final FhirVersion VERSION = FhirVersion.R4;
    private static final String CONTRACT_NUM = "contractNum";
    private static final String ACTIVE_PROFILE = "test";
    private static final String BFD_URL = "http://localhost:8080";
    private static final String BFD_URL_V3 = BFD_URL;

    HttpClient httpClient100() throws IOException {
        HttpClient httpClient = mock(HttpClient.class);
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(httpClient.execute(any())).thenReturn(response);
        when(response.getStatusLine()).thenReturn(mock(org.apache.http.StatusLine.class));
        when(response.getStatusLine().getStatusCode()).thenReturn(HttpStatus.SC_CONTINUE);
        return httpClient;
    }

    HttpClient httpClient200() throws IOException {
        HttpClient httpClient = mock(HttpClient.class);
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(httpClient.execute(any())).thenReturn(response);
        when(response.getStatusLine()).thenReturn(mock(org.apache.http.StatusLine.class));
        when(response.getStatusLine().getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(response.getEntity()).thenReturn(mock(org.apache.http.HttpEntity.class));
        String filePath = "src/test/resources/bb-test-data/eob/20010000001115.json";
        String fileContent = Files.readString(Paths.get(filePath));
        when(response.getEntity().getContent()).thenReturn(new ByteArrayInputStream(fileContent.getBytes()));
        return httpClient;
    }

    HttpClient httpClient404() throws IOException {
        HttpClient httpClient = mock(HttpClient.class);
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(httpClient.execute(any())).thenReturn(response);
        when(response.getStatusLine()).thenReturn(mock(org.apache.http.StatusLine.class));
        when(response.getStatusLine().getStatusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);
        return httpClient;
    }

    HttpClient httpClient500() throws IOException {
        HttpClient httpClient = mock(HttpClient.class);
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(httpClient.execute(any())).thenReturn(response);
        when(response.getStatusLine()).thenReturn(mock(org.apache.http.StatusLine.class));
        when(response.getStatusLine().getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        return httpClient;
    }

    @Test
    void testSearchEOB100() throws IOException {
        // Setup mocks
        HttpClient httpClient = httpClient100();
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{ACTIVE_PROFILE});

        // Setup classes
        BfdClientVersions bfdClientVersions = new BfdClientVersions(BFD_URL, BFD_URL_V3, httpClient);
        BFDSearchImpl bfdSearchImpl = new BFDSearchImpl(httpClient, environment, bfdClientVersions);

        // Business logic & assertion(s)
        assertThrows(
                RuntimeException.class,
                () -> bfdSearchImpl.searchEOB(PATIENT_ID, SINCE, UNTIL, PAGE_SIZE, BULK_JOB_ID, VERSION, CONTRACT_NUM)
        );
    }

    @Test
    void testSearchEOB200() throws IOException {
        // Setup mocks
        HttpClient httpClient = httpClient200();
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{ACTIVE_PROFILE});

        // Setup classes
        BfdClientVersions bfdClientVersions = new BfdClientVersions(BFD_URL, BFD_URL_V3, httpClient);
        BFDSearchImpl bfdSearchImpl = new BFDSearchImpl(httpClient, environment, bfdClientVersions);

        // Business logic & assertion(s)
        IBaseBundle result = bfdSearchImpl.searchEOB(PATIENT_ID, SINCE, UNTIL, PAGE_SIZE, BULK_JOB_ID, VERSION, CONTRACT_NUM);
        assertNotNull(result);
    }

    @Test
    void testSearchEOB200WithFalsyArgs() throws IOException {
        // Setup mocks
        HttpClient httpClient = httpClient200();
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{""});

        // Setup classes
        BfdClientVersions bfdClientVersions = new BfdClientVersions(BFD_URL, BFD_URL_V3, httpClient);
        BFDSearchImpl bfdSearchImpl = new BFDSearchImpl(httpClient, environment, bfdClientVersions);

        // Business logic & assertion(s)
        IBaseBundle result = bfdSearchImpl.searchEOB(PATIENT_ID, null, null, 0, BULK_JOB_ID, VERSION, CONTRACT_NUM);
        assertNotNull(result);
    }

    @Test
    void testSearchEOB404() throws IOException {
        // Setup mocks
        HttpClient httpClient = httpClient404();
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{ACTIVE_PROFILE});

        // Setup classes
        BfdClientVersions bfdClientVersions = new BfdClientVersions(BFD_URL, BFD_URL_V3, httpClient);
        BFDSearchImpl bfdSearchImpl = new BFDSearchImpl(httpClient, environment, bfdClientVersions);

        // Business logic & assertion(s)
        assertThrows(
                ResourceNotFoundException.class,
                () -> bfdSearchImpl.searchEOB(PATIENT_ID, SINCE, UNTIL, PAGE_SIZE, BULK_JOB_ID, VERSION, CONTRACT_NUM)
        );
    }

    @Test
    void testSearchEOB500() throws IOException {
        // Setup mocks
        HttpClient httpClient = httpClient500();
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{ACTIVE_PROFILE});

        // Setup classes
        BfdClientVersions bfdClientVersions = new BfdClientVersions(BFD_URL, BFD_URL_V3, httpClient);
        BFDSearchImpl bfdSearchImpl = new BFDSearchImpl(httpClient, environment, bfdClientVersions);

        // Business logic & assertion(s)
        assertThrows(
                RuntimeException.class,
                () -> bfdSearchImpl.searchEOB(PATIENT_ID, SINCE, UNTIL, PAGE_SIZE, BULK_JOB_ID, VERSION, CONTRACT_NUM)
        );
    }

}
