package gov.cms.ab2d.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.ab2d.api.config.SwaggerConfig;
import gov.cms.ab2d.common.service.JobService;
import gov.cms.ab2d.common.model.Job;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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
import static gov.cms.ab2d.api.util.Constants.API_PREFIX;
import static gov.cms.ab2d.api.util.Constants.FHIR_PREFIX;
import static gov.cms.ab2d.common.util.Constants.*;

@Slf4j
@Api(value = "Bulk Data Access API", description =
        "API through which an authenticated and authorized PDP sponsor" +
                " may request a bulk-data export from a server, receive status information " +
                "regarding progress in the generation of the requested files, and retrieve these " +
                "files")
@RestController
@RequestMapping(path = API_PREFIX + FHIR_PREFIX, produces = "application/json")
/**
 * The sole REST controller for AB2D's implementation of the FHIR Bulk Data API specification.
 */
public class BulkDataAccessAPI {

    // Since this is used in an annotation, it can't be derived from the Set, otherwise it will be an error
    private static final String ALLOWABLE_OUTPUT_FORMATS = "application/fhir+ndjson,application/ndjson,ndjson";

    private static final Set<String> ALLOWABLE_OUTPUT_FORMAT_SET = Set.of(ALLOWABLE_OUTPUT_FORMATS.split(","));

    private static final String RESOURCE_TYPE_VALUE = "ExplanationOfBenefits";

    public static final String JOB_NOT_FOUND_ERROR_MSG = "Job not found. " + GENERIC_FHIR_ERR_MSG;

    public static final String JOB_CANCELLED_MSG = "Job canceled";

    @Value("${api.retry-after.delay}")
    private int retryAfterDelay;

    @Autowired
    private JobService jobService;

    @ApiOperation(value = "Initiate Part A & B bulk claim export job")
    @ApiImplicitParams(value = {
            @ApiImplicitParam(name = "Accept", required = true, paramType = "header", value =
                    "application/fhir+json"),
            @ApiImplicitParam(name = "Prefer", required = true, paramType = "header", value =
                    "respond-async")}
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
            @ApiParam(value = "String of comma-delimited FHIR resource types. Only resources of " +
                    "the specified resource types(s) SHALL be included in the response.",
                    allowableValues = RESOURCE_TYPE_VALUE)
            @RequestParam(required = false, name = "_type") String resourceTypes,
            @ApiParam(value = "The format for the requested bulk data files to be generated.",
                    allowableValues = ALLOWABLE_OUTPUT_FORMATS, defaultValue = "application/fhir" +
                    "+ndjson"
            )
            @RequestParam(required = false, name = "_outputFormat") String outputFormat) {
        log.info("Received request to export");

        if(jobService.checkIfCurrentUserHasActiveJob()) {
            String errorMsg = "User already has an active or submitted job";
            log.error(errorMsg);
            throw new TooManyRequestsException(errorMsg);
        }

        checkResourceTypesAndOutputFormat(resourceTypes, outputFormat);

        Job job = jobService.createJob(resourceTypes, ServletUriComponentsBuilder.fromCurrentRequest().toUriString());

        logSuccessfulJobCreation(job);

        return returnStatusForJobCreation(job);
    }

    private void checkResourceTypesAndOutputFormat(String resourceTypes, String outputFormat) {
        if (resourceTypes != null && !resourceTypes.equals(RESOURCE_TYPE_VALUE)) {
            log.error("Received invalid resourceTypes of {}", resourceTypes);
            throw new InvalidUserInputException("_type must be " + RESOURCE_TYPE_VALUE);
        }
        if (outputFormat != null && !ALLOWABLE_OUTPUT_FORMAT_SET.contains(outputFormat)) {
            log.error("Received _outputFormat {}, which is not valid", outputFormat);
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

    @ApiOperation(value = "Initiate Part A & B bulk claim export job for a given contract number")
    @ApiImplicitParams(value = {
            @ApiImplicitParam(name = "Accept", required = true, paramType = "header", value =
                    "application/fhir+json"),
            @ApiImplicitParam(name = "Prefer", required = true, paramType = "header", value =
                    "respond-async")}
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
            @ApiParam(value = "String of comma-delimited FHIR resource types. Only resources of " +
                    "the specified resource types(s) SHALL be included in the response.",
                    allowableValues = RESOURCE_TYPE_VALUE)
            @RequestParam(required = false, name = "_type") String resourceTypes,
            @ApiParam(value = "The format for the requested bulk data files to be generated.",
                    allowableValues = ALLOWABLE_OUTPUT_FORMATS, defaultValue = "application/fhir" +
                    "+ndjson"
            )
            @RequestParam(required = false, name = "_outputFormat") String outputFormat) {
        MDC.put(CONTRACT_LOG, contractNumber);
        log.info("Received request to export by contractNumber");

        if(jobService.checkIfCurrentUserHasActiveJobForContractNumber(contractNumber)) {
            log.error("User already has an active or submitted job for the contract number {}", contractNumber);
            throw new TooManyRequestsException("User already has an active or submitted job for the contract number " + contractNumber);
        }

        checkResourceTypesAndOutputFormat(resourceTypes, outputFormat);

        Job job = jobService.createJob(resourceTypes, ServletUriComponentsBuilder.fromCurrentRequest().toUriString(), contractNumber);

        logSuccessfulJobCreation(job);

        return returnStatusForJobCreation(job);
    }

    @ApiOperation(value = "Cancel a pending export job")
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

    @ApiOperation(value = "Returns a status of an export job.")
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
    public ResponseEntity<JsonNode> getJobStatus(
            @ApiParam(value = "A job identifier", required = true) @PathVariable @NotBlank String jobUuid) {
        MDC.put(JOB_LOG, jobUuid);
        log.info("Request submitted to get job status");

        Job job = jobService.getJobByJobUuid(jobUuid);

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
                resp.setOutput(job.getJobOutputs().stream().filter(o -> !o.isError()).map(o -> new JobCompletedResponse.Output(o.getFhirResourceType(), getUrlPath(job, o.getFilePath()))).collect(Collectors.toList()));
                resp.setError(job.getJobOutputs().stream().filter(o -> o.isError()).map(o -> new JobCompletedResponse.Output(o.getFhirResourceType(), getUrlPath(job, o.getFilePath()))).collect(Collectors.toList()));

                log.info("Job status completed successfully");

                return new ResponseEntity<>(new ObjectMapper().valueToTree(resp), responseHeaders, HttpStatus.OK);
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
            produces = "application/fhir+ndjson")
    @ApiImplicitParams(value = {
            @ApiImplicitParam(name = "Accept", required = false, paramType = "header", value =
                    "application/fhir+json")}
    )
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Returns the requested file as " +
                    "application/fhir+ndjson.", responseHeaders = {
                    @ResponseHeader(name = "Content-Type", description =
                            "Content-Type header that matches the file format being delivered: " +
                                    "application/fhir+ndjson",
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

        log.info("Sending file to client");

        try (OutputStream out = response.getOutputStream(); FileInputStream in = new FileInputStream(downloadResource.getFile())) {
            IOUtils.copy(in, out);

            jobService.deleteFileForJob(downloadResource.getFile());

            response.setHeader(HttpHeaders.CONTENT_TYPE, NDJSON_FIRE_CONTENT_TYPE);

            return new ResponseEntity<>(null, null, HttpStatus.OK);
        }
    }
}
