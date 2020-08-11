package gov.cms.ab2d.hpms.controller;

import gov.cms.ab2d.hpms.service.AttestationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.StringJoiner;

@RestController
@RequestMapping(path = "/api/cda", produces = MediaType.APPLICATION_JSON_VALUE)
public class HpmsMockController {

    private final AttestationService attestationService;

    @Value("classpath:organizations.json")
    private Resource resource;

    public HpmsMockController(AttestationService attestationService) {
        this.attestationService = attestationService;
    }

    @GetMapping("/orgs/info")
    public ResponseEntity<String> getOrganizationInfo() throws IOException {
        String response = StreamUtils.copyToString(resource.getInputStream(), Charset.defaultCharset());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/contracts/status")
    public ResponseEntity<String> getAttestation(@RequestParam() JsonStringArray contractIds) {
        List<String> attestations = attestationService.retrieveAttestations(contractIds.getValues());
        if (attestations.isEmpty()) {
            return errorAttestationResponse();
        }
        return new ResponseEntity<>(formatAttestationPayload(attestations), HttpStatus.OK);
    }

    private ResponseEntity<String> errorAttestationResponse() {
        return new ResponseEntity<>("42", HttpStatus.BAD_REQUEST);
    }

    String formatAttestationPayload(List<String> jsonEntries) {
        StringJoiner sj = new StringJoiner(",", "{\"contracts\": [", "]}");
        jsonEntries.forEach(sj::add);
        return sj.toString();
    }
}