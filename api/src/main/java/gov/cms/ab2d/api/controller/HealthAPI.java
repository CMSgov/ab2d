package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.api.util.HealthCheck;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.ApiResponseEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import static gov.cms.ab2d.common.util.Constants.HEALTH_ENDPOINT;
import static gov.cms.ab2d.common.util.Constants.HEALTH_METRICS_ENDPOINT;
import static gov.cms.ab2d.common.util.Constants.ORGANIZATION;
import static gov.cms.ab2d.common.util.Constants.REQUEST_ID;

@AllArgsConstructor
@RestController
@Slf4j
public class HealthAPI {

    private final HealthCheck healthCheck;
    private final LogManager eventLogger;

    // Add exceptions for testing and prod site
    @CrossOrigin(origins = {"http://127.0.0.1:4000", "https://ab2d.cms.gov", "http://ab2d.cms.gov"})
    @GetMapping(HEALTH_ENDPOINT)
    public ResponseEntity<Void> getHealth(HttpServletRequest request) {
        if (healthCheck.healthy()) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            eventLogger.log(new ApiResponseEvent(MDC.get(ORGANIZATION), null, HttpStatus.INTERNAL_SERVER_ERROR, "API Health NOT Ok",
                    null, (String) request.getAttribute(REQUEST_ID)));
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @CrossOrigin(origins = {"http://127.0.0.1:4000"})
    @GetMapping(HEALTH_METRICS_ENDPOINT)
    public ResponseEntity<Void> getHealth(HttpServletRequest request, @RequestParam(required = false) String type) {
        boolean ok = false;
        switch (type) {
            case "efs":
                ok = healthCheck.efsHealth();
                break;
            case "api":
                ok = healthCheck.memoryHealth();
                break;
            case "database":
                ok = healthCheck.dbHealth();
                break;
            default:
        }

        if (!ok) {
            log.info("{} healthcheck failed", type);
        }
        return new ResponseEntity<>(ok ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);

    }
}
