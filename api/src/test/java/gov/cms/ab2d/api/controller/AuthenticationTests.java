package gov.cms.ab2d.api.controller;

import com.okta.jwt.AccessTokenVerifier;
import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.AB2DSQSMockConfig;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.eventclient.events.ApiRequestEvent;
import gov.cms.ab2d.eventclient.events.ApiResponseEvent;
import gov.cms.ab2d.eventclient.events.LoggableEvent;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.util.Collections;
import java.util.List;

import static gov.cms.ab2d.common.model.Role.SPONSOR_ROLE;
import static gov.cms.ab2d.common.util.Constants.*;
import static gov.cms.ab2d.common.util.DataSetup.TEST_PDP_CLIENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Import(AB2DSQSMockConfig.class)
public class AuthenticationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUtil testUtil;

    @Autowired
    private PdpClientRepository pdpClientRepository;

    @Autowired
    private DataSetup dataSetup;

    @Autowired
    SqsAsyncClient amazonSqs;

    @MockitoBean
    SQSEventClient sqsEventClient;

    @MockitoBean
    AccessTokenVerifier mockAccessTokenVerifier;

    private ArgumentCaptor<LoggableEvent> captor;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    private String token;

    @BeforeEach
    public void setup() throws JwtVerificationException {
        captor = ArgumentCaptor.forClass(LoggableEvent.class);
        token = testUtil.setupToken(List.of(SPONSOR_ROLE));
    }

    @AfterEach
    public void cleanup() {
        dataSetup.cleanup();
    }

    // Negative tests, successful auth tests are essentially done in other suites
    @Test
    public void testNoAuthHeader() throws Exception {
        this.mockMvc.perform(get(API_PREFIX_V1 + FHIR_PREFIX + "/Patient/$export")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(401));
        verify(sqsEventClient, times(2)).sendLogs(captor.capture());
        List<LoggableEvent> loggableEvents = captor.getAllValues();
        ApiRequestEvent requestEvent = (ApiRequestEvent) loggableEvents.get(0);
        ApiResponseEvent responseEvent = (ApiResponseEvent) loggableEvents.get(1);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), responseEvent.getResponseCode());
        assertEquals(requestEvent.getRequestId(), responseEvent.getRequestId());
    }

    @Test
    public void testBadStartToHeader() throws Exception {
        this.mockMvc.perform(get(API_PREFIX_V1 + FHIR_PREFIX + "/Patient/$export")
                .header("Authorization", "NotBearer")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(401));
    }

    @Test
    public void testNoTokenInHeader() throws Exception {
        this.mockMvc.perform(get(API_PREFIX_V1 + FHIR_PREFIX + "/Patient/$export")
                .header("Authorization", "Bearer ")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(401));
    }

    @Test
    public void testClientDoesNotExist() throws Exception {
        PdpClient pdpClient = pdpClientRepository.findByClientId(TEST_PDP_CLIENT);
        pdpClientRepository.delete(pdpClient);

        this.mockMvc.perform(get(API_PREFIX_V1 + FHIR_PREFIX + "/Patient/$export")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(403));
        verify(sqsEventClient, times(2)).sendLogs(captor.capture());
        List<LoggableEvent> loggableEvents = captor.getAllValues();
        ApiRequestEvent requestEvent = (ApiRequestEvent) loggableEvents.get(0);
        ApiResponseEvent responseEvent = (ApiResponseEvent) loggableEvents.get(1);
        assertEquals(HttpStatus.FORBIDDEN.value(), responseEvent.getResponseCode());
        assertEquals(requestEvent.getRequestId(), responseEvent.getRequestId());
    }

    @Test
    public void testClientIsNotEnabled() throws Exception {
        PdpClient pdpClient = pdpClientRepository.findByClientId(TEST_PDP_CLIENT);
        pdpClient.setEnabled(false);
        pdpClientRepository.save(pdpClient);

        this.mockMvc.perform(get(API_PREFIX_V1 + FHIR_PREFIX + "/Patient/$export")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(403));
        verify(sqsEventClient, times(2)).sendLogs(captor.capture());
        List<LoggableEvent> loggableEvents = captor.getAllValues();
        ApiRequestEvent requestEvent = (ApiRequestEvent) loggableEvents.get(0);
        ApiResponseEvent responseEvent = (ApiResponseEvent) loggableEvents.get(1);
        assertEquals(HttpStatus.FORBIDDEN.value(), responseEvent.getResponseCode());
        assertEquals(requestEvent.getRequestId(), responseEvent.getRequestId());
    }

    @Test
    public void testClientNoAuthorization() throws Exception {
        PdpClient pdpClient = pdpClientRepository.findByClientId(TEST_PDP_CLIENT);
        pdpClient.setRoles(Collections.emptySet());
        pdpClientRepository.save(pdpClient);

        this.mockMvc.perform(get(API_PREFIX_V1 + FHIR_PREFIX + "/Patient/$export")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(403));

        verify(sqsEventClient, timeout(10000).times(1)).sendLogs(captor.capture());
        ApiRequestEvent requestEvent = (ApiRequestEvent)captor.getValue();

        verify(sqsEventClient, timeout(10000).times(1)).logAndAlert(captor.capture(), any());
        ApiResponseEvent responseEvent = (ApiResponseEvent) captor.getValue();

        assertEquals(HttpStatus.FORBIDDEN.value(), responseEvent.getResponseCode());
        assertEquals(requestEvent.getRequestId(), responseEvent.getRequestId());
    }


    @Test
    public void testSwaggerUrlWorks() throws Exception {
        this.mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().is(200));
    }

    private static final String V3_EXPORT = API_PREFIX_V3 + FHIR_PREFIX + "/Patient/$export";

    private ResultActions performV3(MockHttpServletRequestBuilder request) throws Exception {
        return this.mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON));
    }

    private void expectV3Unauthorized(ResultActions result, String detail) throws Exception {
        result.andExpect(status().is(401))
                .andExpect(jsonPath("$.resourceType").value("OperationOutcome"))
                .andExpect(jsonPath("$.issue[0].severity").value("error"))
                .andExpect(jsonPath("$.issue[0].details.text", containsString(detail)));
    }

    @Test
    void testV3NoAuthHeader() throws Exception {
        expectV3Unauthorized(performV3(get(V3_EXPORT)), "Authorization header for token was not present");
    }

    @Test
    void testV3BadStartToHeader() throws Exception {
        expectV3Unauthorized(performV3(get(V3_EXPORT).header("Authorization", "NotBearer")),
                "Authorization header must start with");
    }

    @Test
    void testV3NoTokenInHeader() throws Exception {
        expectV3Unauthorized(performV3(get(V3_EXPORT).header("Authorization", "Bearer ")),
                "Did not receive a token for JWT authentication");
    }

    @Test
    void testV3InvalidToken() throws Exception {
        when(mockAccessTokenVerifier.decode(anyString())).thenThrow(new JwtVerificationException("Failed to parse token"));

        expectV3Unauthorized(performV3(get(V3_EXPORT).header("Authorization", "Bearer " + token)),
                "Token is invalid or expired");

        verify(sqsEventClient, times(2)).sendLogs(captor.capture());
        ApiResponseEvent responseEvent = (ApiResponseEvent) captor.getAllValues().get(1);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), responseEvent.getResponseCode());
    }

    @Test
    void testV3ClientDoesNotExist() throws Exception {
        PdpClient pdpClient = pdpClientRepository.findByClientId(TEST_PDP_CLIENT);
        pdpClientRepository.delete(pdpClient);

        expectV3Unauthorized(performV3(get(V3_EXPORT).header("Authorization", "Bearer " + token)),
                "Client was not found");
    }

    @Test
    void testV3ClientIsNotEnabled() throws Exception {
        PdpClient pdpClient = pdpClientRepository.findByClientId(TEST_PDP_CLIENT);
        pdpClient.setEnabled(false);
        pdpClientRepository.save(pdpClient);

        performV3(get(V3_EXPORT).header("Authorization", "Bearer " + token))
                .andExpect(status().is(403))
                .andExpect(jsonPath("$.resourceType").value("OperationOutcome"))
                .andExpect(jsonPath("$.issue[0].details.text", containsString("is not enabled")));
    }
}
