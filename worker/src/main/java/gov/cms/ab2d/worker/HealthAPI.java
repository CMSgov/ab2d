package gov.cms.ab2d.worker;

import gov.cms.ab2d.worker.util.HealthCheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static gov.cms.ab2d.common.util.Constants.HEALTH_ENDPOINT;

@RestController
public class HealthAPI {
    @Autowired
    private HealthCheck healthCheck;

    @GetMapping(HEALTH_ENDPOINT)
    public ResponseEntity<Void> getHealth() {
        if (healthCheck.healthy()) {
            return new ResponseEntity<Void>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
