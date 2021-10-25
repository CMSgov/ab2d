package gov.cms.ab2d.api.controller.v1;

import gov.cms.ab2d.api.config.SwaggerConfig;
import gov.cms.ab2d.api.controller.common.ApiCommon;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.service.JobService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.AuthorizationScope;
import io.swagger.annotations.ResponseHeader;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;
import java.time.OffsetDateTime;

import static gov.cms.ab2d.api.controller.common.ApiText.APPLICATION_JSON;
import static gov.cms.ab2d.api.controller.common.ApiText.ASYNC;
import static gov.cms.ab2d.api.controller.common.ApiText.BULK_RESPONSE;
import static gov.cms.ab2d.api.controller.common.ApiText.BULK_RESPONSE_LONG;
import static gov.cms.ab2d.api.controller.common.ApiText.BULK_SINCE;
import static gov.cms.ab2d.api.controller.common.ApiText.CONTRACT_NO;
import static gov.cms.ab2d.api.controller.common.ApiText.EXPORT_CLAIM;
import static gov.cms.ab2d.api.controller.common.ApiText.EXPORT_STARTED;
import static gov.cms.ab2d.api.controller.common.ApiText.EXP_PATIENT_INFO;
import static gov.cms.ab2d.api.controller.common.ApiText.MAX_JOBS;
import static gov.cms.ab2d.api.controller.common.ApiText.OUT_FORMAT;
import static gov.cms.ab2d.api.controller.common.ApiText.PREFER;
import static gov.cms.ab2d.api.controller.common.ApiText.RUNNING_JOBIDS;
import static gov.cms.ab2d.api.controller.common.ApiText.SINCE;
import static gov.cms.ab2d.api.controller.common.ApiText.TYPE_PARAM;
import static gov.cms.ab2d.api.util.SwaggerConstants.BULK_CONTRACT_EXPORT;
import static gov.cms.ab2d.api.util.SwaggerConstants.BULK_EXPORT;
import static gov.cms.ab2d.api.util.SwaggerConstants.BULK_EXPORT_TYPE;
import static gov.cms.ab2d.api.util.SwaggerConstants.BULK_MAIN;
import static gov.cms.ab2d.api.util.SwaggerConstants.BULK_OUTPUT_FORMAT;
import static gov.cms.ab2d.api.util.SwaggerConstants.BULK_PREFER;
import static gov.cms.ab2d.common.util.Constants.*;
import static gov.cms.ab2d.fhir.BundleUtils.EOB;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_LOCATION;

/**
 * The sole REST controller for AB2D's implementation of the FHIR Bulk Data API specification.
 */
@Slf4j
@Api(value = "Bulk Data Access API", description = BULK_MAIN, tags = {"Export"})
@RestController
@RequestMapping(path = API_PREFIX_V1 + FHIR_PREFIX, produces = {APPLICATION_JSON})
@SuppressWarnings({"PMD.TooManyStaticImports", "PMD.UnusedImports"})
public class BulkDataAccessAPIV1 {
    private final JobService jobService;
    private final ApiCommon apiCommon;

    public BulkDataAccessAPIV1(JobService jobService, ApiCommon apiCommon) {
        this.jobService = jobService;
        this.apiCommon = apiCommon;
    }

    @ApiOperation(value = BULK_EXPORT,
        authorizations = {
            @Authorization(value = AUTHORIZATION, scopes = { @AuthorizationScope(description = EXP_PATIENT_INFO, scope = AUTHORIZATION) })
        })
    @ApiImplicitParams(value = {
            @ApiImplicitParam(name = PREFER, required = true, paramType = "header", value =
                    BULK_PREFER, allowableValues = ASYNC, defaultValue = ASYNC, type = "string")}
    )
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = EXPORT_STARTED, responseHeaders =
            @ResponseHeader(name = CONTENT_LOCATION, description = BULK_RESPONSE, response = String.class), response = String.class),
            @ApiResponse(code = 429, message = MAX_JOBS, responseHeaders =
            @ResponseHeader(name = "jobs", description = RUNNING_JOBIDS, response = String.class), response = SwaggerConfig.OperationOutcome.class)}
    )
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    @GetMapping("/Patient/$export")
    public ResponseEntity<Void> exportAllPatients(
            HttpServletRequest request,
            @RequestHeader(name = PREFER, defaultValue = ASYNC)
            @ApiParam(value = BULK_EXPORT_TYPE, allowableValues = EOB, defaultValue = EOB)
            @RequestParam(required = false, name = TYPE_PARAM, defaultValue = EOB) String resourceTypes,
            @ApiParam(value = BULK_OUTPUT_FORMAT,
                    allowableValues = ApiCommon.ALLOWABLE_OUTPUT_FORMATS, defaultValue = NDJSON_FIRE_CONTENT_TYPE
            )
            @RequestParam(required = false, name = OUT_FORMAT) String outputFormat,
            @ApiParam(value = BULK_SINCE, example = SINCE_EARLIEST_DATE)
            @RequestParam(required = false, name = SINCE) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime since) {

        log.info("Received request to export");
        apiCommon.checkValidCreateJob(since, resourceTypes, outputFormat);
        Job job = jobService.createJob(resourceTypes, apiCommon.getCurrentUrl(request), null, outputFormat, since, STU3);
        apiCommon.logSuccessfulJobCreation(job);
        return apiCommon.returnStatusForJobCreation(job, API_PREFIX_V1, (String) request.getAttribute(REQUEST_ID), request);
    }

    @ApiOperation(value = BULK_CONTRACT_EXPORT,
            authorizations = {
                    @Authorization(value = AUTHORIZATION, scopes = { @AuthorizationScope(description = EXPORT_CLAIM, scope = AUTHORIZATION) })
            })
    @ApiResponses(
            @ApiResponse(code = 202, message = EXPORT_STARTED, responseHeaders =
            @ResponseHeader(name = CONTENT_LOCATION, description = BULK_RESPONSE_LONG,
                    response = String.class), response = String.class)
    )
    // todo: This endpoint no longer makes sense in the new model where one Okta credential maps to one Contract
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    @GetMapping("/Group/{contractNumber}/$export")
    public ResponseEntity<Void> exportPatientsWithContract(
            HttpServletRequest request,
            @ApiParam(value = CONTRACT_NO, required = true)
            @PathVariable @NotBlank String contractNumber,
            @ApiParam(value = BULK_EXPORT_TYPE, allowableValues = EOB, defaultValue = EOB)
            @RequestParam(required = false, name = TYPE_PARAM) String resourceTypes,
            @ApiParam(value = BULK_OUTPUT_FORMAT,
                    allowableValues = ApiCommon.ALLOWABLE_OUTPUT_FORMATS, defaultValue = NDJSON_FIRE_CONTENT_TYPE
            )
            @RequestParam(required = false, name = OUT_FORMAT) String outputFormat,
            @RequestHeader(value = ASYNC)
            @ApiParam(value = BULK_SINCE, example = SINCE_EARLIEST_DATE)
            @RequestParam(required = false, name = SINCE) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime since) {

        MDC.put(CONTRACT_LOG, contractNumber);
        log.info("Received request to export by contractNumber");
        apiCommon.checkValidCreateJob(since, resourceTypes, outputFormat);
        Job job = jobService.createJob(resourceTypes, apiCommon.getCurrentUrl(request), contractNumber, outputFormat, since, STU3);
        apiCommon.logSuccessfulJobCreation(job);
        return apiCommon.returnStatusForJobCreation(job, API_PREFIX_V1, (String) request.getAttribute(REQUEST_ID), request);
    }
}
