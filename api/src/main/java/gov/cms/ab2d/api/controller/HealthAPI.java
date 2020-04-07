package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.api.util.HealthCheck;
import gov.cms.ab2d.eventlogger.EventLogger;
import gov.cms.ab2d.eventlogger.events.ApiResponseEvent;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import static gov.cms.ab2d.common.util.Constants.*;

@RestController
public class HealthAPI {
    @Autowired
    private HealthCheck healthCheck;

    @Autowired
    private EventLogger eventLogger;

    @GetMapping(HEALTH_ENDPOINT)
    public ResponseEntity<Void> getHealth(HttpServletRequest request) {
        if (healthCheck.healthy()) {
            eventLogger.log(new ApiResponseEvent(MDC.get(USERNAME), null, HttpStatus.OK, "API Health Ok",
                    null, (String) request.getAttribute(REQUEST_ID)));
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            eventLogger.log(new ApiResponseEvent(MDC.get(USERNAME), null, HttpStatus.INTERNAL_SERVER_ERROR, "API Health NOT Ok",
                    null, (String) request.getAttribute(REQUEST_ID)));
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
