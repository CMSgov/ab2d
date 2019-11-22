package gov.cms.ab2d.api.controller;

import com.okta.jwt.AccessTokenVerifier;
import com.okta.jwt.Jwt;
import com.okta.jwt.JwtVerificationException;
import com.okta.jwt.impl.DefaultJwt;
import gov.cms.ab2d.common.model.Role;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.repository.SponsorRepository;
import gov.cms.ab2d.common.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Component
public class TestUtil {

    public static final String TEST_USER = "EileenCFrierson@example.com";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SponsorRepository sponsorRepository;

    @MockBean
    AccessTokenVerifier mockAccessTokenVerifier;

    @Value("${api.okta-url}")
    private String oktaUrl;

    private String jwtStr = null;

    private void setupMock() throws JwtVerificationException {
        // Token that expires in 2 hours that has the user we setup below
        Map<String, Object> claims = Map.of("sub", TEST_USER);
        Jwt jwt = new DefaultJwt("tokenValue", Instant.now(), Instant.now().plus(Duration.ofHours(2)), claims);

        when(mockAccessTokenVerifier.decode(anyString())).thenReturn(jwt);
    }

    private void setupInvalidMock() throws JwtVerificationException {
        when(mockAccessTokenVerifier.decode(anyString())).thenThrow(JwtVerificationException.class);
    }

    public String setupInvalidToken(List<String> userRoles) throws JwtVerificationException {
        setupUser(userRoles);

        setupInvalidMock();

        return buildTokenStr();
    }

    public String setupToken(List<String> userRoles) throws JwtVerificationException {
        setupUser(userRoles);

        setupMock();

        return buildTokenStr();
    }

    private String buildTokenStr() {
        if(jwtStr != null) {
            return jwtStr;
        }

        String clientSecret = "wefikjweglkhjwelgkjweglkwegwegewg";
        SecretKey sharedSecret = Keys.hmacShaKeyFor(clientSecret.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();

        jwtStr = Jwts.builder()
                .setAudience(oktaUrl)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(2L, ChronoUnit.HOURS)))
                .setIssuer(TEST_USER)
                .setSubject(TEST_USER)
                .setId(UUID.randomUUID().toString())
                .signWith(sharedSecret)
                .compact();

        return jwtStr;
    }

    private void setupUser(List<String> userRoles) {
        User testUser = userRepository.findByUsername(TEST_USER);
        if(testUser != null) {
            return;
        }

        Sponsor parent = new Sponsor();
        parent.setOrgName("Parent Corp.");
        parent.setHpmsId(456);
        parent.setLegalName("Parent Corp.");

        Sponsor sponsor = new Sponsor();
        sponsor.setOrgName("Test");
        sponsor.setHpmsId(123);
        sponsor.setLegalName("Test");
        sponsor.setParent(parent);
        Sponsor savedSponsor = sponsorRepository.save(sponsor);

        User user = new User();
        user.setEmail(TEST_USER);
        user.setFirstName("Eileen");
        user.setLastName("Frierson");
        user.setUsername(TEST_USER);
        user.setSponsor(savedSponsor);
        user.setEnabled(true);
        for(String userRole :  userRoles) {
            Role role = new Role();
            role.setName(userRole);
            user.addRole(role);
        }
        userRepository.save(user);
    }
}
