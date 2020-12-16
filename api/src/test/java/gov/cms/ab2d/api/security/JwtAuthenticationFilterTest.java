package gov.cms.ab2d.api.security;

import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.api.controller.TestUtil;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.events.ApiRequestEvent;
import gov.cms.ab2d.eventlogger.reports.sql.DoAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;
import java.util.List;

import static gov.cms.ab2d.api.util.Constants.ADMIN_ROLE;
import static gov.cms.ab2d.common.util.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Uses {@link DirtiesContext} to indicate that all components/services need to be reloaded after these tests are run.
 * The default uri filters for the JwtTokenAuthenticationFilter are repeatedly modified
 * which could cause unexpected behavior in other tests.
 */
@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode =  DirtiesContext.ClassMode.AFTER_CLASS)
class JwtAuthenticationFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUtil testUtil;

    @Autowired
    private JwtTokenAuthenticationFilter filter;

    @Autowired
    private DoAll doAll;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    /**
     * Depending on the order that other test classes are run in these tests can fail because of residual
     * events in the PostgresSQLContainer. Using a {@link BeforeEach} is unnecessary after the first test but allows
     * Spring to autowire in dependencies which a static {@link org.junit.jupiter.api.BeforeAll} would not allow.
     */
    @BeforeEach
    @AfterEach
    public void cleanup() {
        userRepository.deleteAll();
        roleRepository.deleteAll();
        contractRepository.deleteAll();
        doAll.delete();
    }

    @Test
    void testDefaultMatchesHealthOnly() {

        rebuildFilters("^/health$");

        try {
            mockMvc.perform(get(HEALTH_ENDPOINT))
                    .andExpect(status().is(200));

            mockMvc.perform(get("/healthy"))
                    .andExpect(status().is(403));
        } catch (Exception exception) {
            fail("could not perform basic health check", exception);
        }

        List<LoggableEvent> currentEvents = doAll.load(ApiRequestEvent.class);

        assertEquals(1, currentEvents.size(), "healthy should still be logged");
    }

    @Test
    void testMatchFilters() {

        rebuildFilters("/health.*", "/status.*");

        try {
            mockMvc.perform(get(HEALTH_ENDPOINT))
                    .andExpect(status().is(200));

            mockMvc.perform(get(STATUS_ENDPOINT))
                    .andExpect(status().is(200));
        } catch (Exception exception) {
            fail("could not perform basic health check", exception);
        }

        List<LoggableEvent> currentEvents = doAll.load(ApiRequestEvent.class);
        assertEquals(0, currentEvents.size(), "no events should be logged");
    }

    @Test
    void testRegexFunctions() {

        rebuildFilters("/heal*");

        try {
            mockMvc.perform(get(HEALTH_ENDPOINT))
                    .andExpect(status().is(200));

        } catch (Exception exception) {
            fail("could not perform basic health check", exception);
        }

        List<LoggableEvent> currentEvents = doAll.load(ApiRequestEvent.class);
        assertEquals(0, currentEvents.size(), "health endpoint should match regex");
    }

    @Test
    void testNotMatchFilters() {

        rebuildFilters("/health.*");

        try {
            // Expect a 404 because the static site is not yet included in the constructed JAR/resources
            // However, this URI is whitelisted as public so it serves for the test
            mockMvc.perform(get("/swagger-ui"))
                    .andExpect(status().is(404));

        } catch (Exception exception) {
            fail("mock mvc call to pull swagger-ui returned unexpected status code", exception);
        }

        List<LoggableEvent> currentEvents = doAll.load(ApiRequestEvent.class);
        assertEquals(1, currentEvents.size(), "request for swagger docs not logged");
    }

    @Test
    void testAuthenticatedNotFiltered() {

        rebuildFilters(".*");

        try {
            String token = testUtil.setupToken(List.of(ADMIN_ROLE));

            // Expect call to fail but just want to check that event was logged
            this.mockMvc.perform(
                    get(API_PREFIX + ADMIN_PREFIX + "/user/userNotFound")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + token))
                    .andReturn();

            List<LoggableEvent> currentEvents = doAll.load(ApiRequestEvent.class);
            assertEquals(1, currentEvents.size(), "request for user not logged");
        } catch (JwtVerificationException exception) {
            fail("jwt token for test could not be retrieved");
        } catch (Exception exception) {
            fail("despite filtering all public uris, authenticated requests should be processed", exception);
        }
    }

    @Test
    void testNoFiltersNothingApplied() {

        rebuildFilters("");

        try {
            String token = testUtil.setupToken(List.of(ADMIN_ROLE));

            // Expect call to fail but just want to check that event was logged
            mockMvc.perform(get("/swagger-ui"))
                    .andExpect(status().is(404));

            List<LoggableEvent> currentEvents = doAll.load(ApiRequestEvent.class);
            assertEquals(1, currentEvents.size(), "request for user not logged");
        } catch (JwtVerificationException exception) {
            fail("jwt token for test could not be retrieved");
        } catch (Exception exception) {
            fail("despite filtering all public uris, authenticated requests should be processed", exception);
        }
    }

    @Test
    void testNullFiltersNothingApplied() {

        ReflectionTestUtils.setField(filter, "uriFilters", null);
        ReflectionTestUtils.invokeMethod(filter, "constructFilters");

        try {
            String token = testUtil.setupToken(List.of(ADMIN_ROLE));

            // Expect call to fail but just want to check that event was logged
            mockMvc.perform(get("/swagger-ui"))
                    .andExpect(status().is(404));

            List<LoggableEvent> currentEvents = doAll.load(ApiRequestEvent.class);
            assertEquals(1, currentEvents.size(), "request for user not logged");
        } catch (JwtVerificationException exception) {
            fail("jwt token for test could not be retrieved");
        } catch (Exception exception) {
            fail("despite filtering all public uris, authenticated requests should be processed", exception);
        }
    }

    @Test
    void testAllFiltersEmptyThenNothingApplied() {

        rebuildFilters("", "   ", "   ");

        try {
            String token = testUtil.setupToken(List.of(ADMIN_ROLE));

            // Expect call to fail but just want to check that event was logged
            mockMvc.perform(get("/swagger-ui"))
                    .andExpect(status().is(404));

            List<LoggableEvent> currentEvents = doAll.load(ApiRequestEvent.class);
            assertEquals(1, currentEvents.size(), "request for user not logged");
        } catch (JwtVerificationException exception) {
            fail("jwt token for test could not be retrieved");
        } catch (Exception exception) {
            fail("despite filtering all public uris, authenticated requests should be processed", exception);
        }
    }

    @Test
    void testEmptyFiltersNotApplied() {

        rebuildFilters("", "   ", "/health.*");

        try {
            String token = testUtil.setupToken(List.of(ADMIN_ROLE));

            // Expect call to fail but just want to check that event was logged
            mockMvc.perform(get(HEALTH_ENDPOINT))
                    .andExpect(status().is(200));

            // Expect call to fail but just want to check that event was logged
            mockMvc.perform(get("/swagger-ui"))
                    .andExpect(status().is(404));

            List<LoggableEvent> currentEvents = doAll.load(ApiRequestEvent.class);
            assertEquals(1, currentEvents.size(), "health endpoint should be blocked and swagger endpoint allowed");
        } catch (JwtVerificationException exception) {
            fail("jwt token for test could not be retrieved");
        } catch (Exception exception) {
            fail("despite filtering all public uris, authenticated requests should be processed", exception);
        }
    }


    private void rebuildFilters(String ... regexes) {
        ReflectionTestUtils.setField(filter, "uriFilters", String.join(",", regexes));
        ReflectionTestUtils.invokeMethod(filter, "constructFilters");
    }
}
