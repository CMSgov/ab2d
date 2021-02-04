package gov.cms.ab2d.api.controller;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.json.JsonMapper;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.ApiResponseEvent;
import io.swagger.annotations.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;

import static gov.cms.ab2d.common.util.Constants.API_PREFIX;
import static gov.cms.ab2d.common.util.Constants.FHIR_PREFIX;
import static gov.cms.ab2d.common.util.Constants.NDJSON_FIRE_CONTENT_TYPE;
import static gov.cms.ab2d.common.util.Constants.USERNAME;
import static gov.cms.ab2d.common.util.Constants.REQUEST_ID;

/**
 * The sole REST controller for AB2D's implementation of the FHIR Bulk Data API capability statement.
 */
@AllArgsConstructor
@Slf4j
@SuppressWarnings("PMD.TooManyStaticImports")
@Api(value = "FHIR capability statement", description = "Provides the standard required capability statement", tags = {"Capabilities"})
@RestController
@RequestMapping(path = API_PREFIX + FHIR_PREFIX, produces = {"application/json", NDJSON_FIRE_CONTENT_TYPE})
public class CapabilityAPI {

    private final LogManager eventLogger;

    @ApiOperation(value = "A request for the FHIR capability statement", response = String.class,
            produces = "application/json",
            authorizations = {
                    @Authorization(value = "Authorization", scopes = {
                            @AuthorizationScope(description = "Returns the FHIR capability statement", scope = "Authorization") })
            })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Returns the FHIR capability statement", response =
                    String.class)}
    )
    @ResponseStatus(value = HttpStatus.OK)
    @GetMapping(value = "/metadata")
    public ResponseEntity<String> capabilityStatement(HttpServletRequest request) {
        String json = getCapabilityStatement();

        eventLogger.log(new ApiResponseEvent(MDC.get(USERNAME), null, HttpStatus.OK,
                "FHIR Capability Statement",
                "FHIR Capability Statement Returned", (String) request.getAttribute(REQUEST_ID)));

        return new ResponseEntity<>(json, null, HttpStatus.OK);
    }

    private String getCapabilityStatement() {
        try {
            CapabilityStatement capabilityStatement = new CapabilityStatement();
            return new JsonMapper()
                    .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                    .writeValueAsString(capabilityStatement);
        } catch (IOException re) {
            throw new RuntimeException("could not serialize capability statement, should never happen", re);
        }
    }
}
