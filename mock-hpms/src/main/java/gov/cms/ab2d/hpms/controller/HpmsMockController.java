package gov.cms.ab2d.hpms.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/cda/orgs", produces = MediaType.APPLICATION_JSON_VALUE)
public class HpmsMockController {

    @GetMapping("/info")
    public ResponseEntity<String> getOrganizationInfo() {

        return new ResponseEntity<>("Hello SemanticBits!!!", HttpStatus.OK);
    }

}