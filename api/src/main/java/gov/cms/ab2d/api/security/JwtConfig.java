package gov.cms.ab2d.api.security;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

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
}
