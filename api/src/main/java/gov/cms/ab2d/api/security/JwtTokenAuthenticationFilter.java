package gov.cms.ab2d.api.security;

import com.google.common.base.Strings;
import com.okta.jwt.AccessTokenVerifier;
import com.okta.jwt.Jwt;
import com.okta.jwt.JwtVerificationException;
import com.okta.jwt.JwtVerifiers;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;

@Component
public class JwtTokenAuthenticationFilter extends OncePerRequestFilter {

    private final AccessTokenVerifier jwtVerifier;

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private UserService userService;

    public JwtTokenAuthenticationFilter(JwtConfig jwtConfig, UserService userService) {
        this.jwtConfig = jwtConfig;
        this.userService = userService;

        jwtVerifier = JwtVerifiers.accessTokenVerifierBuilder()
                .setIssuer("https://dev-418212.okta.com/oauth2/default")
                .setAudience("api://default")                // defaults to 'api://default'
                .setConnectionTimeout(Duration.ofSeconds(1)) // defaults to 1s
                .setReadTimeout(Duration.ofSeconds(1))       // defaults to 1s
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(jwtConfig.getHeader());

        if (header == null) {
            throw new InvalidAuthHeaderException("Authorization header for token was not present");
        }

        if (!header.startsWith(jwtConfig.getPrefix())) {
            throw new InvalidAuthHeaderException("Authorization header must start with " + jwtConfig.getPrefix());
        }

        String token = header.replace(jwtConfig.getPrefix(), "");

        if (Strings.isNullOrEmpty(token)) {
            throw new MissingTokenException("Token was not present");
        }

        Jwt jwt;
        try {
            jwt = jwtVerifier.decode(token);
        } catch (JwtVerificationException e) {
            throw new BadJWTTokenException("Unable to decode JWT token", e);
        }

        Object subClaim = jwt.getClaims().get("sub");
        if (subClaim == null) {
            throw new BadJWTTokenException("Token did not contain username field");
        }
        String username = subClaim.toString();

        if (username != null) {
            User user = userService.getUserByUsername(username);
            if (user == null) {
                throw new UserNotFoundException("User " + username + " is not present in our database");
            }

            if (!user.getEnabled()) {
                throw new UserNotEnabledException("User " + username + " is not enabled");
            }

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    username, null, null);

            SecurityContextHolder.getContext().setAuthentication(auth);
        } else {
            throw new BadJWTTokenException("Username was blank");
        }

        // go to the next filter in the filter chain
        chain.doFilter(request, response);
    }

}
