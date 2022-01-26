package gov.cms.ab2d.api.controller.common;

import gov.cms.ab2d.api.controller.JobCompletedResponse;
import gov.cms.ab2d.api.controller.JobProcessingException;
import gov.cms.ab2d.api.controller.TooManyRequestsException;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobOutput;
import gov.cms.ab2d.common.service.JobService;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.ApiResponseEvent;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static gov.cms.ab2d.api.controller.common.ApiText.X_PROG;
import static gov.cms.ab2d.common.util.Constants.ORGANIZATION;
import static gov.cms.ab2d.common.util.Constants.JOB_LOG;
import static gov.cms.ab2d.common.util.Constants.REQUEST_ID;
import static gov.cms.ab2d.common.util.Constants.FHIR_PREFIX;
import static org.springframework.http.HttpHeaders.EXPIRES;
import static org.springframework.http.HttpHeaders.RETRY_AFTER;

@Service
@Slf4j
public class StatusCommon {
    private final JobService jobService;
    private final LogManager eventLogger;
    private final int retryAfterDelay;

    StatusCommon(JobService jobService, LogManager eventLogger, @Value("${api.retry-after.delay}") int retryAfterDelay) {
        this.jobService = jobService;
        this.eventLogger = eventLogger;
        this.retryAfterDelay = retryAfterDelay;
    }

    public ResponseEntity throwFailedResponse(String msg) {
        log.error(msg);
        throw new JobProcessingException(msg);
    }

    public ResponseEntity<JobCompletedResponse> doStatus(String jobUuid, HttpServletRequest request, String apiPrefix) {
        MDC.put(JOB_LOG, jobUuid);
        log.info("Request submitted to get job status");

        Job job = jobService.getAuthorizedJobByJobUuidAndRole(jobUuid);

        if (pollingTooMuch(job)) {
            log.error("Client was polling too frequently");
            throw new TooManyRequestsException("You are polling too frequently");
        }

        updateLastPollTime(job);

        HttpHeaders responseHeaders = new HttpHeaders();
        switch (job.getStatus()) {
            case SUCCESSFUL:
                return getSuccessResponse(job, request, apiPrefix);
            case SUBMITTED:
            case IN_PROGRESS:
                responseHeaders.add(X_PROG, job.getProgress() + "% complete");
                responseHeaders.add(RETRY_AFTER, Integer.toString(retryAfterDelay));
                eventLogger.log(new ApiResponseEvent(MDC.get(ORGANIZATION), job.getJobUuid(), HttpStatus.ACCEPTED,
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

    private boolean pollingTooMuch(Job job) {
        return job.getLastPollTime() != null && job.getLastPollTime().plusSeconds(retryAfterDelay).isAfter(OffsetDateTime.now());
    }

    private void updateLastPollTime(Job job) {
        job.setLastPollTime(OffsetDateTime.now());
        jobService.updateJob(job);
    }

    private JobCompletedResponse getJobCompletedResonse(Job job, HttpServletRequest request, String apiPrefix) {

        final JobCompletedResponse resp = new JobCompletedResponse();

        final String jobStartedAt = job.getFhirVersion().getFhirTime(job.getCreatedAt());
        resp.setTransactionTime(jobStartedAt);

        resp.setRequest(job.getRequestUrl());

        resp.setRequiresAccessToken(true);

        resp.setOutput(job.getJobOutputs().stream().filter(o ->
                !o .getError()).map(o -> {
            List<JobCompletedResponse.FileMetadata> valueOutputs = generateValueOutputs(o);
            return new JobCompletedResponse.Output(o.getFhirResourceType(), getUrlPath(job, o.getFilePath(), request, apiPrefix), valueOutputs);
        }).collect(Collectors.toList()));

        resp.setError(job.getJobOutputs().stream().filter(o ->
                o.getError()).map(o -> {
            List<JobCompletedResponse.FileMetadata> valueOutputs = generateValueOutputs(o);
            return new JobCompletedResponse.Output(o.getFhirResourceType(), getUrlPath(job, o.getFilePath(), request, apiPrefix), valueOutputs);
        }).collect(Collectors.toList()));

        return resp;
    }
    private String getUrlPath(Job job, String filePath, HttpServletRequest request, String apiPrefix) {
        return Common.getUrl(apiPrefix + FHIR_PREFIX + "/Job/" + job.getJobUuid() + "/file/" + filePath, request);
    }

    private List<JobCompletedResponse.FileMetadata> generateValueOutputs(JobOutput o) {
        List<JobCompletedResponse.FileMetadata> valueOutputs = new ArrayList<>(2);
        valueOutputs.add(new JobCompletedResponse.FileMetadata(o.getChecksum()));
        valueOutputs.add(new JobCompletedResponse.FileMetadata(o.getFileLength()));
        return valueOutputs;
    }

    public ResponseEntity getSuccessResponse(Job job, HttpServletRequest request, String apiPrefix) {
        HttpHeaders responseHeaders = new HttpHeaders();
        final ZonedDateTime jobExpiresUTC = ZonedDateTime.ofInstant(job.getExpiresAt().toInstant(), ZoneId.of("UTC"));
        responseHeaders.add(EXPIRES, DateTimeFormatter.RFC_1123_DATE_TIME.format(jobExpiresUTC));
        final JobCompletedResponse resp = getJobCompletedResonse(job, request, apiPrefix);
        log.info("Job status completed successfully");
        eventLogger.log(new ApiResponseEvent(MDC.get(ORGANIZATION), job.getJobUuid(), HttpStatus.OK,
                "Job completed", null, (String) request.getAttribute(REQUEST_ID)));
        return new ResponseEntity<>(resp, responseHeaders, HttpStatus.OK);
    }

    public ResponseEntity cancelJob(String jobUuid, HttpServletRequest request) {
        MDC.put(JOB_LOG, jobUuid);
        log.info("Request submitted to cancel job");

        jobService.cancelJob(jobUuid);

        log.info("Job successfully cancelled");

        eventLogger.log(new ApiResponseEvent(MDC.get(ORGANIZATION), jobUuid, HttpStatus.ACCEPTED,
                "Job cancelled", null, (String) request.getAttribute(REQUEST_ID)));

        return new ResponseEntity<>(null, null,
                HttpStatus.ACCEPTED);
    }
}
