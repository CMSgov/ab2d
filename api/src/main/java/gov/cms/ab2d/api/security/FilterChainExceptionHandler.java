package gov.cms.ab2d.api.security;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This class is used to handle exceptions that come from filters, such as the JwtTokenAuthenticationFilter, since by
 * default exceptions coming from a filter will not go to the ErrorHandler class.
 */
@AllArgsConstructor
@Component
@Slf4j
public class FilterChainExceptionHandler extends OncePerRequestFilter {

    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
        try {
            log.warn("AB2D-6303 doFilterInternal before doFilter. filterChain: {}", filterChain);
            filterChain.doFilter(request, response);
            log.warn("AB2D-6303 doFilterInternal after doFilter");
        } catch (Exception e) {
            log.warn("AB2D-6303 doFilterInternal in catch. Exception: {}", e);
            handlerExceptionResolver.resolveException(request, response, null, e);
        }
    }
}
