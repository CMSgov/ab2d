package gov.cms.ab2d.api.controller.v3;

import ca.uhn.fhir.parser.IParser;
import gov.cms.ab2d.api.controller.common.ApiCommon;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.eventclient.events.ApiResponseEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static gov.cms.ab2d.api.controller.common.ApiText.*;
import static gov.cms.ab2d.common.util.Constants.*;
import static gov.cms.ab2d.fhir.FhirVersion.R4;

/**
 * The sole REST controller for AB2D's implementation of the FHIR Bulk Data API capability statement.
 */
@AllArgsConstructor
@Slf4j
@Tag(name = "Capabilities", description = CAP_API)
@RestController
@ConditionalOnExpression("${v3.controller.enabled:true}")
@RequestMapping(path = API_PREFIX_V3 + FHIR_PREFIX, produces = APPLICATION_JSON)
public class CapabilityAPIV3 {

    private final SQSEventClient eventLogger;
    private final ApiCommon common;

    @Operation(summary = CAP_REQ)
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = CAP_DESC + " http://hl7.org/fhir/capabilitystatement.html")})
    @ResponseStatus(value = HttpStatus.OK)
    @GetMapping(value = "/metadata")
    public ResponseEntity<String> capabilityStatement(HttpServletRequest request) {

        IParser parser = R4.getJsonParser();

        eventLogger.sendLogs(new ApiResponseEvent(MDC.get(ORGANIZATION), null, HttpStatus.OK,
                CAP_STMT, CAP_RET, (String) request.getAttribute(REQUEST_ID)));

        String server = common.getCurrentUrl(request).replace("/metadata", "");
        CapabilityStatement capabilityStatement = CapabilityStatementR4V3.populateCS(server);
        return new ResponseEntity<>(parser.encodeResourceToString(capabilityStatement), null, HttpStatus.OK);
    }
}
