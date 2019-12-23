package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.common.config.Mapping;
import gov.cms.ab2d.common.dto.UserDTO;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.service.UserService;
import gov.cms.ab2d.hpms.processing.ExcelReportProcessor;
import gov.cms.ab2d.hpms.processing.ExcelType;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static gov.cms.ab2d.api.util.Constants.ADMIN_PREFIX;
import static gov.cms.ab2d.api.util.Constants.API_PREFIX;
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
    private UserService userService;

    @Autowired
    private Mapping mapping;

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
        User user = userService.createUser(userDTO);
        UserDTO createdUser = mapping.getModelMapper().map(user, UserDTO.class);
        return new ResponseEntity<>(createdUser, null, HttpStatus.CREATED);
    }

    @ResponseStatus(value = HttpStatus.OK)
    @PutMapping("/user")
    public ResponseEntity<UserDTO> updateUser(@RequestBody UserDTO userDTO) {
        User user = userService.updateUser(userDTO);
        UserDTO updatedUser = mapping.getModelMapper().map(user, UserDTO.class);
        return new ResponseEntity<>(updatedUser, null, HttpStatus.OK);
    }
}
