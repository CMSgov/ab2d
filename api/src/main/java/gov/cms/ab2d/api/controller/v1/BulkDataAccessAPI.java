package gov.cms.ab2d.api.controller.v1;

import gov.cms.ab2d.api.controller.common.ApiCommon;
import gov.cms.ab2d.api.util.SwaggerConstants;
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

import static gov.cms.ab2d.api.util.SwaggerConstants.BULK_EXPORT;
import static gov.cms.ab2d.api.util.SwaggerConstants.BULK_PREFER;
import static gov.cms.ab2d.api.util.SwaggerConstants.BULK_EXPORT_TYPE;
import static gov.cms.ab2d.api.util.SwaggerConstants.BULK_OUTPUT_FORMAT;
import static gov.cms.ab2d.api.util.SwaggerConstants.BULK_CONTRACT_EXPORT;
import static gov.cms.ab2d.common.util.Constants.*;
import static gov.cms.ab2d.fhir.BundleUtils.EOB;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;

/**
 * The sole REST controller for AB2D's implementation of the FHIR Bulk Data API specification.
 */
@Slf4j
@Api(value = "Bulk Data Access API", description = SwaggerConstants.BULK_MAIN, tags = {"Export"})
@RestController
@RequestMapping(path = API_PREFIX_V1 + FHIR_PREFIX, produces = {"application/json"})
@SuppressWarnings("PMD.TooManyStaticImports")
public class BulkDataAccessAPI {
    private final JobService jobService;
    private final ApiCommon apiCommon;

    public BulkDataAccessAPI(JobService jobService, ApiCommon apiCommon) {
        this.jobService = jobService;
        this.apiCommon = apiCommon;
    }

    @ApiOperation(value = BULK_EXPORT,
        authorizations = {
            @Authorization(value = "Authorization", scopes = {
                    @AuthorizationScope(description = "Export Patient Information", scope = "Authorization") })
        })
    @ApiImplicitParams(value = {
            @ApiImplicitParam(name = "Prefer", required = true, paramType = "header", value =
                    BULK_PREFER, allowableValues = "respond-async", defaultValue = "respond-async", type = "string")}
    )
    @ApiResponses(
            @ApiResponse(code = 202, message = "Export request has started", responseHeaders =
            @ResponseHeader(name = "Content-Location", description = "Absolute URL of an endpoint" +
                    " for subsequent status requests (polling location)",
                    response = String.class), response = String.class)
    )
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    @GetMapping("/Patient/$export")
    public ResponseEntity<Void> exportAllPatients(
            HttpServletRequest request,
            @RequestHeader(name = "Prefer", defaultValue = "respond-async")
            @ApiParam(value = BULK_EXPORT_TYPE, allowableValues = EOB, defaultValue = EOB)
            @RequestParam(required = false, name = "_type", defaultValue = EOB) String resourceTypes,
            @ApiParam(value = BULK_OUTPUT_FORMAT,
                    allowableValues = ApiCommon.ALLOWABLE_OUTPUT_FORMATS, defaultValue = "application/fhir" +
                    "+ndjson"
            )
            @RequestParam(required = false, name = "_outputFormat") String outputFormat,
            @ApiParam(value = "Beginning time of query. Returns all records \"since\" this time. At this time, it must be after " + SINCE_EARLIEST_DATE,
                    example = SINCE_EARLIEST_DATE)
            @RequestParam(required = false, name = "_since") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime since) {

        log.info("Received request to export");
        apiCommon.checkValidCreateJob(since, resourceTypes, outputFormat);
        Job job = jobService.createJob(resourceTypes, apiCommon.getCurrentUrl(), null, outputFormat, since, STU3);
        apiCommon.logSuccessfulJobCreation(job);
        return apiCommon.returnStatusForJobCreation(job, (String) request.getAttribute(REQUEST_ID));
    }

    @ApiOperation(value = BULK_CONTRACT_EXPORT,
            authorizations = {
                    @Authorization(value = "Authorization", scopes = {
                            @AuthorizationScope(description = "Export Claim Data", scope = "Authorization") })
            })
    @ApiResponses(
            @ApiResponse(code = 202, message = "Export request has started", responseHeaders =
            @ResponseHeader(name = "Content-Location", description = "Absolute URL of an endpoint" +
                    " for subsequent status requests (polling location)",
                    response = String.class), response = String.class)
    )
    // todo: This endpoint no longer makes sense in the new model where one Okta credential maps to one Contract
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    @GetMapping("/Group/{contractNumber}/$export")
    public ResponseEntity<Void> exportPatientsWithContract(
            HttpServletRequest request,
            @ApiParam(value = "A contract number", required = true)
            @PathVariable @NotBlank String contractNumber,
            @ApiParam(value = BULK_EXPORT_TYPE, allowableValues = EOB, defaultValue = EOB)
            @RequestParam(required = false, name = "_type") String resourceTypes,
            @ApiParam(value = BULK_OUTPUT_FORMAT,
                    allowableValues = ApiCommon.ALLOWABLE_OUTPUT_FORMATS, defaultValue = "application/fhir" +
                    "+ndjson"
            )
            @RequestParam(required = false, name = "_outputFormat") String outputFormat,
            @RequestHeader(value = "respond-async")
            @ApiParam(value = "Beginning time of query. Returns all records \"since\" this time. At this time, it must be after " + SINCE_EARLIEST_DATE,
                      example = SINCE_EARLIEST_DATE)
            @RequestParam(required = false, name = "_since") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime since) {
            MDC.put(CONTRACT_LOG, contractNumber);

        log.info("Received request to export by contractNumber");
        apiCommon.checkValidCreateJob(since, resourceTypes, outputFormat);
        Job job = jobService.createJob(resourceTypes, apiCommon.getCurrentUrl(), contractNumber, outputFormat, since, STU3);
        apiCommon.logSuccessfulJobCreation(job);
        return apiCommon.returnStatusForJobCreation(job, (String) request.getAttribute(REQUEST_ID));
    }
}
