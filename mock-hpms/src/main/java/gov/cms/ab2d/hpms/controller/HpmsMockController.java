package gov.cms.ab2d.hpms.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.Charset;

@RestController
@RequestMapping(path = "/api/cda/orgs", produces = MediaType.APPLICATION_JSON_VALUE)
public class HpmsMockController {

    @Value("classpath:organizations.json")
    private Resource resource;

    @GetMapping("/info")
    public ResponseEntity<String> getOrganizationInfo() throws IOException {
        String response = StreamUtils.copyToString(resource.getInputStream(), Charset.defaultCharset());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}