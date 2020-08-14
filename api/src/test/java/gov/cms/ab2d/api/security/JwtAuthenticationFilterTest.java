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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static gov.cms.ab2d.common.util.Constants.*;
import static org.junit.jupiter.api.Assertions.*;
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

    @AfterEach
    public void after() {
        doAll.delete();
    }

    @Test
    void testMatchFilters() {

        try {
            mockMvc.perform(get(HEALTH_ENDPOINT))
                    .andExpect(status().is(200));

            mockMvc.perform(get(STATUS_ENDPOINT))
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
            assertFalse(lastRequest.getUrl().contains(STATUS_ENDPOINT), "status endpoint should be filtered");
        }
    }

    @Test
    void testNotMatchFilters() {
        try {
            // Expect a 404 because the static site is not yet included in the constructed JAR/resources
            // However, this URI is whitelisted as public so it serves for the test
            mockMvc.perform(get("/swagger-ui"))
                    .andExpect(status().is(404));

        } catch (Exception exception) {
            fail("mock mvc call to pull swagger-ui returned unexpected status code", exception);
        }

        List<LoggableEvent> currentEvents = doAll.load(ApiRequestEvent.class);

        assertEquals(1, currentEvents.size(), "should have allowed request for swagger docs to pass through");
    }
}
