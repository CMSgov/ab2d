package gov.cms.ab2d.api.controller.common;

import gov.cms.ab2d.api.remote.JobClient;
import gov.cms.ab2d.common.service.PdpClientService;
import gov.cms.ab2d.common.util.Constants;
import gov.cms.ab2d.common.util.GzipCompressUtils;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.eventclient.events.ApiResponseEvent;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.slf4j.MDC;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import static gov.cms.ab2d.api.controller.common.FileDownloadCommon.Encoding.GZIP_COMPRESSED;
import static gov.cms.ab2d.api.controller.common.FileDownloadCommon.Encoding.UNCOMPRESSED;
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

    enum Encoding {
        UNCOMPRESSED,
        GZIP_COMPRESSED;
    }

    public ResponseEntity<String> downloadFile(String jobUuid, String filename, HttpServletRequest request, HttpServletResponse response) throws IOException {
        MDC.put(JOB_LOG, jobUuid);
        MDC.put(FILE_LOG, filename);
        log.info("Request submitted to download file");

        final Resource downloadResource = getDownloadResource(jobUuid, filename);

        log.info("Sending " + filename + " file to client");

        response.setHeader(HttpHeaders.CONTENT_TYPE, FHIR_NDJSON_CONTENT_TYPE);

        try (OutputStream out = response.getOutputStream();
            FileInputStream in = new FileInputStream(downloadResource.getFile())) {

            // set headers before writing to response stream
            final Encoding fileEncoding = getFileEncoding(downloadResource);
            final Encoding requestedEncoding = getRequestedEncoding(request);
            if (requestedEncoding == GZIP_COMPRESSED) {
                response.setHeader("Content-Encoding", Constants.GZIP_ENCODING);
            }
            final String fileDownloadName = getDownloadFilename(downloadResource, requestedEncoding);
            response.setHeader("Content-Disposition", "inline; swaggerDownload=\"attachment\"; filename=\"" + fileDownloadName + "\"");

            // write to response stream, compressing or decompressing file contents as needed
            if (requestedEncoding == fileEncoding) {
                IOUtils.copy(in, out);
            }
            else if (fileEncoding == GZIP_COMPRESSED && requestedEncoding == UNCOMPRESSED) {
                GzipCompressUtils.decompress(in, response.getOutputStream());
            }
            else if (fileEncoding == UNCOMPRESSED && requestedEncoding == GZIP_COMPRESSED) {
                GzipCompressUtils.compress(in, response.getOutputStream());
            }

            eventLogger.sendLogs(new ApiResponseEvent(MDC.get(ORGANIZATION), jobUuid, HttpStatus.OK, "File Download",
                    "File " + filename + " was downloaded", (String) request.getAttribute(REQUEST_ID)));
            jobClient.incrementDownload(downloadResource.getFile(), jobUuid);
            return new ResponseEntity<>(null, null, HttpStatus.OK);
        }
    }

    Resource getDownloadResource(String jobUuid, String filename) throws IOException {
        val organization = pdpClientService.getCurrentClient().getOrganization();
        try {
            // look for compressed file
            return jobClient.getResourceForJob(jobUuid, filename + ".gz", organization);
        }
        catch (RuntimeException e) {
            // look for uncompressed file
            // allow this exception to be thrown to caller (for consistency with current behavior)
            return jobClient.getResourceForJob(jobUuid, filename, organization);
        }
    }

    static String getDownloadFilename(
            Resource downloadResource,
            Encoding requestedEncoding) throws IOException {

        final Encoding fileEncoding = getFileEncoding(downloadResource);
        final String filename = downloadResource.getFile().getName();
        if (requestedEncoding == fileEncoding) {
            return filename;
        }
        else if (fileEncoding == GZIP_COMPRESSED && requestedEncoding == UNCOMPRESSED) {
            return filename.replace(".gz", "");
        }
        else {
            return filename + ".gz";
        }
    }

    static Encoding getFileEncoding(Resource resource) throws IOException {
        if (resource.getFile().getName().endsWith(".gz")) {
            return GZIP_COMPRESSED;
        }
        return UNCOMPRESSED;
    }

    // determine optional encoding requested by user, defaulting to uncompressed if not provided
    static Encoding getRequestedEncoding(HttpServletRequest request) {
        val values = request.getHeaders("Accept-Encoding");
        if (values != null) {
            while (values.hasMoreElements()) {
                val header = values.nextElement();
                if (header.equalsIgnoreCase(Constants.GZIP_ENCODING)) {
                    return GZIP_COMPRESSED;
                }
            }
        }

        return UNCOMPRESSED;
    }
}
