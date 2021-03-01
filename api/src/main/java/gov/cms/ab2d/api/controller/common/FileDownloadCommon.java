package gov.cms.ab2d.api.controller.common;

import gov.cms.ab2d.common.service.JobService;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.ApiResponseEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.slf4j.MDC;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import static gov.cms.ab2d.common.service.JobService.ZIPFORMAT;
import static gov.cms.ab2d.common.util.Constants.*;

@Service
@AllArgsConstructor
@Slf4j
public class FileDownloadCommon {
    private final JobService jobService;
    private final LogManager eventLogger;

    public ResponseEntity downloadFile(String jobUuid, String filename, HttpServletRequest request, HttpServletResponse response) throws IOException {
        MDC.put(JOB_LOG, jobUuid);
        MDC.put(FILE_LOG, filename);
        log.info("Request submitted to download file");

        Resource downloadResource = jobService.getResourceForJob(jobUuid, filename);

        log.info("Sending " + filename + " file to client");

        String mimeType = NDJSON_FIRE_CONTENT_TYPE;
        if (downloadResource.getFilename().endsWith("zip")) {
            mimeType = ZIPFORMAT;
        }
        response.setHeader(HttpHeaders.CONTENT_TYPE, mimeType);

        try (OutputStream out = response.getOutputStream(); FileInputStream in = new FileInputStream(downloadResource.getFile())) {
            IOUtils.copy(in, out);

            eventLogger.log(new ApiResponseEvent(MDC.get(USERNAME), jobUuid, HttpStatus.OK, "File Download",
                    "File " + filename + " was downloaded", (String) request.getAttribute(REQUEST_ID)));

            jobService.deleteFileForJob(downloadResource.getFile(), jobUuid);

            return new ResponseEntity<>(null, null, HttpStatus.OK);
        }
    }
}
