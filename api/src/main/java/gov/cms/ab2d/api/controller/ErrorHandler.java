package gov.cms.ab2d.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.persistence.EntityNotFoundException;
import java.io.IOException;

import static gov.cms.ab2d.api.util.FHIRUtil.getErrorOutcome;
import static gov.cms.ab2d.api.util.FHIRUtil.outcomeToJSON;

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
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<JsonNode> assertionException(final Exception e) throws IOException {
        return generateFHIRError(e, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<JsonNode> handleNotFoundExceptions(final EntityNotFoundException e) throws IOException {
        return generateFHIRError(e, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<JsonNode> handleTooManyRequestsExceptions(final TooManyRequestsException e) throws IOException {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Retry-After", Integer.toString(retryAfterDelay));
        return generateFHIRError(e, HttpStatus.TOO_MANY_REQUESTS, httpHeaders);
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