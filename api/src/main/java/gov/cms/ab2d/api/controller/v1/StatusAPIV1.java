package gov.cms.ab2d.api.controller.v1;

import gov.cms.ab2d.api.controller.JobCompletedResponse;
import gov.cms.ab2d.api.controller.common.StatusCommon;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;

import static gov.cms.ab2d.api.controller.common.ApiText.FILE_EXPIRES;
import static gov.cms.ab2d.api.controller.common.ApiText.JOB_CANCELLED_MSG;
import static gov.cms.ab2d.api.controller.common.ApiText.JOB_COMPLETE;
import static gov.cms.ab2d.api.controller.common.ApiText.JOB_ID;
import static gov.cms.ab2d.api.controller.common.ApiText.JOB_NOT_FOUND;
import static gov.cms.ab2d.api.controller.common.ApiText.APPLICATION_JSON;
import static gov.cms.ab2d.api.controller.common.ApiText.PROGRESS;
import static gov.cms.ab2d.api.controller.common.ApiText.STATUS_API;
import static gov.cms.ab2d.api.controller.common.ApiText.STATUS_DELAY;
import static gov.cms.ab2d.api.controller.common.ApiText.STATUS_DES;
import static gov.cms.ab2d.api.controller.common.ApiText.STILL_RUNNING;
import static gov.cms.ab2d.api.controller.common.ApiText.X_PROG;
import static gov.cms.ab2d.api.util.SwaggerConstants.BULK_CANCEL;
import static gov.cms.ab2d.common.util.Constants.API_PREFIX_V1;
import static gov.cms.ab2d.common.util.Constants.FHIR_JSON_CONTENT_TYPE;
import static gov.cms.ab2d.common.util.Constants.FHIR_PREFIX;
import static org.springframework.http.HttpHeaders.EXPIRES;
import static org.springframework.http.HttpHeaders.RETRY_AFTER;

/**
 * The sole REST controller for AB2D's implementation of the FHIR Bulk Data API Status (both GET & DELETE).
 */
@Slf4j
@Tag(name = "2. Status", description = STATUS_API)
@RestController
@RequestMapping(path = API_PREFIX_V1 + FHIR_PREFIX, produces = {APPLICATION_JSON, FHIR_JSON_CONTENT_TYPE})
@AllArgsConstructor
public class StatusAPIV1 {

    private final StatusCommon statusCommon;

    @Operation(summary = STATUS_DES)
    @Parameters(value = @Parameter(name = "jobUuid", description = JOB_ID, required = true, in = ParameterIn.PATH))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = STILL_RUNNING, headers = {
                @Header(name = X_PROG, description = PROGRESS, schema = @Schema(type = "string")),
                @Header(name = RETRY_AFTER, description = STATUS_DELAY, schema = @Schema(type = "integer"))}
            ),
            @ApiResponse(responseCode = "200", description = JOB_COMPLETE, headers = {
                @Header(name = EXPIRES, description = FILE_EXPIRES, schema = @Schema(type = "string"))},
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(ref = "#/components/schemas/JobCompletedResponse"))
            ),
            @ApiResponse(responseCode = "404", description = JOB_NOT_FOUND,
                content = @Content(mediaType = FHIR_JSON_CONTENT_TYPE, schema = @Schema(ref = "#/components/schemas/OperationOutcome"))
            )
        }
    )
    @GetMapping(value = "/Job/{jobUuid}/$status", produces = {APPLICATION_JSON, FHIR_JSON_CONTENT_TYPE})
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity<JobCompletedResponse> getJobStatus(HttpServletRequest request,
            @PathVariable @NotBlank String jobUuid) {
        return statusCommon.doStatus(jobUuid, request, API_PREFIX_V1);
    }

    @Operation(summary = BULK_CANCEL)
    @Parameters(value = @Parameter(name = "jobUuid", description = JOB_ID, required = true, in = ParameterIn.PATH))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = JOB_CANCELLED_MSG),
            @ApiResponse(responseCode = "404", description = JOB_NOT_FOUND,
                content = @Content(mediaType = FHIR_JSON_CONTENT_TYPE, schema = @Schema(ref = "#/components/schemas/OperationOutcome"))
            )
        }
    )
    @DeleteMapping(value = "/Job/{jobUuid}/$status")
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    public ResponseEntity deleteRequest(HttpServletRequest request,
            @PathVariable @NotBlank String jobUuid) {
        return statusCommon.cancelJob(jobUuid, request);
    }
}
