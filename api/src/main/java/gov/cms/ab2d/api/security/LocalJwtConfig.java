package gov.cms.ab2d.api.security;

import com.okta.jwt.AccessTokenVerifier;
import com.okta.jwt.Jwt;
import com.okta.jwt.JwtVerificationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Stubbed JWT verifier for local development. Skips Okta calls and accepts any
 * non-empty bearer token, attributing requests to {@code api.local.jwt.client-id}.
 * Active only under the {@code local} Spring profile — never wire this into a
 * deployed environment.
 */
@Slf4j
@Configuration
@Profile("local")
public class LocalJwtConfig {

    @Bean
    public AccessTokenVerifier accessTokenVerifier(
            @Value("${api.local.jwt.client-id:EileenCFrierson@example.com}") String clientId,
            @Value("${api.local.jwt.token-ttl-hours:2}") long tokenTtlHours) {
        log.warn("LOCAL PROFILE: JWT verification is stubbed; any bearer token will authenticate as '{}'", clientId);
        return token -> {
            if (token == null || token.isBlank()) {
                throw new JwtVerificationException("LOCAL stub rejected empty bearer token");
            }
            Instant now = Instant.now();
            return new StubJwt(token, now, now.plus(Duration.ofHours(tokenTtlHours)), Map.of("sub", clientId));
        };
    }

    private record StubJwt(String token, Instant issued, Instant expires, Map<String, Object> claims) implements Jwt {
        @Override public String getTokenValue() {
            return token;
        }
        @Override public Instant getIssuedAt() {
            return issued;
        }
        @Override public Instant getExpiresAt() {
            return expires;
        }
        @Override public Map<String, Object> getClaims() {
            return claims;
        }
    }
}
