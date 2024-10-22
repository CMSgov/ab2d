package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.common.dto.PdpClientDTO;
import gov.cms.ab2d.api.controller.v1.BulkDataAccessAPIV1;
import gov.cms.ab2d.common.service.PdpClientService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import static gov.cms.ab2d.api.controller.common.ApiText.*;
import static gov.cms.ab2d.common.util.Constants.API_PREFIX_V1;
import static gov.cms.ab2d.common.util.Constants.ADMIN_PREFIX;
import static gov.cms.ab2d.fhir.BundleUtils.EOB;

@AllArgsConstructor
@Slf4j
@RestController
@RequestMapping(path = API_PREFIX_V1 + ADMIN_PREFIX, produces = APPLICATION_JSON)
public class AdminAPI {

    private final PdpClientService pdpClientService;

    private final BulkDataAccessAPIV1 bulkDataAccessAPIV1;

    @ResponseStatus(value = HttpStatus.CREATED)
    @PostMapping("/client")
    public ResponseEntity<PdpClientDTO> createClient(@RequestBody PdpClientDTO pdpClientDTO) {
        PdpClientDTO client = pdpClientService.createClient(pdpClientDTO);
        log.info("client {} created", client.getOrganization());
        return new ResponseEntity<>(client, null, HttpStatus.CREATED);
    }

    @ResponseStatus(value = HttpStatus.OK)
    @PutMapping("/client")
    public ResponseEntity<PdpClientDTO> udpateClient(@RequestBody PdpClientDTO pdpClientDTO) {
        PdpClientDTO client = pdpClientService.updateClient(pdpClientDTO);
        log.info("client {} updated", pdpClientDTO.getOrganization());
        return new ResponseEntity<>(client, null, HttpStatus.OK);
    }

    @PostMapping("/job/{contractNumber}")
    public ResponseEntity<Void> createJobByContractOnBehalfOfClient(@PathVariable @NotBlank String contractNumber,
                                                                    HttpServletRequest request,
                                                                    @RequestParam(required = false, name = TYPE_PARAM, defaultValue = EOB) String resourceTypes,
                                                                    @RequestParam(required = false, name = OUT_FORMAT) String outputFormat,
                                                                    @RequestParam(required = false, name = SINCE) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime since,
                                                                    @RequestParam(required = false, name = UNTIL) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime until) {
        pdpClientService.setupClientImpersonation(contractNumber, request);

        return bulkDataAccessAPIV1.exportPatientsWithContract(request, contractNumber, resourceTypes, outputFormat, since, until);
    }

    @ResponseStatus(value = HttpStatus.OK)
    @PutMapping("/client/{contractNumber}/enable")
    public ResponseEntity<PdpClientDTO> enableClient(@PathVariable @NotBlank String contractNumber) {
        return new ResponseEntity<>(pdpClientService.enableClient(contractNumber), null, HttpStatus.OK);
    }

    @ResponseStatus(value = HttpStatus.OK)
    @PutMapping("/client/{contractNumber}/disable")
    public ResponseEntity<PdpClientDTO> disableClient(@PathVariable @NotBlank String contractNumber) {
        return new ResponseEntity<>(pdpClientService.disableClient(contractNumber), null, HttpStatus.OK);
    }

    @ResponseStatus(value = HttpStatus.OK)
    @GetMapping("/client/{contractNumber}")
    public ResponseEntity<PdpClientDTO> getClient(@PathVariable @NotBlank String contractNumber) {
        return new ResponseEntity<>(pdpClientService.getClient(contractNumber), null, HttpStatus.OK);
    }
}
