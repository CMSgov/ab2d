package gov.cms.ab2d.api.security;

import com.google.common.base.Strings;
import com.okta.jwt.AccessTokenVerifier;
import com.okta.jwt.Jwt;
import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.service.ResourceNotFoundException;
import gov.cms.ab2d.common.service.UserService;
import gov.cms.ab2d.eventlogger.EventLogger;
import gov.cms.ab2d.eventlogger.events.ApiRequestEvent;
import gov.cms.ab2d.eventlogger.utils.UtilMethods;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

import static gov.cms.ab2d.common.util.Constants.USERNAME;
import static gov.cms.ab2d.common.util.Constants.REQUEST_ID;
import static gov.cms.ab2d.common.util.Constants.HEALTH_ENDPOINT;
import static gov.cms.ab2d.common.util.Constants.OKTA_PROXY_ENDPOINT;
import static gov.cms.ab2d.common.util.Constants.STATUS_ENDPOINT;

@Slf4j
@Component
@SuppressWarnings("PMD.TooManyStaticImports")
public class JwtTokenAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private AccessTokenVerifier accessTokenVerifier;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private EventLogger eventLogger;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String jobId = UtilMethods.parseJobId(request.getRequestURI());

        if (shouldBePublic(request.getRequestURI())) {
            String requestId = logApiRequestEvent(request, null, null, jobId);
            request.setAttribute(REQUEST_ID, requestId);
            chain.doFilter(request, response);
            return;
        }

        String token = null;
        String username = null;
        try {
            token = getToken(request);
            username = getUserName(token);
        } catch (Exception ex) {
            String requestId = logApiRequestEvent(request, token, username, jobId);
            request.setAttribute(REQUEST_ID, requestId);
            throw ex;
        }

        String requestId = logApiRequestEvent(request, token, username, jobId);
        request.setAttribute(REQUEST_ID, requestId);

        if (username != null) {
            MDC.put(USERNAME, username);
            User user = getUser(username);

            userService.setupUserAndRolesInSecurityContext(user, request);
        } else {
            String usernameBlankMsg = "Username was blank";
            log.error(usernameBlankMsg);
            throw new BadJWTTokenException(usernameBlankMsg);
        }

        // go to the next filter in the filter chain
        chain.doFilter(request, response);
    }

    private String logApiRequestEvent(HttpServletRequest request, String token, String username, String jobId) {
        String url = UtilMethods.getURL(request);
        String uniqueId = UUID.randomUUID().toString();
        ApiRequestEvent requestEvent = new ApiRequestEvent(username, jobId, url, UtilMethods.getIpAddress(request),
                token, uniqueId);
        eventLogger.log(requestEvent);
        return uniqueId;
    }

    /**
     * Given a user name, look up the user in the database
     *
     * @param username - the user name
     * @return - the user object
     */
    private User getUser(String username) {
        User user;
        try {
            user = userService.getUserByUsername(username);
        } catch (ResourceNotFoundException exception) {
            throw new UsernameNotFoundException("User was not found");
        }
        if (user == null) {
            throw new UsernameNotFoundException("User was not found");
        }

        if (!user.getEnabled()) {
            log.error("User is not enabled");
            throw new UserNotEnabledException("User " + username + " is not enabled");
        }
        return user;
    }

    /**
     * Retrieve the user name from a JWT token
     *
     * @param token - the token
     * @return - the user name
     */
    private String getUserName(String token) {
        Jwt jwt;
        try {
            jwt = accessTokenVerifier.decode(token);
        } catch (JwtVerificationException e) {
            log.error("Unable to decode JWT token {}", e.getMessage());
            throw new BadJWTTokenException("Unable to decode JWT token", e);
        }

        Object subClaim = jwt.getClaims().get("sub");
        if (subClaim == null) {
            String tokenErrorMsg = "Token did not contain username field";
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
        if (requestUri.startsWith("/swagger-ui") || requestUri.startsWith("/webjars") || requestUri.startsWith("/swagger-resources") ||
                requestUri.startsWith("/v2/api-docs") || requestUri.startsWith("/configuration")) {
            log.info("Swagger requested");
            return true;
        }

        if (requestUri.contains("favicon.ico")) {
            return true;
        }

        if (requestUri.startsWith(HEALTH_ENDPOINT) || requestUri.startsWith(STATUS_ENDPOINT) || requestUri.startsWith(OKTA_PROXY_ENDPOINT)) {
            log.info("Health, okta proxy, or maintenance requested");
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
