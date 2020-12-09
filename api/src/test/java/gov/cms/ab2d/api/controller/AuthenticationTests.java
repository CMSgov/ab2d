package gov.cms.ab2d.api.controller;

import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.events.ApiRequestEvent;
import gov.cms.ab2d.eventlogger.events.ApiResponseEvent;
import gov.cms.ab2d.eventlogger.reports.sql.DoAll;
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

import java.util.List;

import static gov.cms.ab2d.common.util.Constants.*;
import static gov.cms.ab2d.common.util.DataSetup.TEST_USER;
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
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private DoAll doAll;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    private String token;

    @BeforeEach
    public void setup() throws JwtVerificationException {
        jobRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        contractRepository.deleteAll();
        doAll.delete();

        token = testUtil.setupToken(List.of(SPONSOR_ROLE));
    }

    // Negative tests, successful auth tests are essentially done in other suites
    @Test
    public void testNoAuthHeader() throws Exception {
        this.mockMvc.perform(get(API_PREFIX + FHIR_PREFIX + "/Patient/$export")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(401));
        List<LoggableEvent> apiRequestEvents = doAll.load(ApiRequestEvent.class);
        assertEquals(1, apiRequestEvents.size());
        ApiRequestEvent requestEvent = (ApiRequestEvent) apiRequestEvents.get(0);
        List<LoggableEvent> apiResponseEvents = doAll.load(ApiResponseEvent.class);
        assertEquals(1, apiResponseEvents.size());
        ApiResponseEvent responseEvent = (ApiResponseEvent) apiResponseEvents.get(0);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), responseEvent.getResponseCode());
        assertEquals(requestEvent.getRequestId(), responseEvent.getRequestId());
    }

    @Test
    public void testBadStartToHeader() throws Exception {
        this.mockMvc.perform(get(API_PREFIX + FHIR_PREFIX + "/Patient/$export")
                .header("Authorization", "NotBearer")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(401));
    }

    @Test
    public void testNoTokenInHeader() throws Exception {
        this.mockMvc.perform(get(API_PREFIX + FHIR_PREFIX + "/Patient/$export")
                .header("Authorization", "Bearer ")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(401));
    }

    @Test
    public void testUserDoesNotExist() throws Exception {
        User user = userRepository.findByUsername(TEST_USER);
        userRepository.delete(user);

        this.mockMvc.perform(get(API_PREFIX + FHIR_PREFIX + "/Patient/$export")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(403));
        List<LoggableEvent> apiRequestEvents = doAll.load(ApiRequestEvent.class);
        assertEquals(1, apiRequestEvents.size());
        ApiRequestEvent requestEvent = (ApiRequestEvent) apiRequestEvents.get(0);
        List<LoggableEvent> apiResponseEvents = doAll.load(ApiResponseEvent.class);
        assertEquals(1, apiResponseEvents.size());
        ApiResponseEvent responseEvent = (ApiResponseEvent) apiResponseEvents.get(0);
        assertEquals(HttpStatus.FORBIDDEN.value(), responseEvent.getResponseCode());
        assertEquals(requestEvent.getRequestId(), responseEvent.getRequestId());
    }

    @Test
    public void testUserIsNotEnabled() throws Exception {
        User user = userRepository.findByUsername(TEST_USER);
        user.setEnabled(false);
        userRepository.save(user);

        this.mockMvc.perform(get(API_PREFIX + FHIR_PREFIX + "/Patient/$export")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(403));
        List<LoggableEvent> apiRequestEvents = doAll.load(ApiRequestEvent.class);
        assertEquals(1, apiRequestEvents.size());
        ApiRequestEvent requestEvent = (ApiRequestEvent) apiRequestEvents.get(0);
        List<LoggableEvent> apiResponseEvents = doAll.load(ApiResponseEvent.class);
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
