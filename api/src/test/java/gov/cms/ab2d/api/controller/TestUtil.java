package gov.cms.ab2d.api.controller;

import com.okta.jwt.AccessTokenVerifier;
import com.okta.jwt.Jwt;
import com.okta.jwt.JwtVerificationException;
import com.okta.jwt.impl.DefaultJwt;
import gov.cms.ab2d.common.properties.PropertyServiceStub;
import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.job.model.JobOutput;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static gov.cms.ab2d.common.util.PropertyConstants.MAINTENANCE_MODE;
import static gov.cms.ab2d.common.util.DataSetup.TEST_PDP_CLIENT;
import static gov.cms.ab2d.fhir.BundleUtils.EOB;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Component
public class TestUtil {

    @Autowired
    private DataSetup dataSetup;

    @Getter
    private PropertiesService propertiesService = new PropertyServiceStub();

    @MockBean
    AccessTokenVerifier mockAccessTokenVerifier;

    @Value("${api.okta-jwt-audience}")
    private String oktaUrl;

    private String jwtStr = null;

    private void setupMock() throws JwtVerificationException {
        // Token that expires in 2 hours that has the client we setup below
        Map<String, Object> claims = Map.of("sub", TEST_PDP_CLIENT);
        Jwt jwt = new DefaultJwt("tokenValue", Instant.now(), Instant.now().plus(Duration.ofHours(2)), claims);

        when(mockAccessTokenVerifier.decode(anyString())).thenReturn(jwt);
    }

    private void setupInvalidMock() throws JwtVerificationException {
        when(mockAccessTokenVerifier.decode(anyString())).thenThrow(JwtVerificationException.class);
    }

    public String setupInvalidToken(List<String> clientRoles) throws JwtVerificationException {
        dataSetup.setupPdpClient(clientRoles);

        setupInvalidMock();

        return buildTokenStr();
    }

    public String setupContractWithNoAttestation(List<String> clientRoles) throws JwtVerificationException {
        dataSetup.setupContractWithNoAttestation(clientRoles);

        setupMock();

        return buildTokenStr();
    }

    public String setupContractSponsorForParentClientData(List<String> clientRoles) throws JwtVerificationException {
        dataSetup.setupContractSponsorForParentClientData(clientRoles);

        setupMock();

        return buildTokenStr();
    }

    public String setupToken(List<String> clientRoles) throws JwtVerificationException {
        dataSetup.setupPdpClient(clientRoles);

        setupMock();

        return buildTokenStr();
    }

    public String createTestDownloadFile(String tmpJobLocation, String jobUuid, String testFile) throws IOException {
        Path destination = Paths.get(tmpJobLocation, jobUuid);
        String destinationStr = destination.toString();
        Files.createDirectories(destination);
        InputStream testFileStream = this.getClass().getResourceAsStream("/" + testFile);
        String testFileStr = IOUtils.toString(testFileStream, StandardCharsets.UTF_8);
        try (PrintWriter out = new PrintWriter(destinationStr + File.separator + testFile)) {
            out.println(testFileStr);
        }

        return destinationStr;
    }

    public JobOutput createJobOutput(String testFile) {
        JobOutput jobOutput = new JobOutput();
        jobOutput.setFhirResourceType(EOB);
        jobOutput.setFilePath(testFile);
        jobOutput.setError(false);
        jobOutput.setChecksum("testoutput");
        jobOutput.setFileLength(20L);
        return jobOutput;
    }

    public void turnMaintenanceModeOff() {
        propertiesService.updateProperty(MAINTENANCE_MODE, "false");
    }

    public void turnMaintenanceModeOn() {
        propertiesService.updateProperty(MAINTENANCE_MODE, "true");
    }

    private String buildTokenStr() {
        if(jwtStr != null) {
            return jwtStr;
        }

        String clientSecret = "wefikjweglkhjwelgkjweglkwegwegewg";
        SecretKey sharedSecret = Keys.hmacShaKeyFor(clientSecret.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();

        jwtStr = Jwts.builder()
                .setAudience(oktaUrl)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(2L, ChronoUnit.HOURS)))
                .setIssuer(TEST_PDP_CLIENT)
                .setSubject(TEST_PDP_CLIENT)
                .setId(UUID.randomUUID().toString())
                .signWith(sharedSecret)
                .compact();

        return jwtStr;
    }
}
