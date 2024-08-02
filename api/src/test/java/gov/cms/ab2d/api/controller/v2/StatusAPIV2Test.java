package gov.cms.ab2d.api.controller.v2;

import static gov.cms.ab2d.common.model.Role.SPONSOR_ROLE;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;

import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.api.controller.TestUtil;
import gov.cms.ab2d.common.util.AB2DLocalstackContainer;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;

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

  @Container
  private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

  @Container
  private static final AB2DLocalstackContainer localstackContainer = new AB2DLocalstackContainer();

  @Autowired
  private TestUtil testUtil;

  @Autowired
  private DataSetup dataSetup;

  @Autowired
  AmazonSQS amazonSQS;

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
    amazonSQS.purgeQueue(new PurgeQueueRequest(System.getProperty("sqs.queue-name")));
  }

  @Test
  void testStatus() throws Exception {
    ResultActions resultActions = this.mockMvc.perform(
      get(String.format("https://localhost:8443/api/v2/fhir/Job/%d/$status", 1234)).contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + token));
    resultActions.andExpect(status().isNotFound());
  }

  @Test
  void testDelete() throws Exception {
    ResultActions resultActions = this.mockMvc.perform(
      delete(String.format("https://localhost:8443/api/v2/fhir/Job/%d/$status", 1234)).contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + token));
    resultActions.andExpect(status().isNotFound());
  }

}
