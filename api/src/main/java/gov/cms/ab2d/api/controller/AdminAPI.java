package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.hpms.processing.ExcelReportProcessor;
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
    @Qualifier("hpmsExcelReportProcessor")
    private ExcelReportProcessor hpmsExcelReportProcessor;

    @ResponseStatus(value = HttpStatus.ACCEPTED)
    @PostMapping("/uploadHPMSFile")
    public ResponseEntity<Void> uploadHPMSFile(@RequestParam("file") MultipartFile hpmsFile) throws IOException {
        hpmsExcelReportProcessor.processReport(hpmsFile.getInputStream());

        return new ResponseEntity<>(null, null,
                HttpStatus.ACCEPTED);
    }
}
