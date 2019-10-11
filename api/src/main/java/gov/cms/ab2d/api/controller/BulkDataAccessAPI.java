package gov.cms.ab2d.api.controller;


import gov.cms.ab2d.api.util.Constants;
import io.swagger.annotations.*;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import java.io.IOException;

@Api(value = "Bulk Data Access API", description =
        "API through which an authenticated and authorized PDP sponsor" +
                " may request a bulk-data export from a server, receive status information " +
                "regarding progress in the generation of the requested files, and retrieve these " +
                "files")
@RestController
@RequestMapping(path = "/api/v1", produces = "application/json")
/**
 * The sole REST controller for AB2D's implementation of the FHIR Bulk Data API specification.
 */
public class BulkDataAccessAPI {

    private static final String ALLOWABLE_OUTPUT_FORMATS =
            "application/fhir+ndjson,application/ndjson,ndjson";

    @ApiOperation(value = "Initiate Part A & B bulk claim export job")
    @ApiImplicitParams(
            @ApiImplicitParam(name = "Prefer", required = true, paramType = "header", value =
                    "respond-async"))
    @ApiResponses(
            @ApiResponse(code = 202, message = "Export request has started", responseHeaders =
            @ResponseHeader(name = "Content-Location", description = "URL to query job status",
                    response = String.class))
    )
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    @GetMapping("/Patient/$export")
    public ResponseEntity<Void> exportAllPatients(
            @ApiParam(value = "String of comma-delimited FHIR resource types. Only resources of " +
                    "the specified resource types(s) SHALL be included in the response.",
                    allowableValues = "ExplanationOfBenefits")
            @RequestParam(required = false, name = "_type") String resourceTypes,
            @ApiParam(value = "A FHIR instant. Resources will be included in the response if " +
                    "their state has changed after the supplied time.")
            @RequestParam(required = false, name = "_since") String since,
            @ApiParam(value = "The format for the requested bulk data files to be generated.",
                    allowableValues = ALLOWABLE_OUTPUT_FORMATS, defaultValue = "application/fhir" +
                    "+ndjson"
            )
            @RequestParam(required = false, name = "_outputFormat") String outputFormat) throws IOException {
        //String encoded = FHIRUtil.outcomeToJSON(FHIRUtil.getSuccessfulOutcome("OK"));
        return new ResponseEntity<>(null, null,
                HttpStatus.ACCEPTED);
    }

    @ApiOperation(value = "Cancel a pending export job")
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "Job canceled"),
            @ApiResponse(code = 404, message = "Job not found. " + Constants.GENERIC_FHIR_ERR_MSG)}
    )
    @DeleteMapping(value = "/Job/{jobId}/$status")
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    public ResponseEntity<Void> deleteRequest(
            @ApiParam(value = "A job identifier", required = true)
            @PathVariable @NotBlank String jobId) throws IOException {
        //String encoded = FHIRUtil.outcomeToJSON(FHIRUtil.getSuccessfulOutcome("OK"));
        return new ResponseEntity<>(null, null,
                HttpStatus.ACCEPTED);
    }

    @ApiOperation(value = "Returns a status of an export job.")
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "The job is still in progress.", responseHeaders =
            @ResponseHeader(name = "X-Progress", description = "Completion percentage, such as 50%",
                    response = String.class), response = Void.class),
            @ApiResponse(code = 200, message = "The job is completed.", response =
                    JobCompletedResponse.class),
            @ApiResponse(code = 404, message = "Job not found. " + Constants.GENERIC_FHIR_ERR_MSG)}
    )
    @GetMapping(value = "/Job/{jobId}/$status")
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity<Void> getJobStatus(
            @ApiParam(value = "A job identifier", required = true) @PathVariable @NotBlank String jobId) throws IOException {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("X-Progress", "0%");
        return new ResponseEntity<>(null, responseHeaders, HttpStatus.ACCEPTED);
    }

    @ApiOperation(value = "Downloads a file produced by an export job.", response = String.class,
            produces = "application/fhir+ndjson")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Returns the requested file as " +
                    "application/fhir+ndjson.", response =
                    String.class),
            @ApiResponse(code = 404, message =
                    "Job or file not found. " + Constants.GENERIC_FHIR_ERR_MSG)}
    )
    @ResponseStatus(value = HttpStatus.OK)
    @GetMapping(value = "/Job/{jobId}/file/{filename}")
    public ResponseEntity<Resource> downloadFile(
            @ApiParam(value = "A job identifier", required = true) @PathVariable @NotBlank String jobId,
            @ApiParam(value = "A file name", required = true) @PathVariable @NotBlank String filename) throws IOException {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("X-Progress", "0%");
        return new ResponseEntity<>(null, responseHeaders, HttpStatus.ACCEPTED);
    }


}
