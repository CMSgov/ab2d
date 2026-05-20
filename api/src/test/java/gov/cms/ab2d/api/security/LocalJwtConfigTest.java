package gov.cms.ab2d.api.security;

import com.okta.jwt.AccessTokenVerifier;
import com.okta.jwt.Jwt;
import com.okta.jwt.JwtVerificationException;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalJwtConfigTest {

    @Test
    void localProfilePermitsAnyToken() throws JwtVerificationException {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.getEnvironment().setActiveProfiles("local");
            ctx.register(LocalJwtConfig.class);
            ctx.refresh();

            AccessTokenVerifier verifier = ctx.getBean(AccessTokenVerifier.class);
            Jwt jwt = verifier.decode("test");

            assertEquals("test", jwt.getTokenValue());
            assertEquals("EileenCFrierson@example.com", jwt.getClaims().get("sub"));
        }
    }

    @Test
    void localProfileRejectsBlankToken() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.getEnvironment().setActiveProfiles("local");
            ctx.register(LocalJwtConfig.class);
            ctx.refresh();

            AccessTokenVerifier verifier = ctx.getBean(AccessTokenVerifier.class);
            assertThrows(JwtVerificationException.class, () -> verifier.decode(""));
            assertThrows(JwtVerificationException.class, () -> verifier.decode("   "));
        }
    }

    @Test
    void localConfigSkippedWhenProfileNotActive() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.register(LocalJwtConfig.class);
            ctx.refresh();

            assertTrue(ctx.getBeansOfType(AccessTokenVerifier.class).isEmpty());
        }
    }

    /**
     * Ensure that when the local profile isn't set, the access verifier does not use
     * the stubbed bean.
     */
    @Test
    void nonLocalProfileUsesRealVerifierNotStub() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("test-props", Map.of(
                            "api.okta-jwt-issuer", "https://example.okta.com/oauth2/default",
                            "api.okta-jwt-audience", "api://default",
                            "api.okta-connection-timeout", "1")));
            ctx.register(JwtConfig.class, LocalJwtConfig.class);
            ctx.refresh();

            AccessTokenVerifier verifier = ctx.getBean(AccessTokenVerifier.class);
            assertThrows(JwtVerificationException.class, () -> verifier.decode("deny-this"));
        }
    }
}
