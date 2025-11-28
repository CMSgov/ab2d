package gov.cms.ab2d.api.controller.v3;

import gov.cms.ab2d.api.controller.common.ApiCommon;
import gov.cms.ab2d.api.remote.JobClient;
import gov.cms.ab2d.api.util.SwaggerConstants;
import gov.cms.ab2d.job.dto.StartJobDTO;
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
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

import static gov.cms.ab2d.api.controller.common.ApiText.*;
import static gov.cms.ab2d.api.util.SwaggerConstants.*;
import static gov.cms.ab2d.common.util.Constants.*;
import static gov.cms.ab2d.fhir.BundleUtils.EOB;
import static gov.cms.ab2d.fhir.FhirVersion.R4;
import static org.springframework.http.HttpHeaders.CONTENT_LOCATION;

/**
 * The sole REST controller for AB2D's implementation of the FHIR Bulk Data API specification.
 */
@Slf4j
@Tag(description = SwaggerConstants.BULK_MAIN, name = "Export")
@RestController
@ConditionalOnExpression("${v3.controller.enabled:true}")
@RequestMapping(path = API_PREFIX_V3 + FHIR_PREFIX, produces = {FHIR_JSON_CONTENT_TYPE, APPLICATION_JSON})
public class BulkDataAccessAPIV3 {
    private final JobClient jobClient;
    private final ApiCommon apiCommon;

    public BulkDataAccessAPIV3(JobClient jobClient, ApiCommon apiCommon) {
        this.jobClient = jobClient;
        this.apiCommon = apiCommon;
    }

    @Operation(summary = BULK_EXPORT)
    @Parameters(value = {
            @Parameter(name = PREFER, required = true, in = ParameterIn.HEADER, description =
                    BULK_PREFER, schema = @Schema(type = "string", allowableValues = ASYNC, defaultValue = ASYNC)),
            @Parameter(name = TYPE_PARAM, description = BULK_EXPORT_TYPE, in = ParameterIn.QUERY, schema = @Schema(allowableValues = EOB, defaultValue = EOB)),
            @Parameter(name = OUT_FORMAT, description = BULK_OUTPUT_FORMAT, in = ParameterIn.QUERY,
                    schema = @Schema(allowableValues = {
                            "application/fhir+ndjson", "application/ndjson", "ndjson"
                    }, defaultValue = FHIR_NDJSON_CONTENT_TYPE)
            ),
            @Parameter(name = SINCE, description = BULK_SINCE_DEFAULT, schema = @Schema(type = "date-time", description = SINCE_EARLIEST_DATE)),
            @Parameter(name = UNTIL, description = BULK_UNTIL_DEFAULT, schema = @Schema(type = "date-time", description = UNTIL_EXAMPLE_DATE)),
    }
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = EXPORT_STARTED, headers =
            @Header(name = CONTENT_LOCATION, description = BULK_RESPONSE, schema = @Schema(type = "string")),
                    content = @Content(schema = @Schema(type = "string"))
            ),
            @ApiResponse(responseCode = "429", description = MAX_JOBS, headers =
            @Header(name = CONTENT_LOCATION, description = RUNNING_JOBIDS, schema = @Schema(type = "string")),
                    content = @Content(mediaType = FHIR_JSON_CONTENT_TYPE, schema = @Schema(ref = "#/components/schemas/OperationOutcome"))
            )
    }
    )
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    @GetMapping("/Patient/$export")
    public ResponseEntity<Void> exportAllPatients(
            HttpServletRequest request,
            @RequestParam(name = TYPE_PARAM, required = false, defaultValue = EOB)
            String resourceTypes,
            @RequestParam(name = OUT_FORMAT, required = false, defaultValue = FHIR_NDJSON_CONTENT_TYPE)
            String outputFormat,
            @RequestParam(required = false, name = SINCE) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime since,
            @RequestParam(required = false, name = UNTIL) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime until) {
        log.info("Received request to export");

        StartJobDTO startJobDTO = apiCommon.checkValidCreateJob(request, null, since, until, resourceTypes,
                outputFormat, R4);
        String jobGuid = jobClient.createJob(startJobDTO);
        apiCommon.logSuccessfulJobCreation(jobGuid);
        return apiCommon.returnStatusForJobCreation(jobGuid, API_PREFIX_V3, (String) request.getAttribute(REQUEST_ID), request);
    }
}

