package gov.cms.ab2d.api.security;

import com.google.common.base.Strings;
import com.okta.jwt.AccessTokenVerifier;
import com.okta.jwt.Jwt;
import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.common.model.Role;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.service.ResourceNotFoundException;
import gov.cms.ab2d.common.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class JwtTokenAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private AccessTokenVerifier accessTokenVerifier;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtConfig jwtConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestUri = request.getRequestURI();
        // These need to be here and in SecurityConfig, which uses the antMatchers with ** to do filtering
        if (requestUri.startsWith("/swagger-ui") || requestUri.startsWith("/webjars") || requestUri.startsWith("/swagger-resources") ||
            requestUri.startsWith("/v2/api-docs") || requestUri.startsWith("/configuration")) {
            log.info("Swagger requested");
            chain.doFilter(request, response);
            return;
        }

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
        String username = subClaim.toString();

        if (username != null) {
            MDC.put("username", username);
            User user;
            try {
                user = userService.getUserByUsername(username);
            } catch (ResourceNotFoundException exception) {
                throw new UsernameNotFoundException("User was not found");
            }

            if (!user.getEnabled()) {
                log.error("User is not enabled");
                throw new UserNotEnabledException("User " + username + " is not enabled");
            }

            List<GrantedAuthority> authorities = new ArrayList<>();
            for (Role role : user.getRoles()) {
                log.info("Adding role {}", role.getName());
                authorities.add(new SimpleGrantedAuthority(role.getName()));
            }
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    username, null, authorities);
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            log.info("Successfully logged in");
            SecurityContextHolder.getContext().setAuthentication(auth);
        } else {
            String usernameBlankMsg = "Username was blank";
            log.error(usernameBlankMsg);
            throw new BadJWTTokenException(usernameBlankMsg);
        }

        // go to the next filter in the filter chain
        chain.doFilter(request, response);
    }
}
