package gov.cms.ab2d.api.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.okta.jwt.Jwt;
import com.okta.jwt.AccessTokenVerifier;
import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.api.controller.TestUtil;
import gov.cms.ab2d.common.service.PdpClientService;
import gov.cms.ab2d.common.util.AB2DLocalstackContainer;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.AB2DSQSMockConfig;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import javax.servlet.FilterChain;

import static gov.cms.ab2d.common.model.Role.ADMIN_ROLE;
import static gov.cms.ab2d.common.util.Constants.*;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Uses {@link DirtiesContext} to indicate that all components/services need to be reloaded after these tests are run.
 * The default uri filters for the JwtTokenAuthenticationFilter are repeatedly modified
 * which could cause unexpected behavior in other tests.
 */
@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Import(AB2DSQSMockConfig.class)
@DirtiesContext(classMode =  DirtiesContext.ClassMode.AFTER_CLASS)
class JwtAuthenticationFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUtil testUtil;

    @Autowired
    private JwtTokenAuthenticationFilter filter;

    @Autowired
    DataSetup dataSetup;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Container
    private static final AB2DLocalstackContainer localstackContainer = new AB2DLocalstackContainer();

    @Autowired
    SQSEventClient sqsEventClient;

    @AfterEach
    public void cleanup() {
        dataSetup.cleanup();
    }

    @Test
    void testDefaultMatchesHealthOnly() throws JsonProcessingException {

        rebuildFilters("^/health$");

        try {
            mockMvc.perform(get(HEALTH_ENDPOINT))
                    .andExpect(status().is(200));

            mockMvc.perform(get("/healthy"))
                    .andExpect(status().is(401));
        } catch (Exception exception) {
            fail("could not perform basic health check", exception);
        }

        verify(sqsEventClient, times(1)).sendLogs(any());
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

        verify(sqsEventClient, never()).sendLogs(any());
    }

    @Test
    void testAkamaiTestObject() {

        rebuildFilters("^/health$","^/akamai-test-object.html$");

        try {
            mockMvc.perform(get(AKAMAI_TEST_OBJECT))
                    .andExpect(status().is(404));
        } catch (Exception exception) {
            fail("could not perform akamai test call", exception);
        }

        verify(sqsEventClient, never()).sendLogs(any());
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

        verify(sqsEventClient, never()).sendLogs(any());
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

        verify(sqsEventClient, atMostOnce()).sendLogs(any());
    }

    @Test
    void testAuthenticatedNotFiltered() {

        rebuildFilters(".*");

        try {
            String token = testUtil.setupToken(List.of(ADMIN_ROLE));

            // Expect call to fail but just want to check that event was logged
            this.mockMvc.perform(
                    get(API_PREFIX_V1 + ADMIN_PREFIX + "/client/clientNotFound")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer " + token))
                    .andReturn();

            verify(sqsEventClient, times(2)).sendLogs(any());
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

            verify(sqsEventClient, atMostOnce()).sendLogs(any());
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

            verify(sqsEventClient, atMostOnce()).sendLogs(any());
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

            verify(sqsEventClient, atMostOnce()).sendLogs(any());
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

            verify(sqsEventClient, atMostOnce()).sendLogs(any());
        } catch (JwtVerificationException exception) {
            fail("jwt token for test could not be retrieved");
        } catch (Exception exception) {
            fail("despite filtering all public uris, authenticated requests should be processed", exception);
        }
    }

    @Test
    void testDoFilterInternal1() throws Exception {
        PdpClientService pdpClientService = mock(PdpClientService.class);
        JwtConfig jwtConfig = new JwtConfig("Authorization", "Bearer ");
        SQSEventClient eventLogger = mock(SQSEventClient.class);

        AccessTokenVerifier accessTokenVerifier = mock(AccessTokenVerifier.class);
        Jwt jwt = mock(Jwt.class);
        when(accessTokenVerifier.decode(anyString())).thenReturn(jwt);
        when(jwt.getClaims()).thenReturn(new java.util.HashMap<>(){{
            put("sub", "fake.jwt");
        }});

        filter = new JwtTokenAuthenticationFilter(accessTokenVerifier, pdpClientService, jwtConfig, eventLogger, "");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer fake.jwt");

        assertThrows(UsernameNotFoundException.class, () -> {
            filter.doFilterInternal(request, null, null);
        });
    }

    @Test
    void testDoFilterInternal2() throws Exception {
        PdpClientService pdpClientService = mock(PdpClientService.class);
        JwtConfig jwtConfig = new JwtConfig("Authorization", "Bearer ");
        SQSEventClient eventLogger = mock(SQSEventClient.class);

        AccessTokenVerifier accessTokenVerifier = mock(AccessTokenVerifier.class);
        Jwt jwt = mock(Jwt.class);
        when(accessTokenVerifier.decode(anyString())).thenReturn(jwt);
        when(jwt.getClaims()).thenReturn(new java.util.HashMap<>(){{
            put("sub", null);
        }});

        filter = new JwtTokenAuthenticationFilter(accessTokenVerifier, pdpClientService, jwtConfig, eventLogger, "");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer fake.jwt");

        assertThrows(BadJWTTokenException.class, () -> {
            filter.doFilterInternal(request, null, null);
        });
    }

    @Test
    void testDoFilterInternal3() throws Exception {
        PdpClientService pdpClientService = mock(PdpClientService.class);
        JwtConfig jwtConfig = new JwtConfig("Authorization", "Bearer ");
        SQSEventClient eventLogger = mock(SQSEventClient.class);

        AccessTokenVerifier accessTokenVerifier = mock(AccessTokenVerifier.class);
        Jwt jwt = mock(Jwt.class);
        when(accessTokenVerifier.decode(anyString())).thenReturn(jwt);
        when(jwt.getClaims()).thenReturn(new java.util.HashMap<>(){{
            put("sub", "");
        }});

        filter = new JwtTokenAuthenticationFilter(accessTokenVerifier, pdpClientService, jwtConfig, eventLogger, "");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer fake.jwt");

        assertThrows(BadJWTTokenException.class, () -> {
            filter.doFilterInternal(request, null, null);
        });
    }

    @Test
    void testDoFilterInternal4() {
        PdpClientService pdpClientService = mock(PdpClientService.class);
        JwtConfig jwtConfig = new JwtConfig("Authorization", "Bearer ");
        SQSEventClient eventLogger = mock(SQSEventClient.class);
        AccessTokenVerifier accessTokenVerifier = mock(AccessTokenVerifier.class);
        FilterChain chain = mock(FilterChain.class);

        filter = new JwtTokenAuthenticationFilter(accessTokenVerifier, pdpClientService, jwtConfig, eventLogger, "test");
        ReflectionTestUtils.setField(filter, "uriFilters", String.join(",", "^/health$"));
        ReflectionTestUtils.invokeMethod(filter, "constructFilters");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/favicon.ico");

        assertDoesNotThrow(() -> {
            filter.doFilterInternal(request, null, chain);
        });
    }

    private void rebuildFilters(String ... regexes) {
        ReflectionTestUtils.setField(filter, "uriFilters", String.join(",", regexes));
        ReflectionTestUtils.invokeMethod(filter, "constructFilters");
    }
}
