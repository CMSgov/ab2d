package gov.cms.ab2d.api.security;

import com.okta.jwt.AccessTokenVerifier;
import com.okta.jwt.JwtVerifiers;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Getter
@Configuration
public class JwtConfig {

    private final String header;
    private final String prefix;

    public JwtConfig(@Value("${security.jwt.header:Authorization}") String header,
                     @Value("${security.jwt.prefix:Bearer }")  String prefix) {
        this.header = header;
        this.prefix = prefix;
    }

    @Bean
    public AccessTokenVerifier accessTokenVerifier(@Value("${api.okta-jwt-issuer}") String oktaJwtIssuer,
                                                   @Value("${api.okta-jwt-audience}") String oktaJwtAudience,
                                                   @Value("${api.okta-connection-timeout}") int oktaConnectionTimeout) {
        return JwtVerifiers.accessTokenVerifierBuilder()
                .setIssuer(oktaJwtIssuer)
                .setAudience(oktaJwtAudience)
                .setConnectionTimeout(Duration.ofSeconds(oktaConnectionTimeout))
                .build();
    }
}
