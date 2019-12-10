package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.repository.RoleRepository;
import gov.cms.ab2d.common.repository.SponsorRepository;
import gov.cms.ab2d.common.repository.UserRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.InputStream;
import java.util.List;

import static gov.cms.ab2d.api.util.Constants.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
public class RoleTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUtil testUtil;

    @Autowired
    private SponsorRepository sponsorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JobRepository jobRepository;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    private String token;

    @BeforeEach
    public void setup() {
        jobRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        sponsorRepository.deleteAll();
    }

    // This will test the API using a role that should not be able to access sponsor URLs
    @Test
    public void testWrongRoleSponsorApi() throws Exception {
        token = testUtil.setupToken(List.of(ADMIN_ROLE));

        this.mockMvc.perform(get(API_PREFIX +  FHIR_PREFIX + "/Patient/$export")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(403));
    }

    // This will test the API using a role that should not be able to access admin URLs
    @Test
    public void testWrongRoleAdminApi() throws Exception {
        token = testUtil.setupToken(List.of(SPONSOR_ROLE));

        String fileName = "parent_org_and_legal_entity_20191031_111812.xls";
        InputStream inputStream = this.getClass().getResourceAsStream("/" + fileName);

        MockMultipartFile mockMultipartFile = new MockMultipartFile("file", fileName, "application/vnd.ms-excel", inputStream);
        this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_PREFIX + ADMIN_PREFIX + "/uploadOrgStructureReport")
                .file(mockMultipartFile).contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(403));
    }

    @Test
    public void testUserWithNoRolesAccessFhir() throws Exception {
        token = testUtil.setupToken(List.of());

        this.mockMvc.perform(get(API_PREFIX +  FHIR_PREFIX + "/Patient/$export")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(403));
    }

    @Test
    public void testUserWithNoRolesAccessAdmin() throws Exception {
        token = testUtil.setupToken(List.of());

        String fileName = "parent_org_and_legal_entity_20191031_111812.xls";
        InputStream inputStream = this.getClass().getResourceAsStream("/" + fileName);

        MockMultipartFile mockMultipartFile = new MockMultipartFile("file", fileName, "application/vnd.ms-excel", inputStream);
        this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_PREFIX + ADMIN_PREFIX + "/uploadOrgStructureReport")
                .file(mockMultipartFile).contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(403));
    }
}
