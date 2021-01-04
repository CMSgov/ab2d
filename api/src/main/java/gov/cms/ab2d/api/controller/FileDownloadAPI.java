package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.api.config.SwaggerConfig;
import gov.cms.ab2d.common.service.JobService;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.ApiResponseEvent;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotBlank;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import static gov.cms.ab2d.api.util.Constants.GENERIC_FHIR_ERR_MSG;
import static gov.cms.ab2d.common.service.JobService.ZIPFORMAT;
import static gov.cms.ab2d.common.util.Constants.*;

@Slf4j
@Api(value = "Bulk Data File Download API", description = "After creating a job, the API to download the generated bulk download files",
        tags = {"Download"})
@RestController
@RequestMapping(path = API_PREFIX + FHIR_PREFIX, produces = {"application/json", NDJSON_FIRE_CONTENT_TYPE})
public class FileDownloadAPI {
    @Autowired
    private JobService jobService;

    @Autowired
    private LogManager eventLogger;

    @ApiOperation(value = "Downloads a file produced by an export job.", response = String.class,
            produces = NDJSON_FIRE_CONTENT_TYPE,
            authorizations = {
                    @Authorization(value = "Authorization", scopes = {
                            @AuthorizationScope(description = "Downloads Export File", scope = "Authorization") })
            })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Returns the requested file as " +
                    NDJSON_FIRE_CONTENT_TYPE + " or " + ZIPFORMAT, responseHeaders = {
                    @ResponseHeader(name = "Content-Type", description =
                            "Content-Type header that matches the file format being delivered: " +
                                    NDJSON_FIRE_CONTENT_TYPE,
                            response = String.class)}, response =
                    String.class),
            @ApiResponse(code = 404, message =
                    "Job or file not found. " + GENERIC_FHIR_ERR_MSG, response =
                    SwaggerConfig.OperationOutcome.class)}
    )
    @ResponseStatus(value = HttpStatus.OK)
    @GetMapping(value = "/Job/{jobUuid}/file/{filename}", produces = { NDJSON_FIRE_CONTENT_TYPE })
    public ResponseEntity downloadFile(
            HttpServletRequest request,
            @ApiParam(value = "A job identifier", required = true) @PathVariable @NotBlank String jobUuid,
            @ApiParam(value = "A file name", required = true) @PathVariable @NotBlank String filename) throws IOException {
        MDC.put(JOB_LOG, jobUuid);
        MDC.put(FILE_LOG, filename);
        log.info("Request submitted to download file");

        Resource downloadResource = jobService.getResourceForJob(jobUuid, filename);

        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletResponse response = ((ServletRequestAttributes) requestAttributes).getResponse();

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
