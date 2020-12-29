package gov.cms.ab2d.worker;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.health.SlackAvailable;
import gov.cms.ab2d.worker.util.HealthCheck;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static gov.cms.ab2d.common.util.Constants.*;

@RestController
public class HealthAPI {
    @Autowired
    private HealthCheck healthCheck;

    @Autowired
    private BFDClient bfdClient;

    @Autowired
    private SlackAvailable slackAvailable;

    @GetMapping(HEALTH_ENDPOINT)
    public ResponseEntity<Void> getHealth() {
        if (healthCheck.healthy()) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(VALIDATE_SLACK_ENDPOINT)
    public ResponseEntity<Void> sendMessageToSlack() {
        if (slackAvailable.slackAvailable("Test Message from Worker API")) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(VALIDATE_BFD_ENDPOINT)
    public ResponseEntity<Void> isBfdUp() {
        try {
            bfdClient.capabilityStatement();
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}
