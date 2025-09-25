package gov.cms.ab2d.api.security;

import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.service.PdpClientService;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import gov.cms.ab2d.eventclient.events.ApiResponseEvent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;

import static gov.cms.ab2d.common.model.Role.ADMIN_ROLE;
import static gov.cms.ab2d.common.model.Role.SPONSOR_ROLE;
import static gov.cms.ab2d.common.util.Constants.*;
import static gov.cms.ab2d.eventclient.events.SlackEvents.API_AUTHNZ_ERROR;

@Slf4j
@AllArgsConstructor
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final FilterChainExceptionHandler filterChainExceptionHandler;
    private final JwtTokenAuthenticationFilter jwtTokenAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;
    private final SQSEventClient eventLogger;
    private final PdpClientService pdpClientService;

    private final String[] authExceptions = new String[]{"/swagger-ui/**", "/configuration/**",
            "/swagger-resources/**", "/v3/api-docs/**", "/webjars/**",
            AKAMAI_TEST_OBJECT, "/favicon.ico", "/error", HEALTH_ENDPOINT, STATUS_ENDPOINT,
            "/**/metadata"};

    @SuppressWarnings("java:S4502")
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity security) throws Exception {
        security.csrf(AbstractHttpConfigurer::disable) // NOSONAR java:S4502 - stateless JWT API, no cookies/sessions
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(authExceptions).permitAll()
                        .requestMatchers(API_PREFIX_V1 + ADMIN_PREFIX + "/**").hasAuthority(ADMIN_ROLE)
                        .requestMatchers(API_PREFIX_V1 + FHIR_PREFIX + "/**").hasAnyAuthority(SPONSOR_ROLE)
                        .anyRequest().authenticated())
                // Setup filter exception handling
                .addFilterBefore(filterChainExceptionHandler, LogoutFilter.class)
                // Add a filter to validate the tokens with every request.
                .addFilterAfter(jwtTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Override default behavior to add more informative logs
        security.exceptionHandling(ex -> ex.accessDeniedHandler((request, response, accessDeniedException) -> {

                    // Log authorization errors like PDP does not have SPONSOR role
                    logSecurityException(request, accessDeniedException, HttpServletResponse.SC_FORBIDDEN);
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                })
                .authenticationEntryPoint((request, response, authException) -> {

                    // Log authentication errors that are not caught by JWT filter
                    logSecurityException(request, authException, HttpServletResponse.SC_UNAUTHORIZED);
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                }));
        return security.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        return new DaoAuthenticationProvider(customUserDetailsService);
    }

    private void logSecurityException(HttpServletRequest request, Exception securityException, int status) {

        try {
            String error = String.format(API_AUTHNZ_ERROR + " URL (%s), Exception (%s), Message (%s), Origin(%s)",
                    request.getRequestURL(), securityException.getClass(), securityException.getMessage(),
                    securityException.getStackTrace()[0].toString());

            // Attempt to log who made request
            PdpClient client = pdpClientService.getCurrentClient();
            if (client != null) {
                error += ", Organization(" + client.getOrganization() + ")";
            }

            // Log error for splunk detection
            log.warn(error);

            // Log api response event to a database for long term analytics
            ApiResponseEvent response = new ApiResponseEvent(MDC.get(ORGANIZATION), null, HttpStatus.resolve(status),
                    "API Error", error, (String) request.getAttribute(REQUEST_ID));
            eventLogger.logAndAlert(response, Ab2dEnvironment.PROD_LIST);
        } catch (Exception exception) {
            log.error("Could not additional logs for exception: " + exception.getCause());
        }
    }
}
