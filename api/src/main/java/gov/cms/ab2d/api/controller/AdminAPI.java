package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.api.controller.v1.BulkDataAccessAPIV1;
import gov.cms.ab2d.common.dto.PropertiesDTO;
import gov.cms.ab2d.common.dto.UserDTO;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.service.UserService;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.ReloadEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.List;

import static gov.cms.ab2d.api.controller.common.ApiText.JSON;
import static gov.cms.ab2d.api.controller.common.ApiText.OUT_FORMAT;
import static gov.cms.ab2d.api.controller.common.ApiText.SINCE;
import static gov.cms.ab2d.api.controller.common.ApiText.TYPE;
import static gov.cms.ab2d.common.util.Constants.API_PREFIX_V1;
import static gov.cms.ab2d.common.util.Constants.ADMIN_PREFIX;
import static gov.cms.ab2d.common.util.Constants.USERNAME;
import static gov.cms.ab2d.fhir.BundleUtils.EOB;

@AllArgsConstructor
@Slf4j
@RestController
@SuppressWarnings("PMD.TooManyStaticImports")
@RequestMapping(path = API_PREFIX_V1 + ADMIN_PREFIX, produces = JSON)
public class AdminAPI {

    private final UserService userService;

    private final PropertiesService propertiesService;

    private final LogManager eventLogger;

    private final BulkDataAccessAPIV1 bulkDataAccessAPIV1;

    @ResponseStatus(value = HttpStatus.CREATED)
    @PostMapping("/user")
    public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO userDTO) {
        UserDTO user = userService.createUser(userDTO);
        log.info("{} was created", user.getUsername());
        return new ResponseEntity<>(user, null, HttpStatus.CREATED);
    }

    @ResponseStatus(value = HttpStatus.OK)
    @PutMapping("/user")
    public ResponseEntity<UserDTO> updateUser(@RequestBody UserDTO userDTO) {
        UserDTO user = userService.updateUser(userDTO);
        log.info("{} was updated", user.getUsername());
        return new ResponseEntity<>(user, null, HttpStatus.OK);
    }

    @ResponseStatus(value = HttpStatus.OK)
    @GetMapping("/properties")
    public ResponseEntity<List<PropertiesDTO>> readProperties() {
        return new ResponseEntity<>(propertiesService.getAllPropertiesDTO(), null, HttpStatus.OK);
    }

    @ResponseStatus(value = HttpStatus.OK)
    @PutMapping("/properties")
    public ResponseEntity<List<PropertiesDTO>> updateProperties(@RequestBody List<PropertiesDTO> propertiesDTOs) {
        eventLogger.log(new ReloadEvent(MDC.get(USERNAME), ReloadEvent.FileType.PROPERTIES, null,
                propertiesDTOs.size()));
        return new ResponseEntity<>(propertiesService.updateProperties(propertiesDTOs), null, HttpStatus.OK);
    }

    @PostMapping("/job/{contractNumber}")
    public ResponseEntity<Void> createJobByContractOnBehalfOfUser(@PathVariable @NotBlank String contractNumber,
                                                        HttpServletRequest request,
                                                        @RequestParam(required = false, name = TYPE, defaultValue = EOB) String resourceTypes,
                                                        @RequestParam(required = false, name = OUT_FORMAT) String outputFormat,
                                                        @RequestParam(required = false, name = SINCE) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime since) {
        userService.setupUserImpersonation(contractNumber, request);

        return bulkDataAccessAPIV1.exportPatientsWithContract(request, contractNumber, resourceTypes, outputFormat, since);
    }

    @ResponseStatus(value = HttpStatus.OK)
    @PutMapping("/user/{contractNumber}/enable")
    public ResponseEntity<UserDTO> enableUser(@PathVariable @NotBlank String contractNumber) {
        return new ResponseEntity<>(userService.enableUser(contractNumber), null, HttpStatus.OK);
    }

    @ResponseStatus(value = HttpStatus.OK)
    @PutMapping("/user/{contractNumber}/disable")
    public ResponseEntity<UserDTO> disableUser(@PathVariable @NotBlank String contractNumber) {
        return new ResponseEntity<>(userService.disableUser(contractNumber), null, HttpStatus.OK);
    }

    @ResponseStatus(value = HttpStatus.OK)
    @GetMapping("/user/{contractNumber}")
    public ResponseEntity<UserDTO> getUser(@PathVariable @NotBlank String contractNumber) {
        return new ResponseEntity<>(userService.getUser(contractNumber), null, HttpStatus.OK);
    }
}
