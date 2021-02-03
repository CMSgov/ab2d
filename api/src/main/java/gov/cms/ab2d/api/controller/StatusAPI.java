package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.api.config.SwaggerConfig;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobOutput;
import gov.cms.ab2d.common.service.JobService;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.ApiResponseEvent;
import gov.cms.ab2d.fhir.StatusUtils;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static gov.cms.ab2d.api.controller.BulkDataAccessAPI.JOB_CANCELLED_MSG;
import static gov.cms.ab2d.api.controller.BulkDataAccessAPI.JOB_NOT_FOUND_ERROR_MSG;
import static gov.cms.ab2d.api.util.Constants.GENERIC_FHIR_ERR_MSG;
import static gov.cms.ab2d.api.util.SwaggerConstants.BULK_CANCEL;
import static gov.cms.ab2d.common.util.Constants.*;

@Slf4j
@Api(value = "Bulk Data Access API", description = "API to determine the status of the job, the files to download " +
        "once the job is complete and an endpoint to cancel a job",
        tags = {"Status"})
@RestController
@RequestMapping(path = API_PREFIX + FHIR_PREFIX, produces = {"application/json"})
@SuppressWarnings("PMD.TooManyStaticImports")
/**
 * The sole REST controller for AB2D's implementation of the FHIR Bulk Data API Status (both GET & DELETE).
 */
public class StatusAPI {

    @Value("${api.retry-after.delay}")
    private int retryAfterDelay;

    @Autowired
    private JobService jobService;

    @Autowired
    private LogManager eventLogger;

    private boolean shouldReplaceWithHttps() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getRequest();
        return "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }

    String getUrl(String ending) {
        return shouldReplaceWithHttps() ?
                ServletUriComponentsBuilder.fromCurrentRequestUri().scheme("https").replacePath(ending).toUriString() :
                ServletUriComponentsBuilder.fromCurrentRequestUri().replacePath(ending).toUriString().replace(":80/", "/");
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
                            response = Integer.class)}),
            @ApiResponse(code = 200, message = "The job is completed.", responseHeaders = {
                    @ResponseHeader(name = "Expires", description =
                            "Indicates when (an HTTP-date timestamp) the files " +
                                    "listed will no longer be available for access.",
                            response = String.class)}, response =
                    JobCompletedResponse.class),
            @ApiResponse(code = 404, message = "Job not found. " + GENERIC_FHIR_ERR_MSG, response =
                    SwaggerConfig.OperationOutcome.class)}
    )
    @GetMapping(value = "/Job/{jobUuid}/$status", produces = "application/json")
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity<JobCompletedResponse> getJobStatus(HttpServletRequest request,
            @ApiParam(value = "A job identifier", required = true) @PathVariable @NotBlank String jobUuid) {
        MDC.put(JOB_LOG, jobUuid);
        log.info("Request submitted to get job status");

        Job job = jobService.getAuthorizedJobByJobUuidAndRole(jobUuid);

        if (pollingTooMuch(job)) {
            log.error("User was polling too frequently");
            throw new TooManyRequestsException("You are polling too frequently");
        }

        updateLastPollTime(job);

        HttpHeaders responseHeaders = new HttpHeaders();
        switch (job.getStatus()) {
            case SUCCESSFUL:
                return getSuccessResponse(job);
            case SUBMITTED:
            case IN_PROGRESS:
                responseHeaders.add("X-Progress", job.getProgress() + "% complete");
                responseHeaders.add("Retry-After", Integer.toString(retryAfterDelay));
                eventLogger.log(new ApiResponseEvent(MDC.get(USERNAME), job.getJobUuid(), HttpStatus.ACCEPTED,
                        "Job in progress", job.getProgress() + "% complete",
                        (String) request.getAttribute(REQUEST_ID)));
                return new ResponseEntity<>(null, responseHeaders, HttpStatus.ACCEPTED);
            case FAILED:
                throwFailedResponse("Job failed while processing");
            default:
                throwFailedResponse("Unknown status of job");
        }
        throw new JobProcessingException("Unknown error");
    }

    private ResponseEntity throwFailedResponse(String msg) {
        log.error(msg);
        throw new JobProcessingException(msg);
    }

    private ResponseEntity getSuccessResponse(Job job) {
        HttpHeaders responseHeaders = new HttpHeaders();
        final ZonedDateTime jobExpiresUTC = ZonedDateTime.ofInstant(job.getExpiresAt().toInstant(), ZoneId.of("UTC"));
        responseHeaders.add("Expires", DateTimeFormatter.RFC_1123_DATE_TIME.format(jobExpiresUTC));
        final JobCompletedResponse resp = getJobCompletedResonse(job);
        log.info("Job status completed successfully");
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getRequest();
        eventLogger.log(new ApiResponseEvent(MDC.get(USERNAME), job.getJobUuid(), HttpStatus.OK,
                "Job completed", null, (String) request.getAttribute(REQUEST_ID)));
        return new ResponseEntity<>(resp, responseHeaders, HttpStatus.OK);
    }

    private void updateLastPollTime(Job job) {
        job.setLastPollTime(OffsetDateTime.now());
        jobService.updateJob(job);
    }

    private boolean pollingTooMuch(Job job) {
        return job.getLastPollTime() != null && job.getLastPollTime().plusSeconds(retryAfterDelay).isAfter(OffsetDateTime.now());
    }

    private JobCompletedResponse getJobCompletedResonse(Job job) {

        final JobCompletedResponse resp = new JobCompletedResponse();

        final String jobStartedAt = StatusUtils.getFhirTime(job.getFhirVersion(), job.getCreatedAt());
        resp.setTransactionTime(jobStartedAt);

        resp.setRequest(job.getRequestUrl());

        resp.setRequiresAccessToken(true);

        resp.setOutput(job.getJobOutputs().stream().filter(o ->
                !o .getError()).map(o -> {
            List<JobCompletedResponse.FileMetadata> valueOutputs = generateValueOutputs(o);
            return new JobCompletedResponse.Output(o.getFhirResourceType(), getUrlPath(job, o.getFilePath()), valueOutputs);
        }).collect(Collectors.toList()));

        resp.setError(job.getJobOutputs().stream().filter(o ->
                o.getError()).map(o -> {
                    List<JobCompletedResponse.FileMetadata> valueOutputs = generateValueOutputs(o);
                    return new JobCompletedResponse.Output(o.getFhirResourceType(), getUrlPath(job, o.getFilePath()), valueOutputs);
        }).collect(Collectors.toList()));

        return resp;
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
    public ResponseEntity deleteRequest(
            HttpServletRequest request,
            @ApiParam(value = "A job identifier", required = true)
            @PathVariable @NotBlank String jobUuid) {
        MDC.put(JOB_LOG, jobUuid);
        log.info("Request submitted to cancel job");

        jobService.cancelJob(jobUuid);

        log.info("Job successfully cancelled");

        eventLogger.log(new ApiResponseEvent(MDC.get(USERNAME), jobUuid, HttpStatus.ACCEPTED,
                "Job cancelled", null, (String) request.getAttribute(REQUEST_ID)));

        return new ResponseEntity<>(null, null,
                HttpStatus.ACCEPTED);
    }

    private List<JobCompletedResponse.FileMetadata> generateValueOutputs(JobOutput o) {
        List<JobCompletedResponse.FileMetadata> valueOutputs = new ArrayList<>(2);
        valueOutputs.add(new JobCompletedResponse.FileMetadata(o.getChecksum()));
        valueOutputs.add(new JobCompletedResponse.FileMetadata(o.getFileLength()));
        return valueOutputs;
    }

    private String getUrlPath(Job job, String filePath) {
        return getUrl(API_PREFIX + FHIR_PREFIX + "/Job/" + job.getJobUuid() + "/file/" + filePath);
    }
}
