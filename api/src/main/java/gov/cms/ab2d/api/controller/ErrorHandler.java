package gov.cms.ab2d.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.api.security.BadJWTTokenException;
import gov.cms.ab2d.api.security.InvalidAuthHeaderException;
import gov.cms.ab2d.api.security.MissingTokenException;
import gov.cms.ab2d.api.security.ClientNotEnabledException;
import gov.cms.ab2d.common.service.InvalidClientInputException;
import gov.cms.ab2d.common.service.InvalidJobStateTransition;
import gov.cms.ab2d.common.service.InvalidPropertiesException;
import gov.cms.ab2d.common.service.InvalidContractException;
import gov.cms.ab2d.common.service.InvalidJobAccessException;
import gov.cms.ab2d.common.service.ResourceNotFoundException;
import gov.cms.ab2d.common.service.JobOutputMissingException;
import gov.cms.ab2d.eventlogger.Ab2dEnvironment;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.ApiResponseEvent;
import gov.cms.ab2d.eventlogger.events.ErrorEvent;
import gov.cms.ab2d.eventlogger.utils.UtilMethods;
import gov.cms.ab2d.fhir.FhirVersion;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static gov.cms.ab2d.common.util.Constants.REQUEST_ID;
import static gov.cms.ab2d.common.util.Constants.ORGANIZATION;
import static org.springframework.http.HttpHeaders.RETRY_AFTER;

/**
 * Don't change exception classes without updating alerts in Splunk. Splunk alerts rely on the classname to filter
 * for these exceptions.
 *
 * TODO: in the future use an enum instead to make it more consistent
 */
@ControllerAdvice
@Slf4j
public class ErrorHandler extends ResponseEntityExceptionHandler {

    private final LogManager eventLogger;
    private final int retryAfterDelay;

    private static final Map<Class, HttpStatus> RESPONSE_MAP = new HashMap<>() {
        {
            put(InvalidClientInputException.class, HttpStatus.BAD_REQUEST);
            put(InvalidJobStateTransition.class, HttpStatus.BAD_REQUEST);
            put(InvalidPropertiesException.class, HttpStatus.BAD_REQUEST);
            put(MissingTokenException.class, HttpStatus.UNAUTHORIZED);
            put(InvalidAuthHeaderException.class, HttpStatus.UNAUTHORIZED);
            put(BadJWTTokenException.class, HttpStatus.FORBIDDEN);
            put(UsernameNotFoundException.class, HttpStatus.FORBIDDEN);
            put(ClientNotEnabledException.class, HttpStatus.FORBIDDEN);
            put(JwtVerificationException.class, HttpStatus.FORBIDDEN);
            put(InvalidContractException.class, HttpStatus.FORBIDDEN);
            put(InvalidJobAccessException.class, HttpStatus.FORBIDDEN);
            put(ResourceNotFoundException.class, HttpStatus.NOT_FOUND);
            put(TooManyRequestsException.class, HttpStatus.TOO_MANY_REQUESTS);
            put(InMaintenanceModeException.class, HttpStatus.SERVICE_UNAVAILABLE);
            put(URISyntaxException.class, HttpStatus.SERVICE_UNAVAILABLE);
            put(JobOutputMissingException.class, HttpStatus.INTERNAL_SERVER_ERROR);
            put(JobProcessingException.class, HttpStatus.INTERNAL_SERVER_ERROR);
            put(DataIntegrityViolationException.class, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    };

    public ErrorHandler(LogManager eventLogger, @Value("${api.retry-after.delay}") int retryAfterDelay) {
        this.eventLogger = eventLogger;
        this.retryAfterDelay = retryAfterDelay;
    }

    private static HttpStatus getErrorResponse(Class clazz) {
        HttpStatus res = RESPONSE_MAP.get(clazz);
        if (res == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return res;
    }

    // All errors that are not the fault of the client
    @ExceptionHandler(Exception.class)
    public ResponseEntity<JsonNode> serverException(final Exception e, HttpServletRequest request) throws IOException {
        log.error("Encountered exception: ", e);
        return generateFHIRError(e, request);
    }

    @ExceptionHandler({InvalidClientInputException.class,
            InvalidJobStateTransition.class,
            InvalidPropertiesException.class,
            JobProcessingException.class,
            ResourceNotFoundException.class
    })
    public ResponseEntity<JsonNode> assertionException(final Exception e, HttpServletRequest request) throws IOException {
        return generateFHIRError(e, request);
    }

    @ExceptionHandler({JobOutputMissingException.class})
    public ResponseEntity<JsonNode> handleJobOutputMissing(Exception e, HttpServletRequest request) throws IOException {
        eventLogger.log(new ErrorEvent(MDC.get(ORGANIZATION), UtilMethods.parseJobId(request.getRequestURI()),
                ErrorEvent.ErrorType.FILE_ALREADY_DELETED, getRootCause(e)));
        return generateFHIRError(e, request);
    }

    @ExceptionHandler({InvalidContractException.class})
    public ResponseEntity<Void> handleInvalidContractErrors(Exception ex, HttpServletRequest request) {
        HttpStatus status = getErrorResponse(ex.getClass());
        String description = getRootCause(ex);

        eventLogger.log(new ErrorEvent(MDC.get(ORGANIZATION), null,
                ErrorEvent.ErrorType.UNAUTHORIZED_CONTRACT, description));

        ApiResponseEvent responseEvent = new ApiResponseEvent(MDC.get(ORGANIZATION), null, status,
                "API Error", description, (String) request.getAttribute(REQUEST_ID));
        eventLogger.logAndAlert(responseEvent, Ab2dEnvironment.PROD_LIST);

        return new ResponseEntity<>(null, null, status);
    }

    @ExceptionHandler({MissingTokenException.class,
            InvalidAuthHeaderException.class,
            BadJWTTokenException.class,
            UsernameNotFoundException.class,
            ClientNotEnabledException.class,
            JwtVerificationException.class,
            InvalidJobAccessException.class
    })
    public ResponseEntity<Void> handleAuthorizationErrors(Exception ex, HttpServletRequest request) {
        HttpStatus status = getErrorResponse(ex.getClass());

        String description = String.format("%s %s for request %s", ex.getClass().getSimpleName(), ex.getMessage(),
                request.getAttribute(REQUEST_ID));

        // Log so that Splunk can pick this up and alert
        log.warn(description);

        // Then log to other destinations
        ApiResponseEvent responseEvent = new ApiResponseEvent(MDC.get(ORGANIZATION), null, status,
                "API Error", description, (String) request.getAttribute(REQUEST_ID));
        eventLogger.log(responseEvent);

        return new ResponseEntity<>(null, null, status);
    }

    @ExceptionHandler({InMaintenanceModeException.class})
    public ResponseEntity<Void> handleMaintenanceMode(Exception ex, HttpServletRequest request) {
        HttpStatus status = getErrorResponse(ex.getClass());

        // Log so that Splunk can pick this up and alert
        log.warn("Maintenance mode blocked API request " + request.getAttribute(REQUEST_ID));

        // Then log to other destinations
        eventLogger.log(new ApiResponseEvent(MDC.get(ORGANIZATION), null, status,
                "API Error", ex.getClass().getSimpleName(), (String) request.getAttribute(REQUEST_ID)));
        eventLogger.trace("Maintenance mode blocked API request " + request.getAttribute(REQUEST_ID), Ab2dEnvironment.PROD_LIST);

        return new ResponseEntity<>(null, null, status);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<JsonNode> handleTooManyRequestsExceptions(final TooManyRequestsException e, HttpServletRequest request) throws IOException {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(RETRY_AFTER, Integer.toString(retryAfterDelay));
        eventLogger.log(new ErrorEvent(MDC.get(ORGANIZATION), UtilMethods.parseJobId(request.getRequestURI()),
                ErrorEvent.ErrorType.TOO_MANY_STATUS_REQUESTS, "Too many requests performed in too short a time"));
        return generateFHIRError(e, httpHeaders, request);
    }

    private ResponseEntity<JsonNode> generateFHIRError(Exception e, HttpServletRequest request) throws IOException {
        return generateFHIRError(e, null, request);
    }

    private ResponseEntity<JsonNode> generateFHIRError(Exception e, HttpHeaders httpHeaders, HttpServletRequest request) throws IOException {
        String msg = getRootCause(e);
        HttpStatus httpStatus = getErrorResponse(e.getClass());

        FhirVersion version = FhirVersion.fromAB2DUrl(request.getRequestURI());
        IBaseResource operationOutcome = version.getErrorOutcome(msg);
        String encoded = version.outcomePrettyToJSON(operationOutcome);

        // Log so that Splunk can pick this up and alert
        log.warn("{} {}", ExceptionUtils.getRootCause(e).getClass(), msg);

        eventLogger.log(new ApiResponseEvent(MDC.get(ORGANIZATION), null,
                ErrorHandler.getErrorResponse(e.getClass()),
                "FHIR Error", msg, (String) request.getAttribute(REQUEST_ID)));

        return new ResponseEntity<>(new ObjectMapper().readTree(encoded), httpHeaders, httpStatus);
    }

    private String getRootCause(Exception ex) {
        String msg;
        Throwable rootCause = ExceptionUtils.getRootCause(ex);
        // This exception did not come from us, so don't use the message since we didn't set the text, otherwise it could potentially reveal
        // internals, e.g. a message from a database error
        if (!rootCause.getClass().getName().matches("^gov\\.cms\\.ab2d.*")) {
            msg = "An internal error occurred";
        } else {
            msg = rootCause.getMessage();
            msg = msg.replaceFirst("^(.*Exception: )", "");
        }
        return msg;
    }
}