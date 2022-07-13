package gov.cms.ab2d.api.controller;

import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.eventclient.events.ApiRequestEvent;
import gov.cms.ab2d.eventclient.events.ApiResponseEvent;
import gov.cms.ab2d.eventclient.events.LoggableEvent;
import gov.cms.ab2d.eventlogger.reports.sql.LoggerEventRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;
import java.util.List;

import static gov.cms.ab2d.common.model.Role.SPONSOR_ROLE;
import static gov.cms.ab2d.common.util.Constants.*;
import static gov.cms.ab2d.common.util.DataSetup.TEST_PDP_CLIENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
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
    private LoggerEventRepository loggerEventRepository;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    private String token;

    @BeforeEach
    public void setup() throws JwtVerificationException {
        token = testUtil.setupToken(List.of(SPONSOR_ROLE));
    }

    @AfterEach
    public void cleanup() {
        loggerEventRepository.delete();
        dataSetup.cleanup();
    }

    // Negative tests, successful auth tests are essentially done in other suites
    @Test
    public void testNoAuthHeader() throws Exception {
        this.mockMvc.perform(get(API_PREFIX_V1 + FHIR_PREFIX + "/Patient/$export")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(401));
        List<LoggableEvent> apiRequestEvents = loggerEventRepository.load(ApiRequestEvent.class);
        assertEquals(1, apiRequestEvents.size());
        ApiRequestEvent requestEvent = (ApiRequestEvent) apiRequestEvents.get(0);
        List<LoggableEvent> apiResponseEvents = loggerEventRepository.load(ApiResponseEvent.class);
        assertEquals(1, apiResponseEvents.size());
        ApiResponseEvent responseEvent = (ApiResponseEvent) apiResponseEvents.get(0);
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
        List<LoggableEvent> apiRequestEvents = loggerEventRepository.load(ApiRequestEvent.class);
        assertEquals(1, apiRequestEvents.size());
        ApiRequestEvent requestEvent = (ApiRequestEvent) apiRequestEvents.get(0);
        List<LoggableEvent> apiResponseEvents = loggerEventRepository.load(ApiResponseEvent.class);
        assertEquals(1, apiResponseEvents.size());
        ApiResponseEvent responseEvent = (ApiResponseEvent) apiResponseEvents.get(0);
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
        List<LoggableEvent> apiRequestEvents = loggerEventRepository.load(ApiRequestEvent.class);
        assertEquals(1, apiRequestEvents.size());
        ApiRequestEvent requestEvent = (ApiRequestEvent) apiRequestEvents.get(0);
        List<LoggableEvent> apiResponseEvents = loggerEventRepository.load(ApiResponseEvent.class);
        assertEquals(1, apiResponseEvents.size());
        ApiResponseEvent responseEvent = (ApiResponseEvent) apiResponseEvents.get(0);
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
        List<LoggableEvent> apiRequestEvents = loggerEventRepository.load(ApiRequestEvent.class);
        assertEquals(1, apiRequestEvents.size());
        ApiRequestEvent requestEvent = (ApiRequestEvent) apiRequestEvents.get(0);
        List<LoggableEvent> apiResponseEvents = loggerEventRepository.load(ApiResponseEvent.class);
        assertEquals(1, apiResponseEvents.size());
        ApiResponseEvent responseEvent = (ApiResponseEvent) apiResponseEvents.get(0);
        assertEquals(HttpStatus.FORBIDDEN.value(), responseEvent.getResponseCode());
        assertEquals(requestEvent.getRequestId(), responseEvent.getRequestId());
    }

    @Test
    public void testSwaggerUrlWorks() throws Exception {
        this.mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().is(200));
    }
}
