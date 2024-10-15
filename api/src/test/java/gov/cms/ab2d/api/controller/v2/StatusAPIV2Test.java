package gov.cms.ab2d.api.controller.v2;

import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.api.controller.TestUtil;
import gov.cms.ab2d.api.remote.JobClientMock;
import gov.cms.ab2d.common.util.AB2DLocalstackContainer;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.util.List;

import static gov.cms.ab2d.common.model.Role.SPONSOR_ROLE;
import static gov.cms.ab2d.common.util.Constants.FHIR_NDJSON_CONTENT_TYPE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class StatusAPIV2Test {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  JobClientMock jobClientMock;

  @Container
  private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

  @Container
  private static final AB2DLocalstackContainer localstackContainer = new AB2DLocalstackContainer();

  @Autowired
  private TestUtil testUtil;

  @Autowired
  private DataSetup dataSetup;

  @Autowired
  SqsAsyncClient amazonSQS;

  @Autowired
  SQSEventClient sqsEventClient;

  private String token;

  @BeforeEach
  public void setup() throws Exception {
    token = testUtil.setupToken(List.of(SPONSOR_ROLE));
    testUtil.turnMaintenanceModeOff();
  }

  @AfterEach
  public void cleanup() {
    dataSetup.cleanup();
    jobClientMock.cleanupAll();
  }

  @Test
  void testStatus() throws Exception {
    // start a job
    this.mockMvc.perform(
      get("https://localhost:8443/api/v2/fhir/Patient/$export").contentType(FHIR_NDJSON_CONTENT_TYPE)
              .header("Authorization", "Bearer " + token)
              .header("X-Forwarded-Proto", "https"));

    // get the job status
    String jobUuid = jobClientMock.pickAJob();
    ResultActions resultActions = this.mockMvc.perform(
      get(String.format("https://localhost:8443/api/v2/fhir/Job/%s/$status", jobUuid)).contentType(FHIR_NDJSON_CONTENT_TYPE)
        .header("Authorization", "Bearer " + token));

    // assert ok
    resultActions.andExpect(status().isOk());
  }

  @Test
  void testDelete() throws Exception {
    // start a job
    this.mockMvc.perform(
      get("https://localhost:8443/api/v2/fhir/Patient/$export").contentType(FHIR_NDJSON_CONTENT_TYPE)
              .header("Authorization", "Bearer " + token)
              .header("X-Forwarded-Proto", "https"));

    // try and delete the job
    String jobUuid = jobClientMock.pickAJob();
    ResultActions resultActions = this.mockMvc.perform(
      delete(String.format("https://localhost:8443/api/v2/fhir/Job/%s/$status", jobUuid)).contentType(FHIR_NDJSON_CONTENT_TYPE)
        .header("Authorization", "Bearer " + token));

    // assert that you can't (because the job is already marked successful)
    resultActions.andExpect(status().is4xxClientError());
  }

}
