package gov.cms.ab2d.api.security;

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
@Component
@Slf4j
public class FilterChainExceptionHandler extends OncePerRequestFilter {

    private final HandlerExceptionResolver handlerExceptionResolver;

    public FilterChainExceptionHandler(HandlerExceptionResolver handlerExceptionResolver) {
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
        try {
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            handlerExceptionResolver.resolveException(request, response, null, e);
        }
    }
}
