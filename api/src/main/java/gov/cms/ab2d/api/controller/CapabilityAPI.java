package gov.cms.ab2d.api.controller;

import com.google.gson.Gson;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static gov.cms.ab2d.common.util.Constants.*;

/**
 * The sole REST controller for AB2D's implementation of the FHIR Bulk Data API capability statement.
 */
@Slf4j
@Api(value = "FHIR capability statement", description = "Provides the standard required capability statement", tags = {"Capabilities"})
@RestController
@RequestMapping(path = API_PREFIX + FHIR_PREFIX, produces = {"application/json", NDJSON_FIRE_CONTENT_TYPE})
public class CapabilityAPI {

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
    public ResponseEntity<String> capabilityStatement() {
        CapabilityStatement capabilityStatement = new CapabilityStatement();
        String json = new Gson().toJson(capabilityStatement);
        return new ResponseEntity<>(json, null, HttpStatus.OK);
    }
}
