package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.common.util.PropertyConstants;
import gov.cms.ab2d.common.properties.PropertiesService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static gov.cms.ab2d.api.controller.common.ApiText.APPLICATION_JSON;
import static gov.cms.ab2d.common.util.Constants.*;

@AllArgsConstructor
@Slf4j
@RestController
@RequestMapping(produces = APPLICATION_JSON)
public class MaintenanceModeAPI {

    private final PropertiesService propertiesService;

    // This API endpoint does not have authentication, an exception was made in SecurityConfig.java and JWTAuthenticationFilter.java
    @CrossOrigin // test server for the sandbox runs from port 4000
    @ResponseStatus(value = HttpStatus.ACCEPTED)
    @GetMapping(STATUS_ENDPOINT)
    public ResponseEntity<MaintenanceModeResponse> getMaintenanceMode() {
        MaintenanceModeResponse maintenanceModeResponse = new MaintenanceModeResponse(String.valueOf(propertiesService.isToggleOn(PropertyConstants.MAINTENANCE_MODE, false)));
        return new ResponseEntity<>(maintenanceModeResponse, null, HttpStatus.OK);
    }
}
