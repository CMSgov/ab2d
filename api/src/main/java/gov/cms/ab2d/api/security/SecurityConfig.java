package gov.cms.ab2d.api.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;

import static gov.cms.ab2d.api.util.Constants.ADMIN_ROLE;
import static gov.cms.ab2d.common.util.Constants.*;

@Configuration
@EnableWebSecurity
@SuppressWarnings("PMD.TooManyStaticImports")
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final FilterChainExceptionHandler filterChainExceptionHandler;
    private final JwtTokenAuthenticationFilter jwtTokenAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;

    public SecurityConfig(FilterChainExceptionHandler filterChainExceptionHandler, JwtTokenAuthenticationFilter jwtTokenAuthenticationFilter,
                          CustomUserDetailsService customUserDetailsService) {
        this.filterChainExceptionHandler = filterChainExceptionHandler;
        this.jwtTokenAuthenticationFilter = jwtTokenAuthenticationFilter;
        this.customUserDetailsService = customUserDetailsService;
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
}
