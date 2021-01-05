package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.api.util.SwaggerConstants;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.service.InvalidUserInputException;
import gov.cms.ab2d.common.service.JobService;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.ApiResponseEvent;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.Set;

import static gov.cms.ab2d.api.util.Constants.GENERIC_FHIR_ERR_MSG;
import static gov.cms.ab2d.api.util.SwaggerConstants.BULK_EXPORT;
import static gov.cms.ab2d.api.util.SwaggerConstants.BULK_PREFER;
import static gov.cms.ab2d.api.util.SwaggerConstants.BULK_EXPORT_TYPE;
import static gov.cms.ab2d.api.util.SwaggerConstants.BULK_OUTPUT_FORMAT;
import static gov.cms.ab2d.api.util.SwaggerConstants.BULK_CONTRACT_EXPORT;
import static gov.cms.ab2d.common.service.JobService.ZIPFORMAT;
import static gov.cms.ab2d.common.util.Constants.*;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

/**
 * The sole REST controller for AB2D's implementation of the FHIR Bulk Data API specification.
 */
@Slf4j
@Api(value = "Bulk Data Access API", description = SwaggerConstants.BULK_MAIN, tags = {"Export"})
@RestController
@RequestMapping(path = API_PREFIX + FHIR_PREFIX, produces = {"application/json"})
@SuppressWarnings("PMD.TooManyStaticImports")
public class BulkDataAccessAPI {

    // Since this is used in an annotation, it can't be derived from the Set, otherwise it will be an error
    private static final String ALLOWABLE_OUTPUT_FORMATS =
            "application/fhir+ndjson,application/ndjson,ndjson," + ZIPFORMAT;

    private static final Set<String> ALLOWABLE_OUTPUT_FORMAT_SET = Set.of(ALLOWABLE_OUTPUT_FORMATS.split(","));

    static final String JOB_NOT_FOUND_ERROR_MSG = "Job not found. " + GENERIC_FHIR_ERR_MSG;

    static final String JOB_CANCELLED_MSG = "Job canceled";

    private final JobService jobService;
    private final PropertiesService propertiesService;
    private final LogManager eventLogger;

    public BulkDataAccessAPI(JobService jobService, PropertiesService propertiesService, LogManager eventLogger) {
        this.jobService = jobService;
        this.propertiesService = propertiesService;
        this.eventLogger = eventLogger;
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
                    allowableValues = ALLOWABLE_OUTPUT_FORMATS, defaultValue = "application/fhir" +
                    "+ndjson"
            )
            @RequestParam(required = false, name = "_outputFormat") String outputFormat,
            @ApiParam(value = "Beginning time of query. Returns all records \"since\" this time. At this time, it must be after " + SINCE_EARLIEST_DATE,
                    example = SINCE_EARLIEST_DATE)
            @RequestParam(required = false, name = "_since") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime since) {
        log.info("Received request to export");

        checkIfInMaintenanceMode();
        checkIfCurrentUserCanAddJob();
        checkResourceTypesAndOutputFormat(resourceTypes, outputFormat);
        checkSinceTime(since);

        Job job = jobService.createJob(resourceTypes, getCurrentUrl(), null, outputFormat, since);

        logSuccessfulJobCreation(job);

        return returnStatusForJobCreation(job, (String) request.getAttribute(REQUEST_ID));
    }

    private String getCurrentUrl() {
        return shouldReplaceWithHttps() ?
                ServletUriComponentsBuilder.fromCurrentRequest().scheme("https").toUriString() :
                ServletUriComponentsBuilder.fromCurrentRequest().toUriString().replace(":80/", "/");
    }

    private boolean shouldReplaceWithHttps() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getRequest();
        return "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }

    private void checkSinceTime(OffsetDateTime date) {
        if (date == null) {
            return;
        }
        if (date.isAfter(OffsetDateTime.now())) {
            throw new InvalidUserInputException("You can not use a time after the current time for _since");
        }
        try {
            OffsetDateTime ed = OffsetDateTime.parse(SINCE_EARLIEST_DATE, ISO_DATE_TIME);
            if (date.isBefore(ed)) {
                log.error("Invalid _since time received {}", date);
                throw new InvalidUserInputException("_since must be after " + ed.format(ISO_OFFSET_DATE_TIME));
            }
        } catch (Exception ex) {
            throw new InvalidUserInputException("${api.since.date.earliest} date value '" + SINCE_EARLIEST_DATE + "' is invalid");
        }
    }

    private void checkIfInMaintenanceMode() {
        if (propertiesService.isInMaintenanceMode()) {
            throw new InMaintenanceModeException("The system is currently in maintenance mode. Please try the request again later.");
        }
    }

    private void checkIfCurrentUserCanAddJob() {
        if (!jobService.checkIfCurrentUserCanAddJob()) {
            String errorMsg = "You already have active export requests in progress. Please wait until they complete before submitting a new one.";
            log.error(errorMsg);
            throw new TooManyRequestsException(errorMsg);
        }
    }

    private void checkResourceTypesAndOutputFormat(String resourceTypes, String outputFormat) {
        if (resourceTypes != null && !resourceTypes.equals(EOB)) {
            log.error("Received invalid resourceTypes of {}", resourceTypes);
            throw new InvalidUserInputException("_type must be " + EOB);
        }

        final String errMsg = "An _outputFormat of " + outputFormat + " is not valid";

        if (outputFormat != null && !ALLOWABLE_OUTPUT_FORMAT_SET.contains(outputFormat)) {
            log.error("Received _outputFormat {}, which is not valid", outputFormat);
            throw new InvalidUserInputException(errMsg);
        }

        final boolean zipSupportOn = propertiesService.isToggleOn(ZIP_SUPPORT_ON);
        if (!zipSupportOn && ZIPFORMAT.equalsIgnoreCase(outputFormat)) {
            throw new InvalidUserInputException(errMsg);
        }
    }

    private void logSuccessfulJobCreation(Job job) {
        MDC.put(JOB_LOG, job.getJobUuid());
        log.info("Successfully created job");
    }

    private ResponseEntity<Void> returnStatusForJobCreation(Job job, String requestId) {
        String statusURL = getUrl(API_PREFIX + FHIR_PREFIX + "/Job/" + job.getJobUuid() + "/$status");
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("Content-Location", statusURL);
        eventLogger.log(new ApiResponseEvent(MDC.get(USERNAME), job.getJobUuid(), HttpStatus.ACCEPTED, "Job Created",
                "Job " + job.getJobUuid() + " was created", requestId));
        return new ResponseEntity<>(null, responseHeaders,
                HttpStatus.ACCEPTED);
    }

    private String getUrl(String ending) {
        return shouldReplaceWithHttps() ?
                ServletUriComponentsBuilder.fromCurrentRequestUri().scheme("https").replacePath(ending).toUriString() :
                ServletUriComponentsBuilder.fromCurrentRequestUri().replacePath(ending).toUriString().replace(":80/", "/");
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
                    allowableValues = ALLOWABLE_OUTPUT_FORMATS, defaultValue = "application/fhir" +
                    "+ndjson"
            )
            @RequestParam(required = false, name = "_outputFormat") String outputFormat,
            @RequestHeader(value = "respond-async")
            @ApiParam(value = "Beginning time of query. Returns all records \"since\" this time. At this time, it must be after " + SINCE_EARLIEST_DATE,
                      example = SINCE_EARLIEST_DATE)
            @RequestParam(required = false, name = "_since") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime since) {
            MDC.put(CONTRACT_LOG, contractNumber);
        log.info("Received request to export by contractNumber");

        checkIfInMaintenanceMode();
        checkIfCurrentUserCanAddJob();
        checkResourceTypesAndOutputFormat(resourceTypes, outputFormat);
        checkSinceTime(since);

        Job job = jobService.createJob(resourceTypes, getCurrentUrl(), contractNumber, outputFormat, since);

        logSuccessfulJobCreation(job);

        return returnStatusForJobCreation(job, (String) request.getAttribute(REQUEST_ID));
    }
}
