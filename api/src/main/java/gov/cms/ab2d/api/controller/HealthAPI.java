package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.api.util.HealthCheck;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.eventclient.events.ApiResponseEvent;
import javax.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


import static gov.cms.ab2d.common.util.Constants.HEALTH_ENDPOINT;
import static gov.cms.ab2d.common.util.Constants.ORGANIZATION;
import static gov.cms.ab2d.common.util.Constants.REQUEST_ID;

@AllArgsConstructor
@RestController
public class HealthAPI {

    private final HealthCheck healthCheck;
    private final SQSEventClient eventLogger;

    // Add exceptions for testing and prod site
    @CrossOrigin(origins = {"http://127.0.0.1:4000", "https://ab2d.cms.gov", "http://ab2d.cms.gov"})
    @GetMapping(HEALTH_ENDPOINT)
    public ResponseEntity<Void> getHealth(HttpServletRequest request) {
        if (healthCheck.healthy()) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            eventLogger.sendLogs(new ApiResponseEvent(MDC.get(ORGANIZATION), null, HttpStatus.INTERNAL_SERVER_ERROR, "API Health NOT Ok",
                    null, (String) request.getAttribute(REQUEST_ID)));
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
