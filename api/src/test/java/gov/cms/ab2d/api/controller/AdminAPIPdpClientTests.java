package gov.cms.ab2d.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.api.remote.JobClientMock;
import gov.cms.ab2d.common.dto.PdpClientDTO;
import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.model.Role;
import gov.cms.ab2d.common.repository.PdpClientRepository;
import gov.cms.ab2d.common.service.RoleService;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.AB2DSQSMockConfig;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.contracts.model.ContractDTO;
import gov.cms.ab2d.job.dto.StartJobDTO;
import java.util.List;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import static gov.cms.ab2d.common.model.Role.ADMIN_ROLE;
import static gov.cms.ab2d.common.model.Role.ATTESTOR_ROLE;
import static gov.cms.ab2d.common.model.Role.SPONSOR_ROLE;
import static gov.cms.ab2d.common.util.Constants.ADMIN_PREFIX;
import static gov.cms.ab2d.common.util.Constants.API_PREFIX_V1;
import static gov.cms.ab2d.common.util.DataSetup.VALID_CONTRACT_NUMBER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.HttpHeaders.CONTENT_LOCATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
@Import(AB2DSQSMockConfig.class)
public class AdminAPIPdpClientTests {

    public static final String TEST_CLIENT = "test@test.com";
    public static final String TEST_ORG = "PDP-TEST";
    private static final String CLIENT_URL = "/client";
    private static final String ENABLE_DISABLE_CLIENT = "enableDisableClient";
    private static final String ENABLE_DISABLE_CONTRACT = "Z0000";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    JobClientMock jobClientMock;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Autowired
    private PdpClientRepository pdpClientRepository;

    @Autowired
    private TestUtil testUtil;

    @Autowired
    private DataSetup dataSetup;

    @Autowired
    private RoleService roleService;


    private String token;


    @BeforeEach
    public void setup() throws JwtVerificationException {
        token = testUtil.setupToken(List.of(ADMIN_ROLE, SPONSOR_ROLE, ATTESTOR_ROLE));
    }

    @AfterEach
    public void cleanup() {
        dataSetup.queueForCleanup(pdpClientRepository.findByClientId(TEST_CLIENT));
        dataSetup.cleanup();
        jobClientMock.cleanupAll();
    }

    @Test
    public void testCreateClient() throws Exception {
        PdpClientDTO pdpClientDTO = new PdpClientDTO();
        pdpClientDTO.setClientId(TEST_CLIENT);
        pdpClientDTO.setOrganization(TEST_ORG);
        pdpClientDTO.setEnabled(true);
        pdpClientDTO.setContract(buildContractDTO());
        pdpClientDTO.setRole(ADMIN_ROLE);
        Role role = roleService.findRoleByName(ADMIN_ROLE);
        pdpClientDTO.setRole(role.getName());

        ObjectMapper mapper = getMapper();

        MvcResult mvcResult = this.mockMvc.perform(
                        post(API_PREFIX_V1 + ADMIN_PREFIX + CLIENT_URL)
                                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(pdpClientDTO))
                                .header("Authorization", "Bearer " + token))
                .andReturn();

        assertEquals(201, mvcResult.getResponse().getStatus());

        String result = mvcResult.getResponse().getContentAsString();
        PdpClientDTO createdPdpClientDTO = mapper.readValue(result, PdpClientDTO.class);
        assertEquals(createdPdpClientDTO.getClientId(), pdpClientDTO.getClientId());
        assertEquals(createdPdpClientDTO.getEnabled(), pdpClientDTO.getEnabled());
        assertEquals(createdPdpClientDTO.getContract().getContractNumber(), pdpClientDTO.getContract().getContractNumber());
        assertEquals(createdPdpClientDTO.getContract().getContractName(), pdpClientDTO.getContract().getContractName());
        assertEquals(createdPdpClientDTO.getRole(), pdpClientDTO.getRole());
    }

    private ObjectMapper getMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
    }

    @Test
    public void testCreateClientAttestor() throws Exception {
        PdpClientDTO pdpClientDTO = new PdpClientDTO();
        pdpClientDTO.setClientId(TEST_CLIENT);
        pdpClientDTO.setOrganization(TEST_ORG);
        pdpClientDTO.setEnabled(true);
        pdpClientDTO.setContract(buildContractDTO());
        Role role = roleService.findRoleByName(ATTESTOR_ROLE);
        pdpClientDTO.setRole(role.getName());

        ObjectMapper mapper = getMapper();

        MvcResult mvcResult = this.mockMvc.perform(
                        post(API_PREFIX_V1 + ADMIN_PREFIX + CLIENT_URL)
                                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(pdpClientDTO))
                                .header("Authorization", "Bearer " + token))
                .andReturn();

        assertEquals(201, mvcResult.getResponse().getStatus());

        String result = mvcResult.getResponse().getContentAsString();
        PdpClientDTO createdPdpClientDTO = mapper.readValue(result, PdpClientDTO.class);
        assertEquals(createdPdpClientDTO.getClientId(), pdpClientDTO.getClientId());
        assertEquals(createdPdpClientDTO.getEnabled(), pdpClientDTO.getEnabled());
        assertEquals(createdPdpClientDTO.getContract().getContractNumber(), pdpClientDTO.getContract().getContractNumber());
        assertEquals(createdPdpClientDTO.getContract().getContractName(), pdpClientDTO.getContract().getContractName());
        assertEquals(createdPdpClientDTO.getRole(), pdpClientDTO.getRole());
    }

    @Test
    public void testCreateDuplicateClient() throws Exception {
        PdpClientDTO pdpClientDTO = new PdpClientDTO();
        pdpClientDTO.setClientId(TEST_CLIENT);
        pdpClientDTO.setEnabled(true);
        pdpClientDTO.setContract(buildContractDTO());
        pdpClientDTO.setRole(ADMIN_ROLE);
        Role role = roleService.findRoleByName(ADMIN_ROLE);
        pdpClientDTO.setRole(role.getName());

        ObjectMapper mapper = getMapper();

        this.mockMvc.perform(
                post(API_PREFIX_V1 + ADMIN_PREFIX + CLIENT_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(pdpClientDTO))
                        .header("Authorization", "Bearer " + token));

        this.mockMvc.perform(
                        post(API_PREFIX_V1 + ADMIN_PREFIX + CLIENT_URL)
                                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(pdpClientDTO))
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(500))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text",
                        Is.is("An internal error occurred")));
        PdpClient anotherPdpClient = pdpClientRepository.findByClientId(("anotherEmail@test.com"));
        dataSetup.queueForCleanup(anotherPdpClient);
    }

    @Test
    public void testUpdateClient() throws Exception {
        PdpClientDTO pdpClientDTO = new PdpClientDTO();
        pdpClientDTO.setClientId(TEST_CLIENT);
        pdpClientDTO.setOrganization(TEST_ORG);
        pdpClientDTO.setEnabled(true);
        pdpClientDTO.setContract(buildContractDTO());
        pdpClientDTO.setRole(ADMIN_ROLE);

        ObjectMapper mapper = getMapper();

        MvcResult mvcResult = this.mockMvc.perform(
                        post(API_PREFIX_V1 + ADMIN_PREFIX + CLIENT_URL)
                                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(pdpClientDTO))
                                .header("Authorization", "Bearer " + token))
                .andReturn();

        String result = mvcResult.getResponse().getContentAsString();
        PdpClientDTO createdPdpClientDTO = mapper.readValue(result, PdpClientDTO.class);

        createdPdpClientDTO.setRole("SPONSOR");
        createdPdpClientDTO.setEnabled(false);
        createdPdpClientDTO.getContract().setContractNumber(pdpClientDTO.getContract().getContractNumber());
        createdPdpClientDTO.setRole(SPONSOR_ROLE);

        MvcResult updateMvcResult = this.mockMvc.perform(
                        put(API_PREFIX_V1 + ADMIN_PREFIX + CLIENT_URL)
                                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(createdPdpClientDTO))
                                .header("Authorization", "Bearer " + token))
                .andReturn();

        String updateResult = updateMvcResult.getResponse().getContentAsString();
        PdpClientDTO updatedPdpClientDTO = mapper.readValue(updateResult, PdpClientDTO.class);

        assertEquals(updatedPdpClientDTO.getClientId(), createdPdpClientDTO.getClientId());
        assertEquals(updatedPdpClientDTO.getEnabled(), createdPdpClientDTO.getEnabled());
        assertEquals(updatedPdpClientDTO.getContract().getContractNumber(), createdPdpClientDTO.getContract().getContractNumber());
        assertEquals(updatedPdpClientDTO.getRole(), createdPdpClientDTO.getRole());
    }

    @Test
    public void testUpdateNonExistentClient() throws Exception {
        PdpClientDTO pdpClientDTO = createClient();

        ObjectMapper mapper = getMapper();

        this.mockMvc.perform(
                        put(API_PREFIX_V1 + ADMIN_PREFIX + CLIENT_URL)
                                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(pdpClientDTO))
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(404));
    }

    private PdpClientDTO createClient() {
        PdpClientDTO pdpClientDTO = new PdpClientDTO();
        pdpClientDTO.setClientId(TEST_CLIENT);
        pdpClientDTO.setEnabled(true);
        pdpClientDTO.setContract(buildContractDTO());
        pdpClientDTO.setRole(SPONSOR_ROLE);

        return pdpClientDTO;
    }

    @Test
    public void testCreateClientsJobByContractOnAdminBehalf() throws Exception {
        setupClient("regularClient", true);

        MvcResult mvcResult = this.mockMvc.perform(
                        post(API_PREFIX_V1 + ADMIN_PREFIX + "/job/Z0000")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                .andReturn();

        assertEquals(202, mvcResult.getResponse().getStatus());

        String header = mvcResult.getResponse().getHeader(CONTENT_LOCATION);

        String jobId = header.substring(header.indexOf("/Job/") + 5, header.indexOf("/$status"));
        StartJobDTO startJobDTO = jobClientMock.lookupJob(jobId);
        PdpClient jobPdpClient = pdpClientRepository.findAll().stream()
                .filter(pdp -> startJobDTO.getOrganization().equals(pdp.getOrganization())).findFirst().get();
        jobClientMock.cleanup(jobId);
        dataSetup.queueForCleanup(jobPdpClient);
        assertEquals("regularClient", jobPdpClient.getClientId());
    }

    @Test
    public void enableClient() throws Exception {
        // Ensure client is in right state first
        setupClient(ENABLE_DISABLE_CLIENT, false);

        MvcResult mvcResult = this.mockMvc.perform(
                        put(API_PREFIX_V1 + ADMIN_PREFIX + CLIENT_URL + "/" + ENABLE_DISABLE_CONTRACT + "/enable")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                .andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());

        ObjectMapper mapper = getMapper();

        String updateResult = mvcResult.getResponse().getContentAsString();
        PdpClientDTO updatedPdpClientDTO = mapper.readValue(updateResult, PdpClientDTO.class);

        assertEquals(true, updatedPdpClientDTO.getEnabled());
    }

    @Test
    public void enableClientNotFound() throws Exception {
        this.mockMvc.perform(
                        put(API_PREFIX_V1 + ADMIN_PREFIX + CLIENT_URL + "/badclient/enable")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(404));
    }

    @Test
    public void disableClient() throws Exception {
        // Ensure client is in right state first
        setupClient(ENABLE_DISABLE_CLIENT, true);

        MvcResult mvcResult = this.mockMvc.perform(
                        put(API_PREFIX_V1 + ADMIN_PREFIX + CLIENT_URL + "/" + ENABLE_DISABLE_CONTRACT + "/disable")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                .andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());

        ObjectMapper mapper = getMapper();

        String updateResult = mvcResult.getResponse().getContentAsString();
        PdpClientDTO updatedPdpClientDTO = mapper.readValue(updateResult, PdpClientDTO.class);

        assertEquals(false, updatedPdpClientDTO.getEnabled());
    }

    @Test
    public void disableClientNotFound() throws Exception {
        this.mockMvc.perform(
                        put(API_PREFIX_V1 + ADMIN_PREFIX + CLIENT_URL + "/badclient/disable")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(404));
    }

    @Test
    public void getClient() throws Exception {
        // Ensure client is in right state first
        setupClient(ENABLE_DISABLE_CLIENT, true);

        MvcResult mvcResult = this.mockMvc.perform(
                        get(API_PREFIX_V1 + ADMIN_PREFIX + CLIENT_URL + "/" + ENABLE_DISABLE_CONTRACT)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                .andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());

        ObjectMapper mapper = getMapper();

        String getResult = mvcResult.getResponse().getContentAsString();
        PdpClientDTO pdpClientDTO = mapper.readValue(getResult, PdpClientDTO.class);

        assertEquals(ENABLE_DISABLE_CLIENT, pdpClientDTO.getClientId());
        assertEquals(true, pdpClientDTO.getEnabled());
        ContractDTO contractDTO = pdpClientDTO.getContract();
        assertEquals(ENABLE_DISABLE_CONTRACT, contractDTO.getContractNumber());
        assertEquals("Test Contract Z0000", contractDTO.getContractName());
        assertNotNull(contractDTO.getAttestedOn());
    }

    @Test
    public void getClientNotFound() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(
                        get(API_PREFIX_V1 + ADMIN_PREFIX + CLIENT_URL + "/clientNotFound")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                .andReturn();

        assertEquals(404, mvcResult.getResponse().getStatus());
    }

    private void setupClient(String clientId, boolean enabled) {
        Contract contract = dataSetup.setupContract(ENABLE_DISABLE_CONTRACT);
        PdpClient pdpClient = new PdpClient();
        pdpClient.setClientId(clientId);
        pdpClient.setOrganization(TEST_ORG);
        pdpClient.setEnabled(enabled);
        pdpClient.setContractId(contract.getId());

        PdpClient savedPdpClient = pdpClientRepository.save(pdpClient);
        dataSetup.queueForCleanup(savedPdpClient);
    }

    private ContractDTO buildContractDTO() {
        return new ContractDTO(null, VALID_CONTRACT_NUMBER, "Test Contract " + VALID_CONTRACT_NUMBER, null, null);
    }
}
