package gov.cms.ab2d.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.io.IOException;

import static gov.cms.ab2d.api.util.FHIRUtil.getErrorOutcome;
import static gov.cms.ab2d.api.util.FHIRUtil.outcomeToJSON;

@ControllerAdvice
class ErrorHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<JsonNode> assertionException(final Exception e) throws IOException {
        String msg = ExceptionUtils.getRootCauseMessage(e);
        OperationOutcome operationOutcome = getErrorOutcome(msg);
        String encoded = outcomeToJSON(operationOutcome);
        return new ResponseEntity<>(new ObjectMapper().readTree(encoded), HttpStatus.BAD_REQUEST);
    }
}