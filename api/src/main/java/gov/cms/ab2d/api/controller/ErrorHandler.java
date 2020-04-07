package gov.cms.ab2d.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.api.security.BadJWTTokenException;
import gov.cms.ab2d.api.security.InvalidAuthHeaderException;
import gov.cms.ab2d.api.security.MissingTokenException;
import gov.cms.ab2d.api.security.UserNotEnabledException;
import gov.cms.ab2d.common.service.InvalidUserInputException;
import gov.cms.ab2d.common.service.InvalidJobStateTransition;
import gov.cms.ab2d.common.service.InvalidPropertiesException;
import gov.cms.ab2d.common.service.InvalidContractException;
import gov.cms.ab2d.common.service.InvalidJobAccessException;
import gov.cms.ab2d.common.service.ResourceNotFoundException;
import gov.cms.ab2d.common.service.ContractNotFoundException;
import gov.cms.ab2d.common.service.JobOutputMissingException;
import gov.cms.ab2d.eventlogger.EventLogger;
import gov.cms.ab2d.eventlogger.events.ApiResponseEvent;
import gov.cms.ab2d.eventlogger.events.ErrorEvent;
import gov.cms.ab2d.eventlogger.utils.UtilMethods;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import static gov.cms.ab2d.common.util.FHIRUtil.getErrorOutcome;
import static gov.cms.ab2d.common.util.FHIRUtil.outcomeToJSON;

@ControllerAdvice
@Slf4j
public class ErrorHandler extends ResponseEntityExceptionHandler {

    @Value("${api.retry-after.delay}")
    private int retryAfterDelay;

    @Autowired
    private EventLogger eventLogger;

    private final static Map<Class, HttpStatus> responseMap = new HashMap<>() {
        {
            put(InvalidUserInputException.class, HttpStatus.BAD_REQUEST);
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
            put(ContractNotFoundException.class, HttpStatus.NOT_FOUND);
            put(JobOutputMissingException.class, HttpStatus.NOT_FOUND);
        }
    };

    private static HttpStatus getErrorResponse(Class clazz) {
        HttpStatus res = responseMap.get(clazz);
        if (res == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return res;
    }

    // All errors that are not the fault of the client
    @ExceptionHandler(Exception.class)
    public ResponseEntity<JsonNode> serverException(final Exception e, HttpServletRequest request) throws IOException {
        log.error("Encountered exception: ", e);
        return generateFHIRError(e, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler({InvalidUserInputException.class,
            InvalidJobStateTransition.class,
            InvalidPropertiesException.class,
            ResourceNotFoundException.class
    })
    public ResponseEntity<JsonNode> assertionException(final Exception e, HttpServletRequest request) throws IOException {
        return generateFHIRError(e, responseMap.get(e.getClass()), request);
    }

    @ExceptionHandler({ContractNotFoundException.class})
    public ResponseEntity<Void> contractNotFoundException(final Exception e, HttpServletRequest request) {
        eventLogger.log(new ErrorEvent(MDC.get(USERNAME), null,
                ErrorEvent.ErrorType.CONTRACT_NOT_FOUND, getRootCause(e)));
        return generateError(e, request);
    }

    @ExceptionHandler({JobOutputMissingException.class})
    public ResponseEntity<Void> handleJobOutputMissing(Exception e, HttpServletRequest request) {
        eventLogger.log(new ErrorEvent(MDC.get(USERNAME), null,
                ErrorEvent.ErrorType.FILE_ALREADY_DELETED, getRootCause(e)));
        return generateError(e, request);
    }

    @ExceptionHandler({InvalidContractException.class})
    public ResponseEntity<Void> handleInvalidContractErrors(Exception e, HttpServletRequest request) {
        eventLogger.log(new ErrorEvent(MDC.get(USERNAME), null,
                ErrorEvent.ErrorType.UNAUTHORIZED_CONTRACT, getRootCause(e)));
        return generateError(e, request);
    }

    @ExceptionHandler({MissingTokenException.class,
            InvalidAuthHeaderException.class,
            JobProcessingException.class,
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
        return generateFHIRError(e, responseMap.get(e.getClass()), httpHeaders, request);
    }

    private ResponseEntity<Void> generateError(Exception ex, HttpServletRequest request) {
        HttpStatus status = getErrorResponse(ex.getClass());
        eventLogger.log(new ApiResponseEvent(MDC.get(USERNAME), null, status,
                "API Error", getRootCause(ex), (String) request.getAttribute(REQUEST_ID)));
        return new ResponseEntity<>(null, null, status);
    }

    private ResponseEntity<JsonNode> generateFHIRError(Exception e, HttpStatus httpStatus, HttpServletRequest request) throws IOException {
        return generateFHIRError(e, httpStatus, null, request);
    }

    private ResponseEntity<JsonNode> generateFHIRError(Exception e, HttpStatus httpStatus, HttpHeaders httpHeaders, HttpServletRequest request) throws IOException {
        String msg = getRootCause(e);

        OperationOutcome operationOutcome = getErrorOutcome(msg);
        String encoded = outcomeToJSON(operationOutcome);
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