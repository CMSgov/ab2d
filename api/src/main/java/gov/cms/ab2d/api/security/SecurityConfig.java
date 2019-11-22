package gov.cms.ab2d.api.security;

import com.okta.jwt.AccessTokenVerifier;
import com.okta.jwt.JwtVerifiers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;

import java.time.Duration;

import static gov.cms.ab2d.api.util.Constants.FHIR_PREFIX;
import static gov.cms.ab2d.api.util.Constants.ADMIN_PREFIX;
import static gov.cms.ab2d.api.util.Constants.ADMIN_ROLE;
import static gov.cms.ab2d.api.util.Constants.SPONSOR_ROLE;
import static gov.cms.ab2d.api.util.Constants.API_PREFIX;


@Configuration
@EnableWebSecurity
@SuppressWarnings("PMD.TooManyStaticImports")
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private FilterChainExceptionHandler filterChainExceptionHandler;

    @Autowired
    private JwtTokenAuthenticationFilter jwtTokenAuthenticationFilter;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Value("${api.okta-url}")
    private String oktaUrl;

    @Value("${api.okta-connection-timeout}")
    private int oktaConnectionTimeout;

    @Value("${api.okta-read-timeout}")
    private int oktaReadTimeout;

    @Override
    protected void configure(HttpSecurity security) throws Exception {
        security
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            // Setup filter exception handling
            .addFilterBefore(filterChainExceptionHandler, LogoutFilter.class)
            // Add a filter to validate the tokens with every request. Since class is instantiated outside of DI, need to pass autowired, and value objects to it directly
            .addFilterAfter(jwtTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeRequests()
            .antMatchers(API_PREFIX + ADMIN_PREFIX + "/**").hasAuthority(ADMIN_ROLE)
            .antMatchers(API_PREFIX + FHIR_PREFIX + "/**").hasAuthority(SPONSOR_ROLE)
            .anyRequest().authenticated();
    }

    @Override
    public void configure(AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception {
        authenticationManagerBuilder
                .userDetailsService(customUserDetailsService);
    }

    @Bean
    public AccessTokenVerifier accessTokenVerifier() {
        AccessTokenVerifier jwtVerifier = JwtVerifiers.accessTokenVerifierBuilder()
                .setIssuer(oktaUrl)
                .setAudience("api://default")
                .setConnectionTimeout(Duration.ofSeconds(oktaConnectionTimeout))
                .setReadTimeout(Duration.ofSeconds(oktaReadTimeout))
                .build();

        return jwtVerifier;
    }
}
