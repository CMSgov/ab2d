package gov.cms.ab2d.worker;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.worker.util.HealthCheck;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static gov.cms.ab2d.common.util.Constants.*;
import static gov.cms.ab2d.fhir.FhirVersion.R4;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;

@RequiredArgsConstructor
@RestController
@Slf4j
public class HealthAPI {

    private final HealthCheck healthCheck;
    private final BFDClient bfdClient;

    @GetMapping(HEALTH_ENDPOINT)
    public ResponseEntity<Void> getHealth() {
        if (healthCheck.healthy()) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(VALIDATE_BFD_ENDPOINT)
    public ResponseEntity<Void> isBfdUp() {
        try {
            bfdClient.capabilityStatement(STU3);
            try {
                bfdClient.capabilityStatement(R4);
            } catch (Exception ex) {
                log.error("BFD V2 interface is down");
            }
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}
