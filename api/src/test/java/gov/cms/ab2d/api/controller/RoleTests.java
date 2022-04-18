package gov.cms.ab2d.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.eventlogger.reports.sql.LoggerEventRepository;
import org.junit.jupiter.api.*;
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

import static gov.cms.ab2d.common.model.Role.ADMIN_ROLE;
import static gov.cms.ab2d.common.model.Role.ATTESTOR_ROLE;
import static gov.cms.ab2d.common.model.Role.SPONSOR_ROLE;
import static gov.cms.ab2d.common.util.Constants.*;
import static gov.cms.ab2d.common.util.DataSetup.TEST_PDP_CLIENT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
    LoggerEventRepository loggerEventRepository;

    @Autowired
    private DataSetup dataSetup;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    private String token;

    @BeforeEach
    public void setup() {
        testUtil.turnMaintenanceModeOff();
    }

    @AfterEach
    public void cleanup() {
        dataSetup.cleanup();
        loggerEventRepository.delete();
    }

    // This will test the API using a role that should not be able to access sponsor URLs
    @Test
    public void testAdminRoleAccessingSponsorApiIsDisabled() throws Exception {
        token = testUtil.setupToken(List.of(ADMIN_ROLE));

        this.mockMvc.perform(get(API_PREFIX_V1 + FHIR_PREFIX + "/Patient/$export")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(403));
    }

    // This will test the API using a role that should not be able to access sponsor URLs
    @Test
    public void testAttestorRoleAccessingSponsorApi() throws Exception {
        token = testUtil.setupToken(List.of(ATTESTOR_ROLE));

        this.mockMvc.perform(get(API_PREFIX_V1 + FHIR_PREFIX + "/Patient/$export")
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
        this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_PREFIX_V1 + ADMIN_PREFIX + "/uploadOrgStructureReport")
                .file(mockMultipartFile).contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(403));
    }

    // This will test the API using a role that should not be able to access admin URLs
    @Test
    public void testWrongRoleAttestorAdminApi() throws Exception {
        token = testUtil.setupToken(List.of(ATTESTOR_ROLE));

        String fileName = "parent_org_and_legal_entity_20191031_111812.xls";
        InputStream inputStream = this.getClass().getResourceAsStream("/" + fileName);

        MockMultipartFile mockMultipartFile = new MockMultipartFile("file", fileName, "application/vnd.ms-excel", inputStream);
        this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_PREFIX_V1 + ADMIN_PREFIX + "/uploadOrgStructureReport")
                .file(mockMultipartFile).contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(403));
    }

    @Test
    public void testClientWithNoRolesAccessFhir() throws Exception {
        token = testUtil.setupToken(List.of());

        this.mockMvc.perform(get(API_PREFIX_V1 +  FHIR_PREFIX + "/Patient/$export")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(403));
    }

    @Test
    public void testClientWithNoRolesAccessAdmin() throws Exception {
        token = testUtil.setupToken(List.of());

        String fileName = "parent_org_and_legal_entity_20191031_111812.xls";
        InputStream inputStream = this.getClass().getResourceAsStream("/" + fileName);

        MockMultipartFile mockMultipartFile = new MockMultipartFile("file", fileName, "application/vnd.ms-excel", inputStream);
        this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_PREFIX_V1 + ADMIN_PREFIX + "/uploadOrgStructureReport")
                .file(mockMultipartFile).contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(403));
    }

    @Test
    public void testWrongRoleClientCreate() throws Exception {
        token = testUtil.setupToken(List.of(SPONSOR_ROLE));

        this.mockMvc.perform(post(API_PREFIX_V1 +  ADMIN_PREFIX + "/client")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(403));
    }

    @Test
    public void testWrongRoleClientUpdate() throws Exception {
        token = testUtil.setupToken(List.of(SPONSOR_ROLE));

        this.mockMvc.perform(put(API_PREFIX_V1 +  ADMIN_PREFIX + "/client")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(403));
    }

    @Test
    public void testWrongRolePropertiesRetrieval() throws Exception {
        token = testUtil.setupToken(List.of(SPONSOR_ROLE));

        this.mockMvc.perform(get(API_PREFIX_V1 +  ADMIN_PREFIX + "/properties")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(403));
    }

    @Test
    public void testWrongRolePropertiesUpdate() throws Exception {
        token = testUtil.setupToken(List.of(SPONSOR_ROLE));

        this.mockMvc.perform(put(API_PREFIX_V1 +  ADMIN_PREFIX + "/properties")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(403));
    }

    @Test
    public void testWrongRoleCreateJobOnBehalfOfClient() throws Exception {
        token = testUtil.setupToken(List.of(SPONSOR_ROLE));

        this.mockMvc.perform(put(API_PREFIX_V1 +  ADMIN_PREFIX + "/client/testclient/job")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(403));
    }

    @Test
    public void testWrongRoleCreateJobByContractOnBehalfOfClient() throws Exception {
        token = testUtil.setupToken(List.of(SPONSOR_ROLE));

        this.mockMvc.perform(put(API_PREFIX_V1 +  ADMIN_PREFIX + "/client/testclient/job/Z0001")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(403));
    }

    @Test
    public void testWrongRoleEnableClient() throws Exception {
        token = testUtil.setupToken(List.of(SPONSOR_ROLE));

        this.mockMvc.perform(put(API_PREFIX_V1 +  ADMIN_PREFIX + "/client/" + TEST_PDP_CLIENT + "/enable")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(403));
    }

    @Test
    public void testWrongRoleDisableClient() throws Exception {
        token = testUtil.setupToken(List.of(SPONSOR_ROLE));

        this.mockMvc.perform(put(API_PREFIX_V1 +  ADMIN_PREFIX + "/client/" + TEST_PDP_CLIENT + "/disable")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(403));
    }

    @Test
    public void testWrongRoleGetClient() throws Exception {
        token = testUtil.setupToken(List.of(SPONSOR_ROLE));

        this.mockMvc.perform(get(API_PREFIX_V1 +  ADMIN_PREFIX + "/client/" + TEST_PDP_CLIENT)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(403));
    }

    @Test
    public void testWrongRoleIPs() throws Exception {
        token = testUtil.setupToken(List.of(SPONSOR_ROLE));

        ObjectMapper mapper = new ObjectMapper();

        this.mockMvc.perform(get(API_PREFIX_V1 +  ADMIN_PREFIX + "/ip")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString("sponsorDTO"))
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(403));
    }
}
