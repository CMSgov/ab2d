package gov.cms.ab2d.api.controller;

import com.okta.jwt.AccessTokenVerifier;
import com.okta.jwt.Jwt;
import com.okta.jwt.JwtVerificationException;
import com.okta.jwt.impl.DefaultJwt;
import gov.cms.ab2d.common.util.DataSetup;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static gov.cms.ab2d.common.util.DataSetup.TEST_USER;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Component
public class TestUtil {

    @Autowired
    private DataSetup dataSetup;

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
        dataSetup.setupUser(userRoles);

        setupInvalidMock();

        return buildTokenStr();
    }

    public String setupToken(List<String> userRoles) throws JwtVerificationException {
        dataSetup.setupUser(userRoles);

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
}
