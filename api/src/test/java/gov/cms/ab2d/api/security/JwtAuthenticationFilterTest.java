package gov.cms.ab2d.api.security;

import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.api.controller.TestUtil;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.repository.RoleRepository;
import gov.cms.ab2d.common.repository.SponsorRepository;
import gov.cms.ab2d.common.repository.UserRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.events.ApiRequestEvent;
import gov.cms.ab2d.eventlogger.reports.sql.DoAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static gov.cms.ab2d.api.util.Constants.ADMIN_ROLE;
import static gov.cms.ab2d.common.util.Constants.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Disabled
@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class JwtAuthenticationFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUtil testUtil;

    @Autowired
    private SponsorRepository sponsorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private DoAll doAll;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    private String token;

    @BeforeEach
    public void before() {
        try {
            token = testUtil.setupToken(List.of(ADMIN_ROLE));
        } catch (Exception exception) {
            fail("could not create token for tests", exception);
        }
    }

    @Test
    void filterPublicRequests() {


        try {
            mockMvc.perform(get(API_PREFIX + HEALTH_ENDPOINT))
                    .andExpect(status().is(200));
        } catch (Exception exception) {
            fail("could not perform basic health check", exception);
        }

        List<LoggableEvent> currentEvents = doAll.load(ApiRequestEvent.class);

        if (!currentEvents.isEmpty()) {
            currentEvents.sort(Comparator.comparing(LoggableEvent::getTimeOfEvent));
            Collections.reverse(currentEvents);

            ApiRequestEvent lastRequest = (ApiRequestEvent) currentEvents.get(0);
            assertFalse(lastRequest.getUrl().contains(HEALTH_ENDPOINT), "health endpoint should be filtered");
        }
    }

    @Test
    void filterAuthenticatedRequests() {


        try {
            mockMvc.perform(get(API_PREFIX + FHIR_PREFIX + "/Patient/$export")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(401));

        } catch (Exception exception) {
            fail("could not perform basic health check");
        }

        List<LoggableEvent> currentEvents = doAll.load(ApiRequestEvent.class);
    }

    @Test
    public void logRequests() {

    }

    @Test
    public void emptyFilterListIsValid() {

    }
}
