package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.hpms.processing.ExcelReportProcessor;
import gov.cms.ab2d.hpms.processing.ExcelType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static gov.cms.ab2d.api.util.Constants.API_PREFIX;

@RestController
@RequestMapping(path = API_PREFIX, produces = "application/json")
public class AdminAPI {

    @Autowired
    @Qualifier("orgStructureReportProcessor")
    private ExcelReportProcessor orgStructureReportProcessor;

    @Autowired
    @Qualifier("attestationReportProcessor")
    private ExcelReportProcessor attestationReportProcessor;

    @ResponseStatus(value = HttpStatus.ACCEPTED)
    @PostMapping("/uploadOrgStructureReport")
    public ResponseEntity<Void> uploadOrgStructureReport(@RequestParam("file") MultipartFile hpmsFile) throws IOException {
        orgStructureReportProcessor.processReport(hpmsFile.getInputStream(), ExcelType.fromFileType(hpmsFile.getOriginalFilename()));

        return new ResponseEntity<>(null, null,
                HttpStatus.ACCEPTED);
    }

    @ResponseStatus(value = HttpStatus.ACCEPTED)
    @PostMapping("/uploadAttestationReport")
    public ResponseEntity<Void> uploadAttestationReport(@RequestParam("file") MultipartFile hpmsFile) throws IOException {
        attestationReportProcessor.processReport(hpmsFile.getInputStream(), ExcelType.fromFileType(hpmsFile.getOriginalFilename()));

        return new ResponseEntity<>(null, null,
                HttpStatus.ACCEPTED);
    }
}
