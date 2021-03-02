package gov.cms.ab2d.api.controller.v2;

import gov.cms.ab2d.api.config.SwaggerConfig;
import gov.cms.ab2d.api.controller.JobCompletedResponse;
import gov.cms.ab2d.api.controller.common.StatusCommon;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.AuthorizationScope;
import io.swagger.annotations.ResponseHeader;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

import static gov.cms.ab2d.api.controller.common.ApiCommon.JOB_CANCELLED_MSG;
import static gov.cms.ab2d.api.controller.common.ApiText.BULK_DATA_API;
import static gov.cms.ab2d.api.controller.common.ApiText.CANCEL_JOB;
import static gov.cms.ab2d.api.controller.common.ApiText.FILE_EXPIRES;
import static gov.cms.ab2d.api.controller.common.ApiText.JOB_COMPLETE;
import static gov.cms.ab2d.api.controller.common.ApiText.JOB_ID;
import static gov.cms.ab2d.api.controller.common.ApiText.JOB_NOT_FOUND;
import static gov.cms.ab2d.api.controller.common.ApiText.APPLICATION_JSON;
import static gov.cms.ab2d.api.controller.common.ApiText.PROGRESS;
import static gov.cms.ab2d.api.controller.common.ApiText.EXPORT_JOB_STATUS;
import static gov.cms.ab2d.api.controller.common.ApiText.STATUS_API;
import static gov.cms.ab2d.api.controller.common.ApiText.STATUS_DELAY;
import static gov.cms.ab2d.api.controller.common.ApiText.STATUS_DES;
import static gov.cms.ab2d.api.controller.common.ApiText.STILL_RUNNING;
import static gov.cms.ab2d.api.controller.common.ApiText.X_PROG;
import static gov.cms.ab2d.api.util.SwaggerConstants.BULK_CANCEL;
import static gov.cms.ab2d.common.util.Constants.API_PREFIX_V2;
import static gov.cms.ab2d.common.util.Constants.FHIR_PREFIX;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.EXPIRES;
import static org.springframework.http.HttpHeaders.RETRY_AFTER;

/**
 * The sole REST controller for AB2D's implementation of the FHIR Bulk Data API Status (both GET & DELETE).
 */
@Slf4j
@Api(value = BULK_DATA_API, description = STATUS_API, tags = {"Status"})
@RestController
@ConditionalOnExpression("${v2.controller.enabled:false}")
@RequestMapping(path = API_PREFIX_V2 + FHIR_PREFIX, produces = {APPLICATION_JSON})
@AllArgsConstructor
@SuppressWarnings("PMD.TooManyStaticImports")
public class StatusAPIV2 {

    private final StatusCommon statusCommon;

    @ApiOperation(value = STATUS_DES, authorizations = {
            @Authorization(value = AUTHORIZATION, scopes = { @AuthorizationScope(description = EXPORT_JOB_STATUS, scope = AUTHORIZATION) })
    })
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = STILL_RUNNING, responseHeaders = {
                    @ResponseHeader(name = X_PROG, description = PROGRESS, response = String.class),
                    @ResponseHeader(name = RETRY_AFTER, description = STATUS_DELAY, response = Integer.class)}),
            @ApiResponse(code = 200, message = JOB_COMPLETE, responseHeaders = {
                    @ResponseHeader(name = EXPIRES, description = FILE_EXPIRES, response = String.class)},
                    response = JobCompletedResponse.class),
            @ApiResponse(code = 404, message = JOB_NOT_FOUND, response = SwaggerConfig.OperationOutcome.class)}
    )
    @GetMapping(value = "/Job/{jobUuid}/$status", produces = APPLICATION_JSON)
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity<JobCompletedResponse> getJobStatus(HttpServletRequest request,
            @ApiParam(value = JOB_ID, required = true) @PathVariable @NotBlank String jobUuid) {
        return statusCommon.doStatus(jobUuid, request, API_PREFIX_V2);
    }

    @ApiOperation(value = BULK_CANCEL, authorizations = {
            @Authorization(value = AUTHORIZATION, scopes = { @AuthorizationScope(description = CANCEL_JOB, scope = AUTHORIZATION) }) })
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
