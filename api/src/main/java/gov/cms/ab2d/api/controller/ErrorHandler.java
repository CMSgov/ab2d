package gov.cms.ab2d.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.api.controller.common.ApiCommon;
import gov.cms.ab2d.api.security.BadJWTTokenException;
import gov.cms.ab2d.api.security.ClientNotEnabledException;
import gov.cms.ab2d.api.security.InvalidAuthHeaderException;
import gov.cms.ab2d.api.security.MissingTokenException;
import gov.cms.ab2d.common.service.InvalidClientInputException;
import gov.cms.ab2d.common.service.InvalidContractException;
import gov.cms.ab2d.api.util.UtilMethods;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import gov.cms.ab2d.eventclient.events.ApiResponseEvent;
import gov.cms.ab2d.eventclient.events.ErrorEvent;
import gov.cms.ab2d.fhir.FhirVersion;
import gov.cms.ab2d.job.service.InvalidJobAccessException;
import gov.cms.ab2d.job.service.InvalidJobStateTransition;
import gov.cms.ab2d.job.service.JobOutputMissingException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
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


import static gov.cms.ab2d.common.util.Constants.API_PREFIX_V1;
import static gov.cms.ab2d.common.util.Constants.API_PREFIX_V2;
import static gov.cms.ab2d.common.util.Constants.FHIR_PREFIX;
import static gov.cms.ab2d.common.util.Constants.ORGANIZATION;
import static gov.cms.ab2d.common.util.Constants.REQUEST_ID;
import static gov.cms.ab2d.eventclient.events.SlackEvents.API_INVALID_CONTRACT;
import static org.springframework.http.HttpHeaders.CONTENT_LOCATION;
import static org.springframework.http.HttpHeaders.RETRY_AFTER;

/**
 * Don't change exception classes without updating alerts in Splunk. Splunk alerts rely on the classname to filter
 * for these exceptions.
 *
 * todo in the future use an enum instead to make it more consistent
 */
@ControllerAdvice
@Slf4j
public class ErrorHandler extends ResponseEntityExceptionHandler {

    private final SQSEventClient eventLogger;
    private final int retryAfterDelay;
    private final ApiCommon apiCommon;

    private static final String API_ERROR = "API Error";

    private static final HashMap<Class, HttpStatus> RESPONSE_MAP;
        static {
            RESPONSE_MAP = new HashMap<>();
            RESPONSE_MAP.put(InvalidClientInputException.class, HttpStatus.BAD_REQUEST);
            RESPONSE_MAP.put(InvalidJobStateTransition.class, HttpStatus.BAD_REQUEST);
            RESPONSE_MAP.put(MissingTokenException.class, HttpStatus.UNAUTHORIZED);
            RESPONSE_MAP.put(InvalidAuthHeaderException.class, HttpStatus.UNAUTHORIZED);
            RESPONSE_MAP.put(BadJWTTokenException.class, HttpStatus.FORBIDDEN);
            RESPONSE_MAP.put(UsernameNotFoundException.class, HttpStatus.FORBIDDEN);
            RESPONSE_MAP.put(ClientNotEnabledException.class, HttpStatus.FORBIDDEN);
            RESPONSE_MAP.put(JwtVerificationException.class, HttpStatus.FORBIDDEN);
            RESPONSE_MAP.put(InvalidContractException.class, HttpStatus.FORBIDDEN);
            RESPONSE_MAP.put(InvalidJobAccessException.class, HttpStatus.FORBIDDEN);
            RESPONSE_MAP.put(gov.cms.ab2d.common.service.ResourceNotFoundException.class, HttpStatus.NOT_FOUND);
            RESPONSE_MAP.put(TooManyRequestsException.class, HttpStatus.TOO_MANY_REQUESTS);
            RESPONSE_MAP.put(InMaintenanceModeException.class, HttpStatus.SERVICE_UNAVAILABLE);
            RESPONSE_MAP.put(URISyntaxException.class, HttpStatus.SERVICE_UNAVAILABLE);
            RESPONSE_MAP.put(JobOutputMissingException.class, HttpStatus.INTERNAL_SERVER_ERROR);
            RESPONSE_MAP.put(JobProcessingException.class, HttpStatus.INTERNAL_SERVER_ERROR);
            RESPONSE_MAP.put(DataIntegrityViolationException.class, HttpStatus.INTERNAL_SERVER_ERROR);
        }

    public ErrorHandler(SQSEventClient eventLogger, @Value("${api.retry-after.delay}") int retryAfterDelay, ApiCommon apiCommon) {
        this.eventLogger = eventLogger;
        this.retryAfterDelay = retryAfterDelay;
        this.apiCommon = apiCommon;
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
            JobProcessingException.class
    })
    public ResponseEntity<JsonNode> assertionException(final Exception e, HttpServletRequest request) throws IOException {
        return generateFHIRError(e, request);
    }

    @ExceptionHandler({JobOutputMissingException.class})
    public ResponseEntity<JsonNode> handleJobOutputMissing(Exception e, HttpServletRequest request) throws IOException {
        eventLogger.sendLogs(new ErrorEvent(MDC.get(ORGANIZATION), UtilMethods.parseJobId(request.getRequestURI()),
                ErrorEvent.ErrorType.FILE_ALREADY_DELETED, getRootCause(e)));
        return generateFHIRError(e, request);
    }

    @ExceptionHandler({InvalidContractException.class})
    public ResponseEntity<Void> handleInvalidContractErrors(Exception ex, HttpServletRequest request) {
        HttpStatus status = getErrorResponse(ex.getClass());
        String description = API_INVALID_CONTRACT + " " + getRootCause(ex);

        eventLogger.sendLogs(new ErrorEvent(MDC.get(ORGANIZATION), null,
                ErrorEvent.ErrorType.UNAUTHORIZED_CONTRACT, description));

        ApiResponseEvent responseEvent = new ApiResponseEvent(MDC.get(ORGANIZATION), null, status,
                API_ERROR, description, (String) request.getAttribute(REQUEST_ID));
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
                API_ERROR, description, (String) request.getAttribute(REQUEST_ID));
        eventLogger.sendLogs(responseEvent);

        return new ResponseEntity<>(null, null, status);
    }

    @ExceptionHandler({InMaintenanceModeException.class})
    public ResponseEntity<Void> handleMaintenanceMode(Exception ex, HttpServletRequest request) {
        HttpStatus status = getErrorResponse(ex.getClass());

        // Log so that Splunk can pick this up and alert
        log.warn("Maintenance mode blocked API request " + request.getAttribute(REQUEST_ID));

        // Then log to other destinations
        eventLogger.sendLogs(new ApiResponseEvent(MDC.get(ORGANIZATION), null, status,
                API_ERROR, ex.getClass().getSimpleName(), (String) request.getAttribute(REQUEST_ID)));
        eventLogger.trace("API_MAINT_BLOCKED Maintenance mode blocked API request " + request.getAttribute(REQUEST_ID), Ab2dEnvironment.PROD_LIST);

        return new ResponseEntity<>(null, null, status);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<JsonNode> handleTooManyRequestsExceptions(final TooManyRequestsException e, HttpServletRequest request) throws IOException {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(RETRY_AFTER, Integer.toString(retryAfterDelay));
        if (e.getJobIds() != null) {
            generateContentLocation(e, request, httpHeaders);
        }
        eventLogger.sendLogs(new ErrorEvent(MDC.get(ORGANIZATION), UtilMethods.parseJobId(request.getRequestURI()),
                ErrorEvent.ErrorType.TOO_MANY_STATUS_REQUESTS, "Too many requests performed in too short a time"));
        return generateFHIRError(e, httpHeaders, request);
    }

    private void generateContentLocation(TooManyRequestsException e, HttpServletRequest request, HttpHeaders httpHeaders) {
        String contentLocationHeader = e.getJobIds().stream().map(jobId -> {
            StringBuilder uri = new StringBuilder();
            if (request.getRequestURI().contains("/api/v2/")) {
                uri.append(API_PREFIX_V2).append(FHIR_PREFIX).append("/Job/").append(jobId).append("/$status");

            } else {
                uri.append(API_PREFIX_V1).append(FHIR_PREFIX).append("/Job/").append(jobId).append("/$status");
            }

            return apiCommon.getUrl(uri.toString(), request);
        }).collect(Collectors.joining(", "));

        httpHeaders.add(CONTENT_LOCATION, contentLocationHeader);
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

        eventLogger.sendLogs(new ApiResponseEvent(MDC.get(ORGANIZATION), null,
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