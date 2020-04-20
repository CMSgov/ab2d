package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.common.dto.ClearCoverageCacheRequest;
import gov.cms.ab2d.common.dto.PropertiesDTO;
import gov.cms.ab2d.common.dto.UserDTO;
import gov.cms.ab2d.common.service.CacheService;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.service.UserService;
import gov.cms.ab2d.eventlogger.EventLogger;
import gov.cms.ab2d.eventlogger.events.ApiResponseEvent;
import gov.cms.ab2d.eventlogger.events.ReloadEvent;
import gov.cms.ab2d.hpms.processing.ExcelReportProcessor;
import gov.cms.ab2d.hpms.processing.ExcelType;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

import static gov.cms.ab2d.api.util.SwaggerConstants.BULK_EXPORT_TYPE;
import static gov.cms.ab2d.api.util.SwaggerConstants.BULK_OUTPUT_FORMAT;
import static gov.cms.ab2d.common.util.Constants.API_PREFIX;
import static gov.cms.ab2d.common.util.Constants.ADMIN_PREFIX;
import static gov.cms.ab2d.common.util.Constants.EOB;
import static gov.cms.ab2d.common.util.Constants.FILE_LOG;
import static gov.cms.ab2d.common.util.Constants.SINCE_EARLIEST_DATE;
import static gov.cms.ab2d.common.util.Constants.USERNAME;
import static gov.cms.ab2d.common.util.Constants.REQUEST_ID;

@Slf4j
@RestController
@SuppressWarnings("PMD.TooManyStaticImports")
@RequestMapping(path = API_PREFIX + ADMIN_PREFIX, produces = "application/json")
public class AdminAPI {

    @Autowired
    @Qualifier("orgStructureReportProcessor")
    private ExcelReportProcessor orgStructureReportProcessor;

    @Autowired
    @Qualifier("attestationReportProcessor")
    private ExcelReportProcessor attestationReportProcessor;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private UserService userService;

    @Autowired
    private PropertiesService propertiesService;

    @Autowired
    private EventLogger eventLogger;

    @Autowired
    private BulkDataAccessAPI bulkDataAccessAPI;

    @ResponseStatus(value = HttpStatus.ACCEPTED)
    @PostMapping("/uploadOrgStructureReport")
    public ResponseEntity<Void> uploadOrgStructureReport(HttpServletRequest request, @RequestParam("file") MultipartFile hpmsFile) throws IOException {
        MDC.put(FILE_LOG, hpmsFile.getOriginalFilename());
        log.info("Request submitted to upload org structure report");

        orgStructureReportProcessor.processReport(hpmsFile.getOriginalFilename(),
                hpmsFile.getInputStream(), ExcelType.fromFileType(hpmsFile.getOriginalFilename()));

        String success = "Org structure report successfully uploaded and processed";
        log.info(success);

        eventLogger.log(new ApiResponseEvent(MDC.get(USERNAME), null,
                HttpStatus.ACCEPTED,
                "UploadOrgStructureReport Success",
                success,
                (String) request.getAttribute(REQUEST_ID)));
        return new ResponseEntity<>(null, null,
                HttpStatus.ACCEPTED);
    }

    @ResponseStatus(value = HttpStatus.ACCEPTED)
    @PostMapping("/uploadAttestationReport")
    public ResponseEntity<Void> uploadAttestationReport(HttpServletRequest request, @RequestParam("file") MultipartFile attestationFile) throws IOException {
        MDC.put(FILE_LOG, attestationFile.getOriginalFilename());
        log.info("Request submitted to upload attestation report");

        attestationReportProcessor.processReport(attestationFile.getOriginalFilename(),
                attestationFile.getInputStream(), ExcelType.fromFileType(attestationFile.getOriginalFilename()));

        String success = "Attestation report successfully uploaded";
        log.info(success);

        eventLogger.log(new ApiResponseEvent(MDC.get(USERNAME), null,
                HttpStatus.ACCEPTED,
                "uploadAttestationReport Success",
                success,
                (String) request.getAttribute(REQUEST_ID)));
        return new ResponseEntity<>(null, null,
                HttpStatus.ACCEPTED);
    }

    @ResponseStatus(value = HttpStatus.CREATED)
    @PostMapping("/user")
    public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO userDTO) {
        UserDTO user = userService.createUser(userDTO);
        log.info("{} was created", user.getUsername());
        return new ResponseEntity<>(user, null, HttpStatus.CREATED);
    }

    @ResponseStatus(value = HttpStatus.OK)
    @PutMapping("/user")
    public ResponseEntity<UserDTO> updateUser(@RequestBody UserDTO userDTO) {
        UserDTO user = userService.updateUser(userDTO);
        log.info("{} was updated", user.getUsername());
        return new ResponseEntity<>(user, null, HttpStatus.OK);
    }

    @ResponseStatus(value = HttpStatus.OK)
    @GetMapping("/properties")
    public ResponseEntity<List<PropertiesDTO>> readProperties() {
        return new ResponseEntity<>(propertiesService.getAllPropertiesDTO(), null, HttpStatus.OK);
    }

    @ResponseStatus(value = HttpStatus.OK)
    @PutMapping("/properties")
    public ResponseEntity<List<PropertiesDTO>> updateProperties(@RequestBody List<PropertiesDTO> propertiesDTOs) {
        eventLogger.log(new ReloadEvent(MDC.get(USERNAME), ReloadEvent.FileType.PROPERTIES, null,
                propertiesDTOs.size()));
        return new ResponseEntity<>(propertiesService.updateProperties(propertiesDTOs), null, HttpStatus.OK);
    }

    @PostMapping("/coverage/clearCache")
    public ResponseEntity<Void> clearCoverageCache(@RequestBody ClearCoverageCacheRequest request) {
        cacheService.clearCache(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/user/{username}/job")
    public ResponseEntity<Void> createJobOnBehalfOfUser(@PathVariable @NotBlank String username,
        HttpServletRequest request,
        @RequestParam(required = false, name = "_type", defaultValue = EOB) String resourceTypes,
        @RequestParam(required = false, name = "_outputFormat") String outputFormat,
        @RequestParam(required = false, name = "_since") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime since) {
        userService.setupUserImpersonation(username, request);

        return bulkDataAccessAPI.exportAllPatients(request, resourceTypes, outputFormat, since);
    }
}
