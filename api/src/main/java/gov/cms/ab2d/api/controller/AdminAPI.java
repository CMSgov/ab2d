package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.common.dto.ClearCoverageCacheRequest;
import gov.cms.ab2d.common.dto.UserDTO;
import gov.cms.ab2d.common.service.CacheService;
import gov.cms.ab2d.common.service.UserService;
import gov.cms.ab2d.hpms.processing.ExcelReportProcessor;
import gov.cms.ab2d.hpms.processing.ExcelType;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static gov.cms.ab2d.common.util.Constants.ADMIN_PREFIX;
import static gov.cms.ab2d.common.util.Constants.API_PREFIX;
import static gov.cms.ab2d.common.util.Constants.FILE_LOG;

@Slf4j
@RestController
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

    @ResponseStatus(value = HttpStatus.ACCEPTED)
    @PostMapping("/uploadOrgStructureReport")
    public ResponseEntity<Void> uploadOrgStructureReport(@RequestParam("file") MultipartFile hpmsFile) throws IOException {
        MDC.put(FILE_LOG, hpmsFile.getOriginalFilename());
        log.info("Request submitted to upload org structure report");

        orgStructureReportProcessor.processReport(hpmsFile.getInputStream(), ExcelType.fromFileType(hpmsFile.getOriginalFilename()));

        log.info("Org structure report successfully uploaded and processed");

        return new ResponseEntity<>(null, null,
                HttpStatus.ACCEPTED);
    }

    @ResponseStatus(value = HttpStatus.ACCEPTED)
    @PostMapping("/uploadAttestationReport")
    public ResponseEntity<Void> uploadAttestationReport(@RequestParam("file") MultipartFile attestationFile) throws IOException {
        MDC.put(FILE_LOG, attestationFile.getOriginalFilename());
        log.info("Request submitted to upload attestation report");

        attestationReportProcessor.processReport(attestationFile.getInputStream(), ExcelType.fromFileType(attestationFile.getOriginalFilename()));

        log.info("Attestation report successfully uploaded");

        return new ResponseEntity<>(null, null,
                HttpStatus.ACCEPTED);
    }

    @ResponseStatus(value = HttpStatus.CREATED)
    @PostMapping("/user")
    public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO userDTO) {
        UserDTO user = userService.createUser(userDTO);
        return new ResponseEntity<>(user, null, HttpStatus.CREATED);
    }

    @ResponseStatus(value = HttpStatus.OK)
    @PutMapping("/user")
    public ResponseEntity<UserDTO> updateUser(@RequestBody UserDTO userDTO) {
        UserDTO user = userService.updateUser(userDTO);
        return new ResponseEntity<>(user, null, HttpStatus.OK);
    }


    @PostMapping("/coverage/clearCache")
    public ResponseEntity<Void> clearCoverageCache(@RequestBody ClearCoverageCacheRequest request) {
        cacheService.clearCache(request);
        return ResponseEntity.noContent().build();
    }


}
