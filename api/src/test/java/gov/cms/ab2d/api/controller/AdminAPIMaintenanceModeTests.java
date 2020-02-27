package gov.cms.ab2d.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.common.dto.PropertiesDTO;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static gov.cms.ab2d.api.controller.BulkDataAccessAPIIntegrationTests.PATIENT_EXPORT_PATH;
import static gov.cms.ab2d.api.util.Constants.ADMIN_ROLE;
import static gov.cms.ab2d.common.util.Constants.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
public class AdminAPIMaintenanceModeTests {

    @Autowired
    private MockMvc mockMvc;

    @Value("${efs.mount}")
    private String tmpJobLocation;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Autowired
    private SponsorRepository sponsorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private TestUtil testUtil;

    private static final String PROPERTIES_URL = "/properties";

    private String token;

    @BeforeEach
    public void setup() throws JwtVerificationException {
        contractRepository.deleteAll();
        jobRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        sponsorRepository.deleteAll();

        token = testUtil.setupToken(List.of(SPONSOR_ROLE, ADMIN_ROLE));
    }

    @Test
    public void testSwitchMaintenanceModeOnAndOff() throws Exception {
        List<PropertiesDTO> propertiesDTOs = new ArrayList<>();
        PropertiesDTO maintenanceModeDTO = new PropertiesDTO();
        maintenanceModeDTO.setKey(MAINTENANCE_MODE);
        maintenanceModeDTO.setValue("true");
        propertiesDTOs.add(maintenanceModeDTO);

        ObjectMapper mapper = new ObjectMapper();

        this.mockMvc.perform(
                put(API_PREFIX + ADMIN_PREFIX + PROPERTIES_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(propertiesDTOs))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(200));

        this.mockMvc.perform(get(API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(500));

        propertiesDTOs.clear();
        maintenanceModeDTO.setValue("false");
        propertiesDTOs.add(maintenanceModeDTO);

        this.mockMvc.perform(
                put(API_PREFIX + ADMIN_PREFIX + PROPERTIES_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(propertiesDTOs))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(200));

        this.mockMvc.perform(get(API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(202));
    }

    @Test
    public void testJobsCanStillBeDownloadedWhileInMaintenanceMode() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(get(API_PREFIX + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(202)).andReturn();
        String contentLocationUrl = mvcResult.getResponse().getHeader("Content-Location");

        List<PropertiesDTO> propertiesDTOs = new ArrayList<>();
        PropertiesDTO maintenanceModeDTO = new PropertiesDTO();
        maintenanceModeDTO.setKey(MAINTENANCE_MODE);
        maintenanceModeDTO.setValue("true");
        propertiesDTOs.add(maintenanceModeDTO);

        ObjectMapper mapper = new ObjectMapper();

        this.mockMvc.perform(
                put(API_PREFIX + ADMIN_PREFIX + PROPERTIES_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(propertiesDTOs))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(200));

        String testFile = "test.ndjson";

        Job job = testUtil.createTestJobForDownload(testFile);

        String destinationStr = testUtil.createTestDownloadFile(tmpJobLocation, job, testFile);

        MvcResult mvcResultStatusCheck = this.mockMvc.perform(get(contentLocationUrl)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(200)).andReturn();

        String downloadUrl = JsonPath.read(mvcResultStatusCheck.getResponse().getContentAsString(),
                "$.output[0].url");
        this.mockMvc.perform(get(downloadUrl).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .header("Accept-Encoding", "gzip, deflate, br"))
                        .andExpect(status().is(200));

        Assert.assertTrue(!Files.exists(Paths.get(destinationStr + File.separator + testFile)));

        // Cleanup
        propertiesDTOs.clear();
        maintenanceModeDTO.setValue("false");
        propertiesDTOs.add(maintenanceModeDTO);

        this.mockMvc.perform(
                put(API_PREFIX + ADMIN_PREFIX + PROPERTIES_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(propertiesDTOs))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(200));
    }
}