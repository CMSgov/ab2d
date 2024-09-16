package gov.cms.ab2d.api.controller.common;

import gov.cms.ab2d.api.config.OpenAPIConfig;
import gov.cms.ab2d.api.controller.JobCompletedResponse;
import gov.cms.ab2d.api.controller.JobProcessingException;
import gov.cms.ab2d.api.controller.TooManyRequestsException;
import gov.cms.ab2d.api.remote.JobClient;
import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.model.TooFrequentInvocations;
import gov.cms.ab2d.common.service.PdpClientService;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.eventclient.events.ApiResponseEvent;
import gov.cms.ab2d.job.dto.JobPollResult;
import gov.cms.ab2d.job.model.JobOutput;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import static gov.cms.ab2d.api.controller.common.ApiText.X_PROG;
import static gov.cms.ab2d.common.util.Constants.FHIR_PREFIX;
import static gov.cms.ab2d.common.util.Constants.JOB_LOG;
import static gov.cms.ab2d.common.util.Constants.ORGANIZATION;
import static gov.cms.ab2d.common.util.Constants.REQUEST_ID;
import static org.springframework.http.HttpHeaders.EXPIRES;
import static org.springframework.http.HttpHeaders.RETRY_AFTER;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Service
@Slf4j
public class StatusCommon {
    private final PdpClientService pdpClientService;
    private final JobClient jobClient;
    private final SQSEventClient eventLogger;
    private final int retryAfterDelay;
    private final OpenAPIConfig openApi;

    StatusCommon(PdpClientService pdpClientService, JobClient jobClient,
                 SQSEventClient eventLogger, @Value("${api.retry-after.delay}") int retryAfterDelay) {
        this.pdpClientService = pdpClientService;
        this.jobClient = jobClient;
        this.eventLogger = eventLogger;
        this.retryAfterDelay = retryAfterDelay;

        this.openApi = new OpenAPIConfig();
    }

    public void throwFailedResponse(String msg) {
        log.error(msg);
        throw new JobProcessingException(msg);
    }

    public ResponseEntity doStatus(String jobUuid, HttpServletRequest request, String apiPrefix) {
        MDC.put(JOB_LOG, jobUuid);
        log.info("Request submitted to get job status");

        PdpClient pdpClient = pdpClientService.getCurrentClient();
        JobPollResult jobPollResult;
        try {
            jobPollResult = jobClient.poll(pdpClient.isAdmin(), jobUuid, pdpClient.getOrganization(), retryAfterDelay);
        } catch (TooFrequentInvocations tfi) {
            log.error("Client was polling too frequently");
            throw new TooManyRequestsException("You are polling too frequently");
        }

        HttpHeaders responseHeaders = new HttpHeaders();
        switch (jobPollResult.getStatus()) {
            case SUCCESSFUL:
                return getSuccessResponse(jobPollResult, jobUuid, request, apiPrefix);
            case SUBMITTED:
            case IN_PROGRESS:
                responseHeaders.add(X_PROG, jobPollResult.getProgress() + "% complete");
                responseHeaders.add(RETRY_AFTER, Integer.toString(retryAfterDelay));
                eventLogger.sendLogs(new ApiResponseEvent(MDC.get(ORGANIZATION), jobUuid, HttpStatus.ACCEPTED,
                        "Job in progress", jobPollResult.getProgress() + "% complete",
                        (String) request.getAttribute(REQUEST_ID)));
                return new ResponseEntity<>(null, responseHeaders, HttpStatus.ACCEPTED);
            case CANCELLED:
                return getCanceledResponse(jobPollResult, jobUuid, request);
            case FAILED:
                throwFailedResponse("Job failed while processing");
                break;
            default:
                throwFailedResponse("Unknown status of job");
        }
        throw new JobProcessingException("Unknown error");
    }

    protected JobCompletedResponse getJobCompletedResponse(JobPollResult jobPollResult, String jobUuid,
                                                         HttpServletRequest request, String apiPrefix) {

        final JobCompletedResponse resp = new JobCompletedResponse();

        final String jobStartedAt = jobPollResult.getTransactionTime();
        resp.setTransactionTime(jobStartedAt);

        resp.setRequest(jobPollResult.getRequestUrl());

        resp.setRequiresAccessToken(true);

        resp.setOutput(jobPollResult.getJobOutputs().stream().filter(o ->
                !o.getError()).map(o -> {
            List<JobCompletedResponse.FileMetadata> valueOutputs = generateValueOutputs(o);
            return new JobCompletedResponse.Output(o.getFhirResourceType(),
                    getUrlPath(jobUuid, o.getFilePath(), request, apiPrefix), valueOutputs);
        }).toList());

        resp.setError(jobPollResult.getJobOutputs().stream().filter(JobOutput::getError).map(o -> {
            List<JobCompletedResponse.FileMetadata> valueOutputs = generateValueOutputs(o);
            return new JobCompletedResponse.Output(o.getFhirResourceType(),
                    getUrlPath(jobUuid, o.getFilePath(), request, apiPrefix), valueOutputs);
        }).toList());

        return resp;
    }

    private ResponseEntity getCanceledResponse(JobPollResult jobPollResult, String jobUuid, HttpServletRequest request) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(APPLICATION_JSON);

        OpenAPIConfig.OperationOutcome outcome = openApi.new OperationOutcome();
        OpenAPIConfig.Details details = new OpenAPIConfig.Details();
        details.setText("Job is canceled.");

        OpenAPIConfig.Issue issue = new OpenAPIConfig.Issue();
        issue.setDetails(details);
        issue.setCode("deleted");
        issue.setSeverity("information");

        List<OpenAPIConfig.Issue> issuesList = new ArrayList<>();
        issuesList.add(issue);
        outcome.setIssue(issuesList);

        eventLogger.sendLogs(new ApiResponseEvent(MDC.get(ORGANIZATION), jobUuid, HttpStatus.NOT_FOUND,
                "Job was previously canceled", null, (String) request.getAttribute(REQUEST_ID)));

        return new ResponseEntity<OpenAPIConfig.OperationOutcome>(outcome, responseHeaders, HttpStatus.NOT_FOUND);
    }

    private String getUrlPath(String jobUuid, String filePath, HttpServletRequest request, String apiPrefix) {
        return Common.getUrl(apiPrefix + FHIR_PREFIX + "/Job/" + jobUuid + "/file/" + filePath, request);
    }

    private List<JobCompletedResponse.FileMetadata> generateValueOutputs(JobOutput o) {
        List<JobCompletedResponse.FileMetadata> valueOutputs = new ArrayList<>(2);
        valueOutputs.add(new JobCompletedResponse.FileMetadata(o.getChecksum()));
        valueOutputs.add(new JobCompletedResponse.FileMetadata(o.getFileLength()));
        return valueOutputs;
    }

    private ResponseEntity getSuccessResponse(JobPollResult jobPollResult, String jobUuid,
                                              HttpServletRequest request, String apiPrefix) {
        HttpHeaders responseHeaders = new HttpHeaders();
        final ZonedDateTime jobExpiresUTC =
                ZonedDateTime.ofInstant(jobPollResult.getExpiresAt().toInstant(), ZoneId.of("UTC"));
        responseHeaders.add(EXPIRES, DateTimeFormatter.RFC_1123_DATE_TIME.format(jobExpiresUTC));
        final JobCompletedResponse resp = getJobCompletedResponse(jobPollResult, jobUuid, request, apiPrefix);
        log.info("Job status completed successfully");
        eventLogger.sendLogs(new ApiResponseEvent(MDC.get(ORGANIZATION), jobUuid, HttpStatus.OK,
                "Job completed", null, (String) request.getAttribute(REQUEST_ID)));
        return new ResponseEntity<>(resp, responseHeaders, HttpStatus.OK);
    }

    public ResponseEntity cancelJob(String jobUuid, HttpServletRequest request) {
        MDC.put(JOB_LOG, jobUuid);
        log.info("Request submitted to cancel job");

        jobClient.cancelJob(jobUuid, pdpClientService.getCurrentClient().getOrganization());

        log.info("Job successfully cancelled");

        eventLogger.sendLogs(new ApiResponseEvent(MDC.get(ORGANIZATION), jobUuid, HttpStatus.ACCEPTED,
                "Job cancelled", null, (String) request.getAttribute(REQUEST_ID)));

        return new ResponseEntity<>(null, null,
                HttpStatus.ACCEPTED);
    }
}
