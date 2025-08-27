package gov.cms.ab2d.contracts.controller;

import gov.cms.ab2d.contracts.service.HealthcheckService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@AllArgsConstructor
@RestController
public class HealthCheckController {

    private final HealthcheckService healthcheckService;

    @GetMapping("/health")
    public ResponseEntity<Void> getHealth() {

        return new ResponseEntity<>(healthcheckService.checkDatabaseConnection()
                ? HttpStatus.OK
                : HttpStatus.INTERNAL_SERVER_ERROR);
    }
}