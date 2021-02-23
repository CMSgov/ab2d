package gov.cms.ab2d.api.controller;

import com.okta.jwt.AccessTokenVerifier;
import com.okta.jwt.Jwt;
import com.okta.jwt.JwtVerificationException;
import com.okta.jwt.impl.DefaultJwt;
import gov.cms.ab2d.common.dto.PropertiesDTO;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobOutput;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.util.DataSetup;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Sort;
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
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static gov.cms.ab2d.common.util.Constants.MAINTENANCE_MODE;
import static gov.cms.ab2d.common.util.DataSetup.TEST_PDP_CLIENT;
import static gov.cms.ab2d.fhir.BundleUtils.EOB;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Component
public class TestUtil {

    @Autowired
    private DataSetup dataSetup;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PropertiesService propertiesService;

    @MockBean
    AccessTokenVerifier mockAccessTokenVerifier;

    @Value("${api.okta-jwt-audience}")
    private String oktaUrl;

    private String jwtStr = null;

    private void setupMock() throws JwtVerificationException {
        // Token that expires in 2 hours that has the user we setup below
        Map<String, Object> claims = Map.of("sub", TEST_PDP_CLIENT);
        Jwt jwt = new DefaultJwt("tokenValue", Instant.now(), Instant.now().plus(Duration.ofHours(2)), claims);

        when(mockAccessTokenVerifier.decode(anyString())).thenReturn(jwt);
    }

    private void setupInvalidMock() throws JwtVerificationException {
        when(mockAccessTokenVerifier.decode(anyString())).thenThrow(JwtVerificationException.class);
    }

    public String setupInvalidToken(List<String> userRoles) throws JwtVerificationException {
        dataSetup.setupPdpClient(userRoles);

        setupInvalidMock();

        return buildTokenStr();
    }

    public String setupContractWithNoAttestation(List<String> userRoles) throws JwtVerificationException {
        dataSetup.setupContractWithNoAttestation(userRoles);

        setupMock();

        return buildTokenStr();
    }

    public String setupContractSponsorForParentClientData(List<String> userRoles) throws JwtVerificationException {
        dataSetup.setupContractSponsorForParentClientData(userRoles);

        setupMock();

        return buildTokenStr();
    }

    public String setupToken(List<String> userRoles) throws JwtVerificationException {
        dataSetup.setupPdpClient(userRoles);

        setupMock();

        return buildTokenStr();
    }

    public String createTestDownloadFile(String tmpJobLocation, Job job, String testFile) throws IOException {
        Path destination = Paths.get(tmpJobLocation, job.getJobUuid());
        String destinationStr = destination.toString();
        Files.createDirectories(destination);
        InputStream testFileStream = this.getClass().getResourceAsStream("/" + testFile);
        String testFileStr = IOUtils.toString(testFileStream, StandardCharsets.UTF_8);
        try (PrintWriter out = new PrintWriter(destinationStr + File.separator + testFile)) {
            out.println(testFileStr);
        }

        return destinationStr;
    }

    public Job createTestJobForDownload(String testFile) {
        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();
        job.setStatus(JobStatus.SUCCESSFUL);
        job.setProgress(100);
        OffsetDateTime expireDate = OffsetDateTime.now().plusDays(100);
        job.setExpiresAt(expireDate);
        OffsetDateTime now = OffsetDateTime.now();
        job.setCompletedAt(now);

        addJobOutput(job, testFile);

        return jobRepository.saveAndFlush(job);
    }

    public void addJobOutput(Job job, String testFile) {
        JobOutput jobOutput = new JobOutput();
        jobOutput.setFhirResourceType(EOB);
        jobOutput.setJob(job);
        jobOutput.setFilePath(testFile);
        jobOutput.setError(false);
        jobOutput.setChecksum("testoutput");
        jobOutput.setFileLength(20L);
        job.getJobOutputs().add(jobOutput);
    }

    public void turnMaintenanceModeOff() {
        PropertiesDTO propertiesDTO = new PropertiesDTO();
        propertiesDTO.setKey(MAINTENANCE_MODE);
        propertiesDTO.setValue("false");
        List<PropertiesDTO> propertiesDTOs = new ArrayList<>();
        propertiesDTOs.add(propertiesDTO);
        propertiesService.updateProperties(propertiesDTOs);
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
