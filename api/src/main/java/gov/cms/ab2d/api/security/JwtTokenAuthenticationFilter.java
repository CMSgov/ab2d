package gov.cms.ab2d.api.security;

import com.google.common.base.Strings;
import com.okta.jwt.AccessTokenVerifier;
import com.okta.jwt.Jwt;
import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.common.model.Role;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.service.UserService;
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

@Component
public class JwtTokenAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private AccessTokenVerifier jwtVerifier;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtConfig jwtConfig;

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
                throw new UsernameNotFoundException("User " + username + " is not present in our database");
            }

            if (!user.getEnabled()) {
                throw new UserNotEnabledException("User " + username + " is not enabled");
            }

            List<GrantedAuthority> authorities = new ArrayList<>();
            for (Role role : user.getRoles()) {
                authorities.add(new SimpleGrantedAuthority(role.getName()));
            }
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    username, null, authorities);
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(auth);
        } else {
            throw new BadJWTTokenException("Username was blank");
        }

        // go to the next filter in the filter chain
        chain.doFilter(request, response);
    }
}
