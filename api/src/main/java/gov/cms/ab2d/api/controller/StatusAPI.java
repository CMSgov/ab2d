package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.api.config.SwaggerConfig;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobOutput;
import gov.cms.ab2d.common.service.JobService;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.DateTimeType;
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
@RequestMapping(path = API_PREFIX + FHIR_PREFIX, produces = {"application/json", NDJSON_FIRE_CONTENT_TYPE})
@SuppressWarnings("PMD.TooManyStaticImports")
/**
 * The sole REST controller for AB2D's implementation of the FHIR Bulk Data API Status (both GET & DELETE).
 */
public class StatusAPI {

    @Value("${api.retry-after.delay}")
    private int retryAfterDelay;

    @Autowired
    private JobService jobService;

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

                final DateTimeType jobStartedAt = new DateTimeType(job.getCreatedAt().toString());

                final JobCompletedResponse resp = new JobCompletedResponse();
                resp.setTransactionTime(jobStartedAt.toHumanDisplay());
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
