package gov.cms.ab2d.api.controller;

import com.google.gson.Gson;
import gov.cms.ab2d.api.config.SwaggerConfig;
import gov.cms.ab2d.api.util.SwaggerConstants;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.service.InvalidUserInputException;
import gov.cms.ab2d.common.service.JobService;
import gov.cms.ab2d.common.service.PropertiesService;
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
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotBlank;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.stream.Collectors;

import static gov.cms.ab2d.api.util.Constants.GENERIC_FHIR_ERR_MSG;
import static gov.cms.ab2d.api.util.SwaggerConstants.*;
import static gov.cms.ab2d.common.service.JobService.ZIPFORMAT;
import static gov.cms.ab2d.common.util.Constants.*;

@Slf4j
@Api(value = "Bulk Data Access API", description = SwaggerConstants.BULK_MAIN)
@RestController
@RequestMapping(path = API_PREFIX + FHIR_PREFIX, produces = {"application/json", NDJSON_FIRE_CONTENT_TYPE})
/**
 * The sole REST controller for AB2D's implementation of the FHIR Bulk Data API specification.
 */
public class BulkDataAccessAPI {

    // Since this is used in an annotation, it can't be derived from the Set, otherwise it will be an error
    private static final String ALLOWABLE_OUTPUT_FORMATS =
            "application/fhir+ndjson,application/ndjson,ndjson," + ZIPFORMAT;

    private static final Set<String> ALLOWABLE_OUTPUT_FORMAT_SET = Set.of(ALLOWABLE_OUTPUT_FORMATS.split(","));

    public static final String JOB_NOT_FOUND_ERROR_MSG = "Job not found. " + GENERIC_FHIR_ERR_MSG;

    public static final String JOB_CANCELLED_MSG = "Job canceled";

    @Value("${api.retry-after.delay}")
    private int retryAfterDelay;

    @Autowired
    private JobService jobService;

    @Autowired
    private PropertiesService propertiesService;

    @ApiOperation(value = BULK_EXPORT,
        authorizations = {
            @Authorization(value = "Authorization", scopes = {
                    @AuthorizationScope(description = "Export Patient Information", scope = "Authorization") })
        })
    @ApiImplicitParams(value = {
            @ApiImplicitParam(name = "Accept", required = true, paramType = "header", value =
                    BULK_ACCEPT, defaultValue = "application/fhir+json"),
            @ApiImplicitParam(name = "Prefer", required = true, paramType = "header", value =
                    BULK_PREFER, defaultValue = "respond-async")}
    )
    @ApiResponses(
            @ApiResponse(code = 202, message = "Export request has started", responseHeaders =
            @ResponseHeader(name = "Content-Location", description = "Absolute URL of an endpoint" +
                    " for subsequent status requests (polling location)",
                    response = String.class))
    )
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    @GetMapping("/Patient/$export")
    public ResponseEntity<Void> exportAllPatients(
            @ApiParam(value = BULK_EXPORT_TYPE, allowableValues = EOB, defaultValue = EOB)
            @RequestParam(required = false, name = "_type", defaultValue = EOB) String resourceTypes,
            @ApiParam(value = BULK_OUTPUT_FORMAT,
                    allowableValues = ALLOWABLE_OUTPUT_FORMATS, defaultValue = "application/fhir" +
                    "+ndjson"
            )
            @RequestParam(required = false, name = "_outputFormat") String outputFormat) {
        log.info("Received request to export");

        checkIfInMaintenanceMode();

        checkIfCurrentUserCanAddJob();

        checkResourceTypesAndOutputFormat(resourceTypes, outputFormat);

        Job job = jobService.createJob(resourceTypes, ServletUriComponentsBuilder.fromCurrentRequest().toUriString(),
                outputFormat);

        logSuccessfulJobCreation(job);

        return returnStatusForJobCreation(job);
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

        if (outputFormat != null && !ALLOWABLE_OUTPUT_FORMAT_SET.contains(outputFormat)) {
            log.error("Received _outputFormat {}, which is not valid", outputFormat);
            throw new InvalidUserInputException("An _outputFormat of " + outputFormat + " is not valid");
        }

        final boolean zipSupportOn = propertiesService.isToggleOn(ZIP_SUPPORT_ON);
        if (!zipSupportOn && ZIPFORMAT.equalsIgnoreCase(outputFormat)) {
            throw new InvalidUserInputException("An _outputFormat of " + outputFormat + " is not valid");
        }
    }

    private void logSuccessfulJobCreation(Job job) {
        MDC.put(JOB_LOG, job.getJobUuid());
        log.info("Successfully created job");
    }

    private ResponseEntity<Void> returnStatusForJobCreation(Job job) {
        String statusURL = ServletUriComponentsBuilder.fromCurrentRequestUri().replacePath
                (String.format(API_PREFIX + FHIR_PREFIX + "/Job/%s/$status", job.getJobUuid())).toUriString();
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("Content-Location", statusURL);

        return new ResponseEntity<>(null, responseHeaders,
                HttpStatus.ACCEPTED);
    }

    @ApiOperation(value = BULK_CONTRACT_EXPORT,
            authorizations = {
                    @Authorization(value = "Authorization", scopes = {
                            @AuthorizationScope(description = "Export Claim Data", scope = "Authorization") })
            })
    @ApiImplicitParams(value = {
            @ApiImplicitParam(name = "Accept", required = true, paramType = "header", value =
                    BULK_ACCEPT, defaultValue = "application/fhir+json"),
            @ApiImplicitParam(name = "Prefer", required = true, paramType = "header", value =
                    BULK_PREFER, defaultValue = "respond-async")}
    )
    @ApiResponses(
            @ApiResponse(code = 202, message = "Export request has started", responseHeaders =
            @ResponseHeader(name = "Content-Location", description = "Absolute URL of an endpoint" +
                    " for subsequent status requests (polling location)",
                    response = String.class))
    )
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    @GetMapping("/Group/{contractNumber}/$export")
    public ResponseEntity<Void> exportPatientsWithContract(@ApiParam(value = "A contract number", required = true)
            @PathVariable @NotBlank String contractNumber,
            @ApiParam(value = BULK_EXPORT_TYPE, allowableValues = EOB, defaultValue = EOB)
            @RequestParam(required = false, name = "_type") String resourceTypes,
            @ApiParam(value = BULK_OUTPUT_FORMAT,
                    allowableValues = ALLOWABLE_OUTPUT_FORMATS, defaultValue = "application/fhir" +
                    "+ndjson"
            )
            @RequestParam(required = false, name = "_outputFormat") String outputFormat) {
        MDC.put(CONTRACT_LOG, contractNumber);
        log.info("Received request to export by contractNumber");

        checkIfInMaintenanceMode();

        checkIfCurrentUserCanAddJob();

        checkResourceTypesAndOutputFormat(resourceTypes, outputFormat);

        Job job = jobService.createJob(resourceTypes, ServletUriComponentsBuilder.fromCurrentRequest().toUriString(),
                contractNumber, outputFormat);

        logSuccessfulJobCreation(job);

        return returnStatusForJobCreation(job);
    }

    @ApiOperation(value = BULK_CANCEL,
            authorizations = {
                    @Authorization(value = "Authorization", scopes = {
                            @AuthorizationScope(description = "Cancel Export Job", scope = "Authorization") })
            })
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = JOB_CANCELLED_MSG),
            @ApiResponse(code = 404, message = JOB_NOT_FOUND_ERROR_MSG, response =
                    SwaggerConfig.OperationOutcome.class)}
    )
    @DeleteMapping(value = "/Job/{jobUuid}/$status")
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    public ResponseEntity<Void> deleteRequest(
            @ApiParam(value = "A job identifier", required = true)
            @PathVariable @NotBlank String jobUuid) {
        MDC.put(JOB_LOG, jobUuid);
        log.info("Request submitted to cancel job");

        jobService.cancelJob(jobUuid);

        log.info("Job successfully cancelled");

        return new ResponseEntity<>(null, null,
                HttpStatus.ACCEPTED);
    }

    @ApiOperation(value = "Returns a status of an export job.",
            authorizations = {
                    @Authorization(value = "Authorization", scopes = {
                            @AuthorizationScope(description = "Status of export job", scope = "Authorization") })
            })
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "The job is still in progress.", responseHeaders = {
                    @ResponseHeader(name = "X-Progress", description = "Completion percentage, " +
                            "such as 50%",
                            response = String.class),
                    @ResponseHeader(name = "Retry-After", description =
                            "A delay time in seconds before another status request will be " +
                                    "accepted.",
                            response = Integer.class)}, response = Void.class),
            @ApiResponse(code = 200, message = "The job is completed.", responseHeaders = {
                    @ResponseHeader(name = "Expires", description =
                            "Indicates when (an HTTP-date timestamp) the files " +
                                    "listed will no longer be available for access.",
                            response = String.class)}, response =
                    JobCompletedResponse.class),
            @ApiResponse(code = 404, message = "Job not found. " + GENERIC_FHIR_ERR_MSG, response =
                    SwaggerConfig.OperationOutcome.class)}
    )
    @GetMapping(value = "/Job/{jobUuid}/$status")
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity<JobCompletedResponse> getJobStatus(
            @ApiParam(value = "A job identifier", required = true) @PathVariable @NotBlank String jobUuid) {
        MDC.put(JOB_LOG, jobUuid);
        log.info("Request submitted to get job status");

        Job job = jobService.getAuthorizedJobByJobUuid(jobUuid);

        OffsetDateTime now = OffsetDateTime.now();

        if (job.getLastPollTime() != null && job.getLastPollTime().plusSeconds(retryAfterDelay).isAfter(now)) {
            log.error("User was polling too frequently");
            throw new TooManyRequestsException("You are polling too frequently");
        }

        job.setLastPollTime(now);
        jobService.updateJob(job);

        HttpHeaders responseHeaders = new HttpHeaders();
        switch (job.getStatus()) {
            case SUCCESSFUL:
                final ZonedDateTime jobExpiresUTC = ZonedDateTime.ofInstant(job.getExpiresAt().toInstant(), ZoneId.of("UTC"));
                responseHeaders.add("Expires", DateTimeFormatter.RFC_1123_DATE_TIME.format(jobExpiresUTC));

                final DateTimeType jobCompletedAt = new DateTimeType(job.getCompletedAt().toString());

                final JobCompletedResponse resp = new JobCompletedResponse();
                resp.setTransactionTime(jobCompletedAt.toHumanDisplay());
                resp.setRequest(job.getRequestUrl());
                resp.setRequiresAccessToken(true);
                resp.setOutput(job.getJobOutputs().stream().filter(o ->
                    !o .getError()).map(o ->
                        new JobCompletedResponse.Output(o.getFhirResourceType(), getUrlPath(job, o.getFilePath()))).collect(Collectors.toList()));
                resp.setError(job.getJobOutputs().stream().filter(o ->
                    o.getError()).map(o ->
                        new JobCompletedResponse.Output(o.getFhirResourceType(), getUrlPath(job, o.getFilePath()))).collect(Collectors.toList()));

                log.info("Job status completed successfully");

                return new ResponseEntity<>(resp, responseHeaders, HttpStatus.OK);
            case SUBMITTED:
            case IN_PROGRESS:
                responseHeaders.add("X-Progress", job.getProgress() + "% complete");
                responseHeaders.add("Retry-After", Integer.toString(retryAfterDelay));
                return new ResponseEntity<>(null, responseHeaders, HttpStatus.ACCEPTED);
            case FAILED:
                String jobFailedMessage = "Job failed while processing";
                log.error(jobFailedMessage);
                throw new JobProcessingException(jobFailedMessage);
            default:
                String unknownErrorMsg = "Unknown status of job";
                log.error(unknownErrorMsg);
                throw new RuntimeException(unknownErrorMsg);
        }
    }

    private String getUrlPath(Job job, String filePath) {
        String requestURIString = ServletUriComponentsBuilder.fromCurrentRequestUri().replacePath(API_PREFIX + FHIR_PREFIX + "/Job/" + job.getJobUuid()).toUriString();
        return requestURIString + "/file/" + filePath;
    }

    @ApiOperation(value = "Downloads a file produced by an export job.", response = String.class,
            produces = NDJSON_FIRE_CONTENT_TYPE + " or " + ZIPFORMAT,
            authorizations = {
                    @Authorization(value = "Authorization", scopes = {
                            @AuthorizationScope(description = "Downloads Export File", scope = "Authorization") })
            })
    @ApiImplicitParams(value = {
            @ApiImplicitParam(name = "Accept", required = false, paramType = "header", value =
                    NDJSON_FIRE_CONTENT_TYPE + " or " + ZIPFORMAT, defaultValue = NDJSON_FIRE_CONTENT_TYPE)}
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Returns the requested file as " +
                    NDJSON_FIRE_CONTENT_TYPE + " or " + ZIPFORMAT, responseHeaders = {
                    @ResponseHeader(name = "Content-Type", description =
                            "Content-Type header that matches the file format being delivered: " +
                                    NDJSON_FIRE_CONTENT_TYPE,
                            response = String.class)}, response =
                    String.class),
            @ApiResponse(code = 404, message =
                    "Job or file not found. " + GENERIC_FHIR_ERR_MSG, response =
                    SwaggerConfig.OperationOutcome.class)}
    )
    @ResponseStatus(value = HttpStatus.OK)
    @GetMapping(value = "/Job/{jobUuid}/file/{filename}")
    public ResponseEntity<Void> downloadFile(
            @ApiParam(value = "A job identifier", required = true) @PathVariable @NotBlank String jobUuid,
            @ApiParam(value = "A file name", required = true) @PathVariable @NotBlank String filename) throws IOException {
        MDC.put(JOB_LOG, jobUuid);
        MDC.put(FILE_LOG, filename);
        log.info("Request submitted to download file");

        Resource downloadResource = jobService.getResourceForJob(jobUuid, filename);

        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletResponse response = ((ServletRequestAttributes) requestAttributes).getResponse();

        log.info("Sending " + filename + " file to client");

        String mimeType = NDJSON_FIRE_CONTENT_TYPE;
        if (downloadResource.getFilename().endsWith("zip")) {
            mimeType = ZIPFORMAT;
        }
        response.setHeader(HttpHeaders.CONTENT_TYPE, mimeType);

        try (OutputStream out = response.getOutputStream(); FileInputStream in = new FileInputStream(downloadResource.getFile())) {
            IOUtils.copy(in, out);

            jobService.deleteFileForJob(downloadResource.getFile(), jobUuid);

            return new ResponseEntity<>(null, null, HttpStatus.OK);
        }
    }

    @ApiOperation(value = "A request for the FHIR capability statement", response = String.class,
            produces = "application/json",
            authorizations = {
                    @Authorization(value = "Authorization", scopes = {
                            @AuthorizationScope(description = "Returns the FHIR capability statement", scope = "Authorization") })
            })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Returns the FHIR capability statement", response =
                    String.class)}
    )
    @ResponseStatus(value = HttpStatus.OK)
    @GetMapping(value = "/metadata")
    public ResponseEntity<String> capabilityStatement() {
        CapabilityStatement capabilityStatement = new CapabilityStatement();
        String json = new Gson().toJson(capabilityStatement);
        return new ResponseEntity<>(json, null, HttpStatus.OK);
    }
}
