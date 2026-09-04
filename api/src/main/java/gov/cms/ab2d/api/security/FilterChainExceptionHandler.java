package gov.cms.ab2d.api.security;

import gov.cms.ab2d.api.util.ApiRequestMetrics;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            // Record the type before the exception is turned into a response so the metrics filter can
            // tag ab2d.api.error.count with it
            request.setAttribute(ApiRequestMetrics.ERROR_TYPE_ATTRIBUTE, e.getClass().getSimpleName());
            handlerExceptionResolver.resolveException(request, response, null, e);
        }
    }
}
