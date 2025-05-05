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

import gov.cms.ab2d.job.service.JobOutputMissingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
            final String fileDownloadName = getSwaggerDownloadFilename(downloadResource);
            response.setHeader("Content-Disposition", "inline; swaggerDownload=\"attachment\"; filename=\"" + fileDownloadName + "\"");

            // write to response stream, compressing or decompressing file contents depending on 'Accept-Encoding' header
            if (requestedEncoding == fileEncoding) {
                IOUtils.copy(in, out);
            } else if (fileEncoding == GZIP_COMPRESSED && requestedEncoding == UNCOMPRESSED) {
                GzipCompressUtils.decompress(in, response.getOutputStream());
            } else if (fileEncoding == UNCOMPRESSED && requestedEncoding == GZIP_COMPRESSED) {
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
        } catch (JobOutputMissingException e) {
            // compressed file is found but file has either (a) expired or (b) been downloaded the max number of times
            throw e;
        } catch (RuntimeException e) {
            // look for uncompressed file
            // allow this exception to be thrown to caller (for consistency with current behavior)
            return jobClient.getResourceForJob(jobUuid, filename, organization);
        }
    }

    // In Swagger UI, omit ".gz" file extension because browsers implicitly send "accept-encoding:gzip" header
    // and automatically decompress file when "Download file" is clicked
    static String getSwaggerDownloadFilename(Resource downloadResource) throws IOException {
        return downloadResource.getFile().getName().replace(".gz", "");
    }

    static Encoding getFileEncoding(Resource resource) throws IOException {
        if (resource.getFile().getName().endsWith(".gz")) {
            return GZIP_COMPRESSED;
        }
        return UNCOMPRESSED;
    }

    // determine optional encoding requested by user, defaulting to uncompressed if not provided
    static Encoding getRequestedEncoding(HttpServletRequest request) {
        val headers = request.getHeaders("Accept-Encoding");
        if (headers != null) {
            while (headers.hasMoreElements()) {
                val values = headers.nextElement().split(",");
                for (String value : values) {
                    if (value.trim().equalsIgnoreCase(Constants.GZIP_ENCODING)) {
                        return GZIP_COMPRESSED;
                    }
                }
            }
        }

        return UNCOMPRESSED;
    }
}
