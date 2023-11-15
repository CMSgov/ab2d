package gov.cms.ab2d.api.controller.v2;

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
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;

import static gov.cms.ab2d.api.controller.common.ApiText.BULK_DNLD_DSC;
import static gov.cms.ab2d.api.controller.common.ApiText.CONTENT_TYPE_DESC;
import static gov.cms.ab2d.api.controller.common.ApiText.DNLD_DESC;
import static gov.cms.ab2d.api.controller.common.ApiText.DOWNLOAD_DESC;
import static gov.cms.ab2d.api.controller.common.ApiText.FILE_NAME;
import static gov.cms.ab2d.api.controller.common.ApiText.JOB_ID;
import static gov.cms.ab2d.api.controller.common.ApiText.APPLICATION_JSON;
import static gov.cms.ab2d.api.controller.common.ApiText.NOT_FOUND;
import static gov.cms.ab2d.api.util.Constants.GENERIC_FHIR_ERR_MSG;
import static gov.cms.ab2d.common.util.Constants.API_PREFIX_V2;
import static gov.cms.ab2d.common.util.Constants.FHIR_PREFIX;
import static gov.cms.ab2d.common.util.Constants.NDJSON_FIRE_CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

@AllArgsConstructor
@Slf4j
@Tag(name = "Download", description = BULK_DNLD_DSC)
@RestController
@ConditionalOnExpression("${v2.controller.enabled:true}")
@RequestMapping(path = API_PREFIX_V2 + FHIR_PREFIX, produces = {APPLICATION_JSON, NDJSON_FIRE_CONTENT_TYPE})
public class FileDownloadAPIV2 {
    private FileDownloadCommon fileDownloadCommon;

    @Operation(summary = DOWNLOAD_DESC)
    @Parameters(value = {
        @Parameter(name = "jobUuid", description = JOB_ID, required = true),
        @Parameter(name = "filename", description = FILE_NAME, required = true)
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = DNLD_DESC,
                headers = {@Header(name = CONTENT_TYPE, description = CONTENT_TYPE_DESC + NDJSON_FIRE_CONTENT_TYPE)},
                content = @Content(mediaType = NDJSON_FIRE_CONTENT_TYPE)
            ),
            @ApiResponse(responseCode = "404", description = NOT_FOUND + GENERIC_FHIR_ERR_MSG, content =
                @Content(mediaType = APPLICATION_JSON, schema = @Schema(ref = "#/components/schemas/OperationOutcome"))
            )
     }
    )
    @ResponseStatus(value = HttpStatus.OK)
    @GetMapping(value = "/Job/{jobUuid}/file/{filename}", produces = { NDJSON_FIRE_CONTENT_TYPE })
    public ResponseEntity downloadFile(HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable @NotBlank String jobUuid,
            @PathVariable @NotBlank String filename) throws IOException {

        return fileDownloadCommon.downloadFile(jobUuid, filename, request, response);
    }
}
