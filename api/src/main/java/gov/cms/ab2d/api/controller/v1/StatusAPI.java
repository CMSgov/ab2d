package gov.cms.ab2d.api.controller.v1;

import gov.cms.ab2d.api.config.SwaggerConfig;
import gov.cms.ab2d.api.controller.JobCompletedResponse;
import gov.cms.ab2d.api.controller.common.StatusCommon;
import io.swagger.annotations.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

import static gov.cms.ab2d.api.controller.common.ApiText.BULK_DATA_API;
import static gov.cms.ab2d.api.controller.common.ApiText.STATUS_API;
import static gov.cms.ab2d.api.controller.common.ApiText.JSON;
import static gov.cms.ab2d.api.controller.common.ApiText.STATUS_DES;
import static gov.cms.ab2d.api.controller.common.ApiText.AUTH;
import static gov.cms.ab2d.api.controller.common.ApiText.STATUS;
import static gov.cms.ab2d.api.controller.common.ApiText.STILL_RUNNING;
import static gov.cms.ab2d.api.controller.common.ApiText.PROGRESS;
import static gov.cms.ab2d.api.controller.common.ApiText.STATUS_DELAY;
import static gov.cms.ab2d.api.controller.common.ApiText.JOB_COMPLETE;
import static gov.cms.ab2d.api.controller.common.ApiText.FILE_EXPIRES;
import static gov.cms.ab2d.api.controller.common.ApiText.JOB_NOT_FOUND;
import static gov.cms.ab2d.api.controller.common.ApiText.CANCEL;
import static gov.cms.ab2d.api.controller.common.ApiText.JOB_ID;
import static gov.cms.ab2d.api.controller.common.ApiText.JOB_CANCELLED_MSG;

import static gov.cms.ab2d.api.util.SwaggerConstants.BULK_CANCEL;
import static gov.cms.ab2d.common.util.Constants.API_PREFIX_V1;
import static gov.cms.ab2d.common.util.Constants.FHIR_PREFIX;

/**
 * The sole REST controller for AB2D's implementation of the FHIR Bulk Data API Status (both GET & DELETE).
 */
@Slf4j
@Api(value = BULK_DATA_API, description = STATUS_API, tags = {"Status"})
@RestController
@RequestMapping(path = API_PREFIX_V1 + FHIR_PREFIX, produces = {JSON})
@AllArgsConstructor
@SuppressWarnings("PMD.TooManyStaticImports")
public class StatusAPI {

    private final StatusCommon statusCommon;

    @ApiOperation(value = STATUS_DES, authorizations = {
            @Authorization(value = AUTH, scopes = { @AuthorizationScope(description = STATUS, scope = AUTH) })
    })
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = STILL_RUNNING, responseHeaders = {
                    @ResponseHeader(name = "X-Progress", description = PROGRESS, response = String.class),
                    @ResponseHeader(name = "Retry-After", description = STATUS_DELAY, response = Integer.class)}),
            @ApiResponse(code = 200, message = JOB_COMPLETE, responseHeaders = {
                    @ResponseHeader(name = "Expires", description = FILE_EXPIRES, response = String.class)},
                    response = JobCompletedResponse.class),
            @ApiResponse(code = 404, message = JOB_NOT_FOUND, response = SwaggerConfig.OperationOutcome.class)}
    )
    @GetMapping(value = "/Job/{jobUuid}/$status", produces = JSON)
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity<JobCompletedResponse> getJobStatus(HttpServletRequest request,
            @ApiParam(value = JOB_ID, required = true) @PathVariable @NotBlank String jobUuid) {
        return statusCommon.doStatus(jobUuid, request, API_PREFIX_V1);
    }

    @ApiOperation(value = BULK_CANCEL, authorizations = {
                    @Authorization(value = AUTH, scopes = { @AuthorizationScope(description = CANCEL, scope = AUTH) }) })
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = JOB_CANCELLED_MSG),
            @ApiResponse(code = 404, message = JOB_NOT_FOUND, response = SwaggerConfig.OperationOutcome.class)}
    )
    @DeleteMapping(value = "/Job/{jobUuid}/$status")
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    public ResponseEntity deleteRequest(HttpServletRequest request,
            @ApiParam(value = JOB_ID, required = true)
            @PathVariable @NotBlank String jobUuid) {
        return statusCommon.cancelJob(jobUuid, request);
    }
}
