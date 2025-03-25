package gov.cms.ab2d.api.controller.common;

import com.fasterxml.jackson.databind.JsonNode;
import gov.cms.ab2d.api.controller.ErrorHandler;
import gov.cms.ab2d.api.remote.JobClient;
import gov.cms.ab2d.common.service.PdpClientService;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.eventclient.events.ApiResponseEvent;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.slf4j.MDC;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


import static gov.cms.ab2d.common.util.Constants.FILE_LOG;
import static gov.cms.ab2d.common.util.Constants.JOB_LOG;
import static gov.cms.ab2d.common.util.Constants.FHIR_NDJSON_CONTENT_TYPE;
import static gov.cms.ab2d.common.util.Constants.ORGANIZATION;
import static gov.cms.ab2d.common.util.Constants.REQUEST_ID;

@Service
@AllArgsConstructor
@Slf4j
public class FileDownloadCommon {
    private final JobClient jobClient;
    private final SQSEventClient eventLogger;
    private final PdpClientService pdpClientService;
    private final ErrorHandler errorHandler;

    public ResponseEntity downloadFile(String jobUuid, String filename, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            return downloadFileInternal(jobUuid, filename, request, response);
        } catch (Exception e) {
            errorHandler.generateFHIRError(e, request, response);
            return null;
        }
    }

    private ResponseEntity downloadFileInternal(String jobUuid, String filename, HttpServletRequest request, HttpServletResponse response) throws IOException {
        MDC.put(JOB_LOG, jobUuid);
        MDC.put(FILE_LOG, filename);
        log.info("Request submitted to download file");

        Resource downloadResource = jobClient.getResourceForJob(jobUuid, filename,
                pdpClientService.getCurrentClient().getOrganization());

        log.info("Sending " + filename + " file to client");

        String fileDownloadName = downloadResource.getFile().getName();

        response.setHeader(HttpHeaders.CONTENT_TYPE, FHIR_NDJSON_CONTENT_TYPE);
        response.setHeader("Content-Disposition", "inline; swaggerDownload=\"attachment\"; filename=\"" + fileDownloadName + "\"");

        try (OutputStream out = response.getOutputStream(); FileInputStream in = new FileInputStream(downloadResource.getFile())) {
            IOUtils.copy(in, out);

            eventLogger.sendLogs(new ApiResponseEvent(MDC.get(ORGANIZATION), jobUuid, HttpStatus.OK, "File Download",
                    "File " + filename + " was downloaded", (String) request.getAttribute(REQUEST_ID)));
            jobClient.incrementDownload(downloadResource.getFile(), jobUuid);
            return new ResponseEntity<>(null, null, HttpStatus.OK);
        }
    }
}
