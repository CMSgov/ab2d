package gov.cms.ab2d.api.controller.v2;

import gov.cms.ab2d.api.config.SwaggerConfig;
import gov.cms.ab2d.api.controller.common.FileDownloadCommon;
import io.swagger.annotations.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;
import java.io.IOException;

import static gov.cms.ab2d.api.controller.common.ApiText.BULK_DNLD;
import static gov.cms.ab2d.api.controller.common.ApiText.BULK_DNLD_DSC;
import static gov.cms.ab2d.api.controller.common.ApiText.DOWNLOAD_DESC;
import static gov.cms.ab2d.api.controller.common.ApiText.JSON;
import static gov.cms.ab2d.api.controller.common.ApiText.AUTH;
import static gov.cms.ab2d.api.controller.common.ApiText.DOWNLOAD;
import static gov.cms.ab2d.api.controller.common.ApiText.DNLD_DESC;
import static gov.cms.ab2d.api.controller.common.ApiText.CONTENT_TYPE;
import static gov.cms.ab2d.api.controller.common.ApiText.CONTENT_TYPE_DESC;
import static gov.cms.ab2d.api.controller.common.ApiText.NOT_FOUND;
import static gov.cms.ab2d.api.controller.common.ApiText.JOB_ID;
import static gov.cms.ab2d.api.controller.common.ApiText.FILE_NAME;

import static gov.cms.ab2d.api.util.Constants.GENERIC_FHIR_ERR_MSG;

import static gov.cms.ab2d.common.util.Constants.API_PREFIX_V2;
import static gov.cms.ab2d.common.util.Constants.FHIR_PREFIX;
import static gov.cms.ab2d.common.util.Constants.NDJSON_FIRE_CONTENT_TYPE;

@AllArgsConstructor
@Slf4j
@Api(value = BULK_DNLD, description = BULK_DNLD_DSC, tags = {"Download"})
@RestController
@RequestMapping(path = API_PREFIX_V2 + FHIR_PREFIX, produces = {JSON, NDJSON_FIRE_CONTENT_TYPE})
@SuppressWarnings("PMD.TooManyStaticImports")
public class FileDownloadAPIV2 {
    private FileDownloadCommon fileDownloadCommon;

    @ApiOperation(value = DOWNLOAD_DESC, response = String.class, produces = NDJSON_FIRE_CONTENT_TYPE,
            authorizations = { @Authorization(value = AUTH, scopes = { @AuthorizationScope(description = DOWNLOAD, scope = AUTH) }) })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = DNLD_DESC, responseHeaders = {
                    @ResponseHeader(name = CONTENT_TYPE, description = CONTENT_TYPE_DESC + NDJSON_FIRE_CONTENT_TYPE,
                            response = String.class)}, response = String.class),
            @ApiResponse(code = 404, message = NOT_FOUND + GENERIC_FHIR_ERR_MSG, response = SwaggerConfig.OperationOutcome.class)}
    )
    @ResponseStatus(value = HttpStatus.OK)
    @GetMapping(value = "/Job/{jobUuid}/file/{filename}", produces = { NDJSON_FIRE_CONTENT_TYPE })
    public ResponseEntity downloadFile(HttpServletRequest request,
            @ApiParam(value = JOB_ID, required = true) @PathVariable @NotBlank String jobUuid,
            @ApiParam(value = FILE_NAME, required = true) @PathVariable @NotBlank String filename) throws IOException {

        return fileDownloadCommon.downloadFile(jobUuid, filename, request);
    }
}
