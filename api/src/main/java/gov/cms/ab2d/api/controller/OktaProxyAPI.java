package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.api.security.OktaTokenGenerator;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URISyntaxException;

import static gov.cms.ab2d.common.util.Constants.OKTA_PROXY_ENDPOINT;

@Slf4j
@RestController
@RequestMapping(produces = "application/json")
public class OktaProxyAPI {

    @Autowired
    private OktaTokenGenerator oktaTokenGenerator;

    // This API endpoint does not have authentication, an exception was made in SecurityConfig.java and JWTAuthenticationFilter.java
    @CrossOrigin // test server for the sandbox runs from port 4000
    @ResponseStatus(value = HttpStatus.OK)
    @PostMapping(OKTA_PROXY_ENDPOINT)
    public ResponseEntity<OktaProxyResponse> getOktaToken(@RequestParam String clientID, @RequestParam String clientSecret) throws URISyntaxException,
            InterruptedException, JSONException, IOException {
        String token = oktaTokenGenerator.generateToken(clientID, clientSecret);
        return new ResponseEntity<>(new OktaProxyResponse(token), null, HttpStatus.OK);
    }
}
