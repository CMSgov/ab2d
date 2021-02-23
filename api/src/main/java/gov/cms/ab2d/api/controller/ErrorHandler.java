package gov.cms.ab2d.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.api.security.BadJWTTokenException;
import gov.cms.ab2d.api.security.InvalidAuthHeaderException;
import gov.cms.ab2d.api.security.MissingTokenException;
import gov.cms.ab2d.api.security.UserNotEnabledException;
import gov.cms.ab2d.common.service.InvalidClientInputException;
import gov.cms.ab2d.common.service.InvalidJobStateTransition;
import gov.cms.ab2d.common.service.InvalidPropertiesException;
import gov.cms.ab2d.common.service.InvalidContractException;
import gov.cms.ab2d.common.service.InvalidJobAccessException;
import gov.cms.ab2d.common.service.ResourceNotFoundException;
import gov.cms.ab2d.common.service.JobOutputMissingException;
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
import static gov.cms.ab2d.common.util.Constants.USERNAME;

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
            put(UserNotEnabledException.class, HttpStatus.FORBIDDEN);
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
        eventLogger.log(new ErrorEvent(MDC.get(USERNAME), UtilMethods.parseJobId(request.getRequestURI()),
                ErrorEvent.ErrorType.FILE_ALREADY_DELETED, getRootCause(e)));
        return generateFHIRError(e, request);
    }

    @ExceptionHandler({InvalidContractException.class})
    public ResponseEntity<Void> handleInvalidContractErrors(Exception e, HttpServletRequest request) {
        eventLogger.log(new ErrorEvent(MDC.get(USERNAME), null,
                ErrorEvent.ErrorType.UNAUTHORIZED_CONTRACT, getRootCause(e)));
        return generateError(e, request);
    }

    @ExceptionHandler({MissingTokenException.class,
            InvalidAuthHeaderException.class,
            BadJWTTokenException.class,
            UsernameNotFoundException.class,
            UserNotEnabledException.class,
            JwtVerificationException.class,
            InvalidJobAccessException.class,
            InMaintenanceModeException.class
    })
    public ResponseEntity<Void> handleErrors(Exception ex, HttpServletRequest request) {
        return generateError(ex, request);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<JsonNode> handleTooManyRequestsExceptions(final TooManyRequestsException e, HttpServletRequest request) throws IOException {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Retry-After", Integer.toString(retryAfterDelay));
        eventLogger.log(new ErrorEvent(MDC.get(USERNAME), UtilMethods.parseJobId(request.getRequestURI()),
                ErrorEvent.ErrorType.TOO_MANY_STATUS_REQUESTS, "Too many requests performed in too short a time"));
        return generateFHIRError(e, httpHeaders, request);
    }

    private ResponseEntity<Void> generateError(Exception ex, HttpServletRequest request) {
        HttpStatus status = getErrorResponse(ex.getClass());
        eventLogger.log(new ApiResponseEvent(MDC.get(USERNAME), null, status,
                "API Error", getRootCause(ex), (String) request.getAttribute(REQUEST_ID)));
        return new ResponseEntity<>(null, null, status);
    }

    private ResponseEntity<JsonNode> generateFHIRError(Exception e, HttpServletRequest request) throws IOException {
        return generateFHIRError(e, null, request);
    }

    private ResponseEntity<JsonNode> generateFHIRError(Exception e, HttpHeaders httpHeaders, HttpServletRequest request) throws IOException {
        String msg = getRootCause(e);
        HttpStatus httpStatus = getErrorResponse(e.getClass());

        FhirVersion version = FhirVersion.fromUrl(request.getRequestURI());
        IBaseResource operationOutcome = version.getErrorOutcome(msg);
        String encoded = version.outcomePrettyToJSON(operationOutcome);
        eventLogger.log(new ApiResponseEvent(MDC.get(USERNAME), null,
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