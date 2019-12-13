package gov.cms.ab2d.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.api.security.BadJWTTokenException;
import gov.cms.ab2d.api.security.InvalidAuthHeaderException;
import gov.cms.ab2d.api.security.MissingTokenException;
import gov.cms.ab2d.api.security.UserNotEnabledException;
import gov.cms.ab2d.common.service.InvalidContractException;
import gov.cms.ab2d.common.service.InvalidJobStateTransition;
import gov.cms.ab2d.common.service.ResourceNotFoundException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.io.IOException;

import static gov.cms.ab2d.common.util.FHIRUtil.getErrorOutcome;
import static gov.cms.ab2d.common.util.FHIRUtil.outcomeToJSON;

@ControllerAdvice
class ErrorHandler extends ResponseEntityExceptionHandler {

    @Value("${api.retry-after.delay}")
    private int retryAfterDelay;

    // All errors that are not the fault of the client
    @ExceptionHandler(Exception.class)
    public ResponseEntity<JsonNode> serverException(final Exception e) throws IOException {
        return generateFHIRError(e, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Most errors that are the fault of the client
    @ExceptionHandler({InvalidUserInputException.class, InvalidJobStateTransition.class})
    public ResponseEntity<JsonNode> assertionException(final Exception e) throws IOException {
        return generateFHIRError(e, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({MissingTokenException.class, InvalidAuthHeaderException.class})
    public ResponseEntity<Void> handleUnauthorizedAccessExceptions() {
        return generateError(HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({BadJWTTokenException.class, UsernameNotFoundException.class, UserNotEnabledException.class,
            JwtVerificationException.class, InvalidContractException.class})
    public ResponseEntity<Void> handleForbiddenAccessExceptions() {
        return generateError(HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<JsonNode> handleNotFoundExceptions(final ResourceNotFoundException e) throws IOException {
        return generateFHIRError(e, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<JsonNode> handleTooManyRequestsExceptions(final TooManyRequestsException e) throws IOException {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Retry-After", Integer.toString(retryAfterDelay));
        return generateFHIRError(e, HttpStatus.TOO_MANY_REQUESTS, httpHeaders);
    }

    private ResponseEntity<Void> generateError(HttpStatus httpStatus) {
        return new ResponseEntity<>(null, null, httpStatus);
    }

    private ResponseEntity<JsonNode> generateFHIRError(Exception e, HttpStatus httpStatus) throws IOException {
        return generateFHIRError(e, httpStatus, null);
    }

    private ResponseEntity<JsonNode> generateFHIRError(Exception e, HttpStatus httpStatus, HttpHeaders httpHeaders) throws IOException {
        String msg = ExceptionUtils.getRootCauseMessage(e);
        OperationOutcome operationOutcome = getErrorOutcome(msg);
        String encoded = outcomeToJSON(operationOutcome);
        return new ResponseEntity<>(new ObjectMapper().readTree(encoded), httpHeaders, httpStatus);
    }
}