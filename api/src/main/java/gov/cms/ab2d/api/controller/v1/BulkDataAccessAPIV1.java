package gov.cms.ab2d.api.controller.v1;

import gov.cms.ab2d.api.controller.common.ApiCommon;
import gov.cms.ab2d.job.dto.StartJobDTO;
import gov.cms.ab2d.api.remote.JobClient;
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
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


import static gov.cms.ab2d.api.controller.common.ApiText.APPLICATION_JSON;
import static gov.cms.ab2d.api.controller.common.ApiText.ASYNC;
import static gov.cms.ab2d.api.controller.common.ApiText.BULK_RESPONSE;
import static gov.cms.ab2d.api.controller.common.ApiText.BULK_SINCE;
import static gov.cms.ab2d.api.controller.common.ApiText.CONTRACT_NO;
import static gov.cms.ab2d.api.controller.common.ApiText.EXPORT_STARTED;
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
import static gov.cms.ab2d.common.util.Constants.API_PREFIX_V1;
import static gov.cms.ab2d.common.util.Constants.CONTRACT_LOG;
import static gov.cms.ab2d.common.util.Constants.FHIR_PREFIX;
import static gov.cms.ab2d.common.util.Constants.NDJSON_FIRE_CONTENT_TYPE;
import static gov.cms.ab2d.common.util.Constants.REQUEST_ID;
import static gov.cms.ab2d.common.util.Constants.SINCE_EARLIEST_DATE;
import static gov.cms.ab2d.fhir.BundleUtils.EOB;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static org.springframework.http.HttpHeaders.CONTENT_LOCATION;

/**
 * The sole REST controller for AB2D's implementation of the FHIR Bulk Data API specification.
 */
@Slf4j
@Tag(name = "2. Export", description = BULK_MAIN)
@RestController
@RequestMapping(path = API_PREFIX_V1 + FHIR_PREFIX, produces = {APPLICATION_JSON})
public class BulkDataAccessAPIV1 {
    private final JobClient jobClient;
    private final ApiCommon apiCommon;

    public BulkDataAccessAPIV1(JobClient jobClient, ApiCommon apiCommon) {
        this.jobClient = jobClient;
        this.apiCommon = apiCommon;
    }

    @ResponseStatus(value = HttpStatus.ACCEPTED)
    @Parameters(value = {
            @Parameter(name = PREFER, required = true, in = ParameterIn.HEADER, description =
                BULK_PREFER, schema = @Schema(type = "string", allowableValues = ASYNC, defaultValue = ASYNC)),
            @Parameter(name = TYPE_PARAM, description = BULK_EXPORT_TYPE, in = ParameterIn.QUERY, schema = @Schema(allowableValues = EOB, defaultValue = EOB)),
            @Parameter(name = OUT_FORMAT, description = BULK_OUTPUT_FORMAT, in = ParameterIn.QUERY,
                schema = @Schema(allowableValues = {
                    "application/fhir+ndjson", "application/ndjson", "ndjson"
                }, defaultValue = NDJSON_FIRE_CONTENT_TYPE)
            ),
            @Parameter(name = SINCE, description = BULK_SINCE, schema = @Schema(type = "date-time", description = SINCE_EARLIEST_DATE))

        }
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = EXPORT_STARTED, headers =
                @Header(name = CONTENT_LOCATION, description = BULK_RESPONSE, schema = @Schema(type = "string"))
            ),
            @ApiResponse(responseCode = "429", description = MAX_JOBS,
                headers = @Header(name = CONTENT_LOCATION, description = RUNNING_JOBIDS, schema = @Schema(type = "string")),
                content = @Content(schema = @Schema(ref = "#/components/schemas/OperationOutcome"))
            )
        }
    )
    @Operation(summary = BULK_EXPORT)
    @GetMapping("/Patient/$export")
    public ResponseEntity<Void> exportAllPatients(
            HttpServletRequest request,
            @RequestParam(name = TYPE_PARAM, required = false, defaultValue = EOB)
                String resourceTypes,
            @RequestParam(name = OUT_FORMAT, required = false, defaultValue = NDJSON_FIRE_CONTENT_TYPE)
                String outputFormat,
            @RequestParam(required = false, name = SINCE) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                OffsetDateTime since) {

        log.info("Received request to export");
        StartJobDTO startJobDTO = apiCommon.checkValidCreateJob(request, null, since, resourceTypes, outputFormat, STU3);
        String jobGuid = jobClient.createJob(startJobDTO);
        apiCommon.logSuccessfulJobCreation(jobGuid);
        return apiCommon.returnStatusForJobCreation(jobGuid, API_PREFIX_V1, (String) request.getAttribute(REQUEST_ID), request);
    }

    @Deprecated
    @Operation(summary = BULK_CONTRACT_EXPORT)
    @Parameters(value = {
            @Parameter(name = PREFER, required = true, in = ParameterIn.HEADER, description =
                BULK_PREFER, schema = @Schema(type = "string", allowableValues = ASYNC, defaultValue = ASYNC)),
            @Parameter(name = "contractNumber", required = true, in = ParameterIn.PATH, description = CONTRACT_NO, example = "Z0000"),
            @Parameter(name = TYPE_PARAM, description = BULK_EXPORT_TYPE, in = ParameterIn.QUERY, schema = @Schema(allowableValues = EOB, defaultValue = EOB)),
            @Parameter(name = OUT_FORMAT, description = BULK_OUTPUT_FORMAT, in = ParameterIn.QUERY,
                schema = @Schema(allowableValues = {
                    "application/fhir+ndjson", "application/ndjson", "ndjson"
                }, defaultValue = NDJSON_FIRE_CONTENT_TYPE)
            ),
            @Parameter(name = SINCE, description = BULK_SINCE, example = SINCE_EARLIEST_DATE, schema = @Schema(type = "date-time"))
        }
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = EXPORT_STARTED, headers =
                @Header(name = CONTENT_LOCATION, description = BULK_RESPONSE, schema = @Schema(type = "string"))
            ),
            @ApiResponse(responseCode = "429", description = MAX_JOBS,
                headers = @Header(name = CONTENT_LOCATION, description = RUNNING_JOBIDS, schema = @Schema(type = "string")),
                content = @Content(schema = @Schema(ref = "#/components/schemas/OperationOutcome"))
            )
        }
    )
    // todo: This endpoint no longer makes sense in the new model where one Okta credential maps to one Contract
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    @GetMapping("/Group/{contractNumber}/$export")
    public ResponseEntity<Void> exportPatientsWithContract(
            HttpServletRequest request,
            @PathVariable @NotBlank
                    String contractNumber,
            @RequestParam(required = false, name = TYPE_PARAM)
                    String resourceTypes,
            @RequestParam(required = false, name = OUT_FORMAT)
                    String outputFormat,
            @RequestParam(required = false, name = SINCE) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    OffsetDateTime since
    ) {

        MDC.put(CONTRACT_LOG, contractNumber);
        log.info("Received request to export by contractNumber");
        StartJobDTO startJobDTO = apiCommon.checkValidCreateJob(request, contractNumber, since, resourceTypes,
                outputFormat, STU3);
        String jobGuid = jobClient.createJob(startJobDTO);
        apiCommon.logSuccessfulJobCreation(jobGuid);
        return apiCommon.returnStatusForJobCreation(jobGuid, API_PREFIX_V1, (String) request.getAttribute(REQUEST_ID), request);
    }
}
