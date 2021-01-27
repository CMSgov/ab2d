package gov.cms.ab2d.api.security;

import com.okta.jwt.AccessTokenVerifier;
import com.okta.jwt.JwtVerifiers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;

import java.time.Duration;

import static gov.cms.ab2d.api.util.Constants.ADMIN_ROLE;
import static gov.cms.ab2d.common.util.Constants.*;

@Configuration
@EnableWebSecurity
@SuppressWarnings("PMD.TooManyStaticImports")
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final FilterChainExceptionHandler filterChainExceptionHandler;
    private final JwtTokenAuthenticationFilter jwtTokenAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;
    private final String oktaJwtIssuer;
    private final String oktaJwtAudience;
    private final int oktaConnectionTimeout;

    public SecurityConfig(FilterChainExceptionHandler filterChainExceptionHandler, JwtTokenAuthenticationFilter jwtTokenAuthenticationFilter,
                          CustomUserDetailsService customUserDetailsService, @Value("${api.okta-jwt-issuer}") String oktaJwtIssuer,
                          @Value("${api.okta-jwt-audience}") String oktaJwtAudience,
                          @Value("${api.okta-connection-timeout}") int oktaConnectionTimeout) {
        this.filterChainExceptionHandler = filterChainExceptionHandler;
        this.jwtTokenAuthenticationFilter = jwtTokenAuthenticationFilter;
        this.customUserDetailsService = customUserDetailsService;
        this.oktaJwtIssuer = oktaJwtIssuer;
        this.oktaJwtAudience = oktaJwtAudience;
        this.oktaConnectionTimeout = oktaConnectionTimeout;
    }

    @Override
    public void configure(WebSecurity web) {
        web.ignoring().antMatchers("/swagger-ui/**", "/configuration/**",
                "/swagger-resources/**", "/v3/api-docs", "/webjars/**", HEALTH_ENDPOINT, STATUS_ENDPOINT);
    }

    @Override
    protected void configure(HttpSecurity security) throws Exception {
        security
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            // Setup filter exception handling
            .addFilterBefore(filterChainExceptionHandler, LogoutFilter.class)
            // Add a filter to validate the tokens with every request.
            .addFilterAfter(jwtTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeRequests()
            .antMatchers(API_PREFIX + ADMIN_PREFIX + "/**").hasAuthority(ADMIN_ROLE)
            .antMatchers(API_PREFIX + FHIR_PREFIX + "/**").hasAnyAuthority(SPONSOR_ROLE)
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
                .setIssuer(oktaJwtIssuer)
                .setAudience(oktaJwtAudience)
                .setConnectionTimeout(Duration.ofSeconds(oktaConnectionTimeout))
                .build();

        return jwtVerifier;
    }
}
