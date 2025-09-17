package gov.cms.ab2d.bfd.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.ab2d.fhir.FhirVersion;

class BFDSearchImplTest {

  private static int patientId = 1;
  private static OffsetDateTime since = OffsetDateTime.now();
  private static OffsetDateTime until = OffsetDateTime.now();
  private static int pageSize = 10;
  private static String bulkJobId = "bulkJobId";
  private static FhirVersion version = FhirVersion.R4;
  private static String contractNum = "contractNum";
  private static String filePath = "src/test/resources/bb-test-data/eob/20010000001115.json";
  private static String activeProfile = "test";
  private static String bfdUrl = "http://localhost:8080";

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
    when(environment.getActiveProfiles()).thenReturn(new String[] { activeProfile });

    // Setup classes
    BfdClientVersions bfdClientVersions = new BfdClientVersions(bfdUrl, httpClient);
    BFDSearchImpl bfdSearchImpl = new BFDSearchImpl(httpClient, environment, bfdClientVersions);

    // Business logic & assertion(s)
    assertThrows(
      RuntimeException.class,
      () -> bfdSearchImpl.searchEOB(patientId, since, until, pageSize, bulkJobId, version, contractNum)
    );
  }

  @Test
  void testSearchEOB200() throws IOException{
    // Setup mocks
    HttpClient httpClient = httpClient200();
    Environment environment = mock(Environment.class);
    when(environment.getActiveProfiles()).thenReturn(new String[] { activeProfile });

    // Setup classes
    BfdClientVersions bfdClientVersions = new BfdClientVersions(bfdUrl, httpClient);
    BFDSearchImpl bfdSearchImpl = new BFDSearchImpl(httpClient, environment, bfdClientVersions);

    // Business logic & assertion(s)
    IBaseBundle result = bfdSearchImpl.searchEOB(patientId, since, until, pageSize, bulkJobId, version, contractNum);
    assertNotNull(result);
  }

  @Test
  void testSearchEOB200WithFalsyArgs() throws IOException{
    // Setup mocks
    HttpClient httpClient = httpClient200();
    Environment environment = mock(Environment.class);
    when(environment.getActiveProfiles()).thenReturn(new String[] { "" });

    // Setup classes
    BfdClientVersions bfdClientVersions = new BfdClientVersions(bfdUrl, httpClient);
    BFDSearchImpl bfdSearchImpl = new BFDSearchImpl(httpClient, environment, bfdClientVersions);

    // Business logic & assertion(s)
    IBaseBundle result = bfdSearchImpl.searchEOB(patientId, null, null,0, bulkJobId, version, contractNum);
    assertNotNull(result);
  }

  @Test
  void testSearchEOB404() throws IOException {
    // Setup mocks
    HttpClient httpClient = httpClient404();
    Environment environment = mock(Environment.class);
    when(environment.getActiveProfiles()).thenReturn(new String[] { activeProfile });

    // Setup classes
    BfdClientVersions bfdClientVersions = new BfdClientVersions(bfdUrl, httpClient);
    BFDSearchImpl bfdSearchImpl = new BFDSearchImpl(httpClient, environment, bfdClientVersions);

    // Business logic & assertion(s)
    assertThrows(
      ResourceNotFoundException.class,
      () -> bfdSearchImpl.searchEOB(patientId, since, until, pageSize, bulkJobId, version, contractNum)
    );
  }

  @Test
  void testSearchEOB500() throws IOException {
    // Setup mocks
    HttpClient httpClient = httpClient500();
    Environment environment = mock(Environment.class);
    when(environment.getActiveProfiles()).thenReturn(new String[] { activeProfile });

    // Setup classes
    BfdClientVersions bfdClientVersions = new BfdClientVersions(bfdUrl, httpClient);
    BFDSearchImpl bfdSearchImpl = new BFDSearchImpl(httpClient, environment, bfdClientVersions);

    // Business logic & assertion(s)
    assertThrows(
      RuntimeException.class,
      () -> bfdSearchImpl.searchEOB(patientId, since, until, pageSize, bulkJobId, version, contractNum)
    );
  }

}
