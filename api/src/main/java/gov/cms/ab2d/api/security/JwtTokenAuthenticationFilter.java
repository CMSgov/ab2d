package gov.cms.ab2d.api.security;

import com.google.common.base.Strings;
import com.okta.jwt.AccessTokenVerifier;
import com.okta.jwt.Jwt;
import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.service.ResourceNotFoundException;
import gov.cms.ab2d.common.service.PdpClientService;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.ApiRequestEvent;
import gov.cms.ab2d.eventlogger.utils.UtilMethods;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.annotation.PostConstruct;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static gov.cms.ab2d.common.util.Constants.*;

@Slf4j
@Component
@SuppressWarnings({"PMD.TooManyStaticImports", "PMD.UnusedPrivateMethod"})
public class JwtTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final List<String> SWAGGER_LIST = List.of(
            "/swagger-ui", "/webjars", "/swagger-resources", "/v3/api-docs",
            "/configuration", "/error"
    );

    private final AccessTokenVerifier accessTokenVerifier;
    private final PdpClientService pdpClientService;
    private final JwtConfig jwtConfig;
    private final LogManager eventLogger;

    // Filters for public URIs
    private final String uriFilters;

    // Predicate used for filtering public uris
    // If predicate.test("uri") -> true then URI does not match any regex filters and should be logged
    // If predicate.test("uri") -> false then URI does match at least one regex filter and should not be logged
    private Predicate<String> uriFilter;

    public JwtTokenAuthenticationFilter(AccessTokenVerifier accessTokenVerifier, PdpClientService pdpClientService,
                                        JwtConfig jwtConfig, LogManager eventLogger,
                                        @Value("${api.requestlogging.filter:#{null}}") String uriFilters) {
        this.accessTokenVerifier = accessTokenVerifier;
        this.pdpClientService = pdpClientService;
        this.jwtConfig = jwtConfig;
        this.eventLogger = eventLogger;
        this.uriFilters = uriFilters;
    }

    @PostConstruct
    private void constructFilters() {

        // Check whether no filters were provided
        if (StringUtils.isBlank(uriFilters)) {
            log.warn("no filters provided so all api requests will be logged");
            uriFilter = uri -> true;
            return;
        }

        List<String> filters = List.of(uriFilters.split(",")).stream()
                .filter(StringUtils::isNotBlank).collect(Collectors.toList());

        if (filters.isEmpty()) {
            log.warn("all filters provided are empty so all api requests will be logged");
            uriFilter = uri -> true;
            return;
        }

        // Compiled filters, much quicker if patterns are pre-compiled
        List<Predicate<String>> compiledFilters = filters.stream()
                .filter(StringUtils::isNotBlank)
                .map(Pattern::compile).map(Pattern::asPredicate)
                .collect(Collectors.toList());

        // Reduce filters to single predicate statement
        uriFilter = compiledFilters.stream().reduce(Predicate::or).orElse(uri -> false).negate();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String jobId = UtilMethods.parseJobId(request.getRequestURI());

        if (shouldBePublic(request.getRequestURI())) {
            if (uriFilter.test(request.getRequestURI())) {
                logApiRequestEvent(request, null, null, jobId);
            }
            chain.doFilter(request, response);
            return;
        }

        String token = null;
        String client;

        try {
            token = getToken(request);
            client = getClientId(token);
        } catch (Exception ex) {
            logApiRequestEvent(request, token, null, jobId);
            throw ex;
        }

        if (client.isEmpty()) {
            logApiRequestEvent(request, token, null, jobId);

            String clientBlankMsg = "Client id was blank";
            log.error(clientBlankMsg);
            throw new BadJWTTokenException(clientBlankMsg);
        }

        // Attempt to get client object from repository (to check whether enabled and setup roles if enabled)
        PdpClient pdpClient;
        try {
            pdpClient = pdpClientService.getClientById(client);
        } catch (ResourceNotFoundException exception) {
            logApiRequestEvent(request, token, null, jobId);
            throw new UsernameNotFoundException("Client was not found");
        }

        // If client is null then continue throwing username not found
        if (pdpClient == null) {
            logApiRequestEvent(request, token, null, jobId);
            throw new UsernameNotFoundException("Client was not found");
        }

        // Save organization
        MDC.put(ORGANIZATION, pdpClient.getOrganization());

        // If client is disabled for any reason do not proceed
        if (!pdpClient.getEnabled()) {
            log.error("Client {} is not enabled", pdpClient.getOrganization());
            logApiRequestEvent(request, token, pdpClient.getOrganization(), jobId);
            throw new ClientNotEnabledException("Client " + pdpClient.getOrganization() + " is not enabled");
        }

        // Otherwise setup roles and context
        logApiRequestEvent(request, token, pdpClient.getOrganization(), jobId);
        pdpClientService.setupClientAndRolesInSecurityContext(pdpClient, request);

        // go to the next filter in the filter chain
        chain.doFilter(request, response);
    }

    private void logApiRequestEvent(HttpServletRequest request, String token, String organization, String jobId) {
        String url = UtilMethods.getURL(request);
        String uniqueId = UUID.randomUUID().toString();
        ApiRequestEvent requestEvent = new ApiRequestEvent(organization, jobId, url, UtilMethods.getIpAddress(request),
                token, uniqueId);
        eventLogger.log(requestEvent);

        request.setAttribute(REQUEST_ID, uniqueId);
    }

    /**
     * Retrieve the client id from a JWT token
     *
     * @param token - the token
     * @return - the {@link PdpClient#getClientId()}
     */
    private String getClientId(String token) {
        Jwt jwt;
        try {
            jwt = accessTokenVerifier.decode(token);
        } catch (JwtVerificationException e) {
            log.error("Unable to decode JWT token {}", e.getMessage());
            throw new BadJWTTokenException("Unable to decode JWT token", e);
        }

        Object subClaim = jwt.getClaims().get("sub");
        if (subClaim == null) {
            String tokenErrorMsg = "Token did not contain client id field";
            log.error(tokenErrorMsg);
            throw new BadJWTTokenException(tokenErrorMsg);
        }
        return subClaim.toString();
    }

    /**
     * Return true if the page should be publically available without authorization. Examples
     * include health check, swagger pages, etc.
     *
     * @param requestUri - the URL requested
     * @return true if it's public
     */
    private boolean shouldBePublic(String requestUri) {
        if (SWAGGER_LIST.stream().anyMatch(requestUri::startsWith)) {
            log.debug("Swagger requested");
            return true;
        }

        if (requestUri.contains("/favicon.ico")) {
            return true;
        }

        if (requestUri.startsWith("/akamai-test-object.html")) {
            log.debug("Akamai requested");
            return true;
        }

        if (requestUri.startsWith(HEALTH_ENDPOINT) || requestUri.startsWith(STATUS_ENDPOINT)) {
            log.debug("Health or maintenance requested");
            return true;
        }

        return false;
    }

    /**
     * Retrieve the value of the bearer token from the header
     *
     * @param request - the request object
     * @return - the value of the bearer token
     */
    private String getToken(HttpServletRequest request) {
        String header = request.getHeader(jwtConfig.getHeader());
        if (header == null) {
            String noHeaderMsg = "Authorization header for token was not present";
            log.error(noHeaderMsg);
            throw new InvalidAuthHeaderException(noHeaderMsg);
        }

        if (!header.startsWith(jwtConfig.getPrefix())) {
            log.error("Header did not start with prefix {}", jwtConfig.getPrefix());
            throw new InvalidAuthHeaderException("Authorization header must start with " + jwtConfig.getPrefix());
        }

        String token = header.replace(jwtConfig.getPrefix(), "");

        if (Strings.isNullOrEmpty(token)) {
            String emptyTokenMsg = "Did not receive a token for JWT authentication";
            log.error(emptyTokenMsg);
            throw new MissingTokenException(emptyTokenMsg);
        }
        return token;
    }
}
