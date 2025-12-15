package gov.cms.ab2d.api.controller.v3;

import gov.cms.ab2d.api.controller.ErrorHandler;
import gov.cms.ab2d.api.controller.common.FileDownloadCommon;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

import static gov.cms.ab2d.api.controller.common.ApiText.*;
import static gov.cms.ab2d.api.controller.common.FileDownloadCommon.sanitizeFilename;
import static gov.cms.ab2d.api.controller.common.FileDownloadCommon.sanitizeJobUuid;
import static gov.cms.ab2d.api.util.Constants.GENERIC_FHIR_ERR_MSG;
import static gov.cms.ab2d.common.util.Constants.*;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

@AllArgsConstructor
@Slf4j
@Tag(name = "Download", description = BULK_DNLD_DSC)
@RestController
@ConditionalOnExpression("${v3.controller.enabled:true}")
@RequestMapping(path = API_PREFIX_V3 + FHIR_PREFIX, produces = {FHIR_NDJSON_CONTENT_TYPE, FHIR_JSON_CONTENT_TYPE})
public class FileDownloadAPIV3 {
    private FileDownloadCommon fileDownloadCommon;
    private ErrorHandler errorHandler;

    @Operation(summary = DOWNLOAD_DESC)
    @Parameters(value = {
        @Parameter(name = "jobUuid", description = JOB_ID, required = true),
        @Parameter(name = "filename", description = FILE_NAME, required = true)
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = DNLD_DESC,
                headers = {@Header(name = CONTENT_TYPE, description = CONTENT_TYPE_DESC + FHIR_NDJSON_CONTENT_TYPE)},
                content = @Content(mediaType = FHIR_NDJSON_CONTENT_TYPE)
            ),
            @ApiResponse(responseCode = "404", description = NOT_FOUND + GENERIC_FHIR_ERR_MSG, content =
                @Content(mediaType = FHIR_JSON_CONTENT_TYPE, schema = @Schema(ref = "#/components/schemas/OperationOutcome"))
            )
     }
    )
    @ResponseStatus(value = HttpStatus.OK)
    @GetMapping(value = "/Job/{jobUuid}/file/{filename}", produces = {FHIR_NDJSON_CONTENT_TYPE})
    public ResponseEntity downloadFile(HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable @NotBlank String jobUuid,
            @PathVariable @NotBlank String filename) throws IOException {
        try {
            return fileDownloadCommon.downloadFile(sanitizeJobUuid(jobUuid), sanitizeFilename(filename), request, response);
        } catch (Exception e) {
            errorHandler.generateFHIRError(e, request, response);
            return null;
        }
    }
}
