package gov.cms.ab2d.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.ab2d.api.service.JobService;
import gov.cms.ab2d.api.util.Constants;
import gov.cms.ab2d.api.util.DateUtil;
import gov.cms.ab2d.domain.Job;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ResponseHeader;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.DateTimeType;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.constraints.NotBlank;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import static gov.cms.ab2d.api.util.Constants.API_PREFIX;
import static gov.cms.ab2d.api.util.DateUtil.convertLocalDateTimeToDate;

@Slf4j
@Api(value = "Bulk Data Access API", description =
        "API through which an authenticated and authorized PDP sponsor" +
                " may request a bulk-data export from a server, receive status information " +
                "regarding progress in the generation of the requested files, and retrieve these " +
                "files")
@RestController
@RequestMapping(path = API_PREFIX, produces = "application/json")
/**
 * The sole REST controller for AB2D's implementation of the FHIR Bulk Data API specification.
 */
public class BulkDataAccessAPI {

    // Since this is used in an annotation, it can't be derived from the Set, otherwise it will be an error
    private static final String ALLOWABLE_OUTPUT_FORMATS = "application/fhir+ndjson,application/ndjson,ndjson";

    private static final Set<String> ALLOWABLE_OUTPUT_FORMAT_SET = Set.of(ALLOWABLE_OUTPUT_FORMATS.split(","));

    private static final String RESOURCE_TYPE_VALUE = "ExplanationOfBenefits";

    public static final String JOB_NOT_FOUND_ERROR_MSG = "Job not found. " + Constants.GENERIC_FHIR_ERR_MSG;

    public static final String JOB_CANCELLED_MSG = "Job canceled";

    @Value("${api.retry-after.delay}")
    private int retryAfterDelay;

    @Autowired
    private JobService jobService;

    @ApiOperation(value = "Initiate Part A & B bulk claim export job")
    @ApiImplicitParams(
            @ApiImplicitParam(name = "Prefer", required = true, paramType = "header", value =
                    "respond-async"))
    @ApiResponses(
            @ApiResponse(code = 202, message = "Export request has started", responseHeaders =
            @ResponseHeader(name = "Content-Location", description = "URL to query job status",
                    response = String.class))
    )
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    @GetMapping("/Patient/$export")
    public ResponseEntity<Void> exportAllPatients(
            @ApiParam(value = "String of comma-delimited FHIR resource types. Only resources of " +
                    "the specified resource types(s) SHALL be included in the response.",
                    allowableValues = RESOURCE_TYPE_VALUE)
            @RequestParam(required = false, name = "_type") String resourceTypes,
            @ApiParam(value = "A FHIR instant. Resources will be included in the response if " +
                    "their state has changed after the supplied time.")
            @RequestParam(required = false, name = "_since") String since,
            @ApiParam(value = "The format for the requested bulk data files to be generated.",
                    allowableValues = ALLOWABLE_OUTPUT_FORMATS, defaultValue = "application/fhir" +
                    "+ndjson"
            )
            @RequestParam(required = false, name = "_outputFormat") String outputFormat) {

        if (resourceTypes != null && !resourceTypes.equals(RESOURCE_TYPE_VALUE)) {
            throw new InvalidUserInputException("_type must be " + RESOURCE_TYPE_VALUE);
        }
        if (outputFormat != null && !ALLOWABLE_OUTPUT_FORMAT_SET.contains(outputFormat)) {
            throw new InvalidUserInputException("An _outputFormat of " + outputFormat + " is not valid");
        }

        Job job = jobService.createJob(resourceTypes, ServletUriComponentsBuilder.fromCurrentRequest().toUriString());

        String statusURL = ServletUriComponentsBuilder.fromCurrentRequestUri().replacePath
                (String.format(API_PREFIX + "/Job/%s/$status", job.getJobID())).toUriString();
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("Content-Location", statusURL);

        return new ResponseEntity<>(null, responseHeaders,
                HttpStatus.ACCEPTED);
    }

    @ApiOperation(value = "Cancel a pending export job")
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = JOB_CANCELLED_MSG),
            @ApiResponse(code = 404, message = JOB_NOT_FOUND_ERROR_MSG)}
    )
    @DeleteMapping(value = "/Job/{jobId}/$status")
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    public ResponseEntity<String> deleteRequest(
            @ApiParam(value = "A job identifier", required = true)
            @PathVariable @NotBlank String jobId) {
        jobService.cancelJob(jobId);

        return new ResponseEntity<>(JOB_CANCELLED_MSG, null,
                HttpStatus.ACCEPTED);
    }

    @ApiOperation(value = "Returns a status of an export job.")
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "The job is still in progress.", responseHeaders =
            @ResponseHeader(name = "X-Progress", description = "Completion percentage, such as 50%",
                    response = String.class), response = Void.class),
            @ApiResponse(code = 200, message = "The job is completed.", response =
                    JobCompletedResponse.class),
            @ApiResponse(code = 404, message = "Job not found. " + Constants.GENERIC_FHIR_ERR_MSG)}
    )
    @GetMapping(value = "/Job/{jobId}/$status")
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity<JsonNode> getJobStatus(
            @ApiParam(value = "A job identifier", required = true) @PathVariable @NotBlank String jobId) {
        Job job = jobService.getJobByJobID(jobId);

        OffsetDateTime now = OffsetDateTime.now();

        if (job.getLastPollTime() != null && job.getLastPollTime().plusSeconds(retryAfterDelay).isAfter(now)) {
            throw new TooManyRequestsException("You are polling too frequently");
        }

        job.setLastPollTime(now);
        jobService.updateJob(job);

        HttpHeaders responseHeaders = new HttpHeaders();
        switch (job.getStatus()) {
            case SUCCESSFUL:
//                responseHeaders.add("Expires", DateUtil.formatLocalDateTimeAsUTC(job.getExpires());
                final String ldtFormatted = DateUtil.formatLocalDateTimeAsUTC(job.getExpires().toLocalDateTime());
                final String offsetDateTimeFormat = job.getExpires().toString();
                responseHeaders.add("Expires", ldtFormatted);
                JobCompletedResponse resp = new JobCompletedResponse();

                final DateTimeType oldDT = new DateTimeType(convertLocalDateTimeToDate(job.getCompletedAt().toLocalDateTime()));
                final DateTimeType newDT = new DateTimeType(job.getCompletedAt().toString());
                resp.setTransactionTime(newDT.toHumanDisplay());
                resp.setRequest(job.getRequestURL());
                resp.setRequiresAccessToken(true);
                resp.setOutput(job.getJobOutput().stream().filter(o -> !o.isError()).map(o -> new JobCompletedResponse.Output(o.getFhirResourceType(), o.getFilePath())).collect(Collectors.toList()));
                resp.setError(job.getJobOutput().stream().filter(o -> o.isError()).map(o -> new JobCompletedResponse.Output(o.getFhirResourceType(), o.getFilePath())).collect(Collectors.toList()));
                return new ResponseEntity<>(new ObjectMapper().valueToTree(resp), responseHeaders, HttpStatus.OK);
            case SUBMITTED:
                IN_PROGRESS:
                responseHeaders.add("X-Progress", job.getProgress() + "% complete");
                responseHeaders.add("Retry-After", Integer.toString(retryAfterDelay));
                return new ResponseEntity<>(null, responseHeaders, HttpStatus.ACCEPTED);
            case FAILED:
                throw new JobProcessingException("Job failed while processing");
            default:
                throw new RuntimeException("Unknown status of job");
        }
    }

    @ApiOperation(value = "Downloads a file produced by an export job.", response = String.class,
            produces = "application/fhir+ndjson")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Returns the requested file as " +
                    "application/fhir+ndjson.", response =
                    String.class),
            @ApiResponse(code = 404, message =
                    "Job or file not found. " + Constants.GENERIC_FHIR_ERR_MSG)}
    )
    @ResponseStatus(value = HttpStatus.OK)
    @GetMapping(value = "/Job/{jobId}/file/{filename}")
    public ResponseEntity<Resource> downloadFile(
            @ApiParam(value = "A job identifier", required = true) @PathVariable @NotBlank String jobId,
            @ApiParam(value = "A file name", required = true) @PathVariable @NotBlank String filename) throws IOException {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("X-Progress", "0%");
        return new ResponseEntity<>(null, responseHeaders, HttpStatus.ACCEPTED);
    }
}
