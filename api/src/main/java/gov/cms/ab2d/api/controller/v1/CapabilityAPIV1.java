package gov.cms.ab2d.api.controller.v1;

import ca.uhn.fhir.parser.IParser;
import gov.cms.ab2d.api.controller.common.ApiCommon;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.ApiResponseEvent;
import io.swagger.annotations.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import static gov.cms.ab2d.api.controller.common.ApiText.CAP_STMT;
import static gov.cms.ab2d.api.controller.common.ApiText.CAP_API;
import static gov.cms.ab2d.api.controller.common.ApiText.JSON;
import static gov.cms.ab2d.api.controller.common.ApiText.CAP_REQ;
import static gov.cms.ab2d.api.controller.common.ApiText.AUTH;
import static gov.cms.ab2d.api.controller.common.ApiText.CAP_DESC;
import static gov.cms.ab2d.api.controller.common.ApiText.CAP_RET;

import static gov.cms.ab2d.common.util.Constants.API_PREFIX_V1;
import static gov.cms.ab2d.common.util.Constants.FHIR_PREFIX;
import static gov.cms.ab2d.common.util.Constants.NDJSON_FIRE_CONTENT_TYPE;
import static gov.cms.ab2d.common.util.Constants.USERNAME;
import static gov.cms.ab2d.common.util.Constants.REQUEST_ID;

import static gov.cms.ab2d.fhir.FhirVersion.STU3;

/**
 * The sole REST controller for AB2D's implementation of the FHIR Bulk Data API capability statement.
 */
@AllArgsConstructor
@Slf4j
@SuppressWarnings("PMD.TooManyStaticImports")
@Api(value = CAP_STMT, description = CAP_API, tags = {"Capabilities"})
@RestController
@RequestMapping(path = API_PREFIX_V1 + FHIR_PREFIX, produces = {JSON, NDJSON_FIRE_CONTENT_TYPE})
public class CapabilityAPIV1 {

    private final LogManager eventLogger;
    private final ApiCommon common;

    @ApiOperation(value = CAP_REQ, response = String.class, produces = JSON, authorizations = {
            @Authorization(value = AUTH, scopes = { @AuthorizationScope(description = CAP_DESC, scope = AUTH) })
    })
    @ApiResponses(value = { @ApiResponse(code = 200, message = CAP_DESC, response = String.class)})
    @ResponseStatus(value = HttpStatus.OK)
    @GetMapping(value = "/metadata")
    public ResponseEntity<String> capabilityStatement(HttpServletRequest request) {

        IParser parser = STU3.getJsonParser();

        eventLogger.log(new ApiResponseEvent(MDC.get(USERNAME), null, HttpStatus.OK,
                CAP_STMT, CAP_RET, (String) request.getAttribute(REQUEST_ID)));

        String server = common.getCurrentUrl(request).replace("/metadata", "");
        CapabilityStatement capabilityStatement = CapabilityStatementSTU3.populateCS(server);
        return new ResponseEntity<>(parser.encodeResourceToString(capabilityStatement), null, HttpStatus.OK);
    }
}
