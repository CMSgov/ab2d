package gov.cms.ab2d.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.common.dto.ContractDTO;
import gov.cms.ab2d.common.dto.UserDTO;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.model.Role;
import gov.cms.ab2d.common.service.RoleService;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.eventlogger.reports.sql.LoggerEventRepository;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static gov.cms.ab2d.api.controller.common.ApiText.CONT_LOC;
import static gov.cms.ab2d.common.util.Constants.*;
import static gov.cms.ab2d.common.util.Constants.ADMIN_ROLE;
import static gov.cms.ab2d.common.util.DataSetup.VALID_CONTRACT_NUMBER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
public class AdminAPIUserTests {

    public static final String TEST_USER = "test@test.com";
    private static final String USER_URL = "/user";
    private static final String ENABLE_DISABLE_USER = "enableDisableUser";
    private static final String ENABLE_DISABLE_CONTRACT = "Z0000";

    @Autowired
    private MockMvc mockMvc;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private TestUtil testUtil;

    @Autowired
    private DataSetup dataSetup;

    @Autowired
    private LoggerEventRepository loggerEventRepository;

    @Autowired
    private RoleService roleService;

    private String token;



    @BeforeEach
    public void setup() throws JwtVerificationException {
        token = testUtil.setupToken(List.of(ADMIN_ROLE, SPONSOR_ROLE, ATTESTOR_ROLE));
    }

    @AfterEach
    public void cleanup() {
        dataSetup.queueForCleanup(userRepository.findByUsername(TEST_USER));
        dataSetup.cleanup();
        loggerEventRepository.delete();
    }

    @Test
    public void testCreateUser() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(TEST_USER);
        userDTO.setEmail(TEST_USER);
        userDTO.setEnabled(true);
        userDTO.setFirstName("Test");
        userDTO.setLastName("User");
        userDTO.setContract(buildContractDTO(VALID_CONTRACT_NUMBER));
        userDTO.setRole(ADMIN_ROLE);
        Role role = roleService.findRoleByName(ADMIN_ROLE);
        userDTO.setRole(role.getName());

        ObjectMapper mapper = new ObjectMapper();

        MvcResult mvcResult = this.mockMvc.perform(
                post(API_PREFIX_V1 + ADMIN_PREFIX + USER_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(userDTO))
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        assertEquals(201, mvcResult.getResponse().getStatus());

        String result = mvcResult.getResponse().getContentAsString();
        UserDTO createdUserDTO = mapper.readValue(result, UserDTO.class);
        assertEquals(createdUserDTO.getEmail(), userDTO.getEmail());
        assertEquals(createdUserDTO.getUsername(), userDTO.getUsername());
        assertEquals(createdUserDTO.getFirstName(), userDTO.getFirstName());
        assertEquals(createdUserDTO.getLastName(), userDTO.getLastName());
        assertEquals(createdUserDTO.getEnabled(), userDTO.getEnabled());
        assertEquals(createdUserDTO.getContract().getContractNumber(), userDTO.getContract().getContractNumber());
        assertEquals(createdUserDTO.getContract().getContractName(), userDTO.getContract().getContractName());
        assertEquals(createdUserDTO.getRole(), userDTO.getRole());
    }

    @Test
    public void testCreateUserAttestor() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(TEST_USER);
        userDTO.setEmail(TEST_USER);
        userDTO.setEnabled(true);
        userDTO.setFirstName("Test");
        userDTO.setLastName("User");
        userDTO.setContract(buildContractDTO(VALID_CONTRACT_NUMBER));
        Role role = roleService.findRoleByName(ATTESTOR_ROLE);
        userDTO.setRole(role.getName());

        ObjectMapper mapper = new ObjectMapper();

        MvcResult mvcResult = this.mockMvc.perform(
                post(API_PREFIX_V1 + ADMIN_PREFIX + USER_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(userDTO))
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        assertEquals(201, mvcResult.getResponse().getStatus());

        String result = mvcResult.getResponse().getContentAsString();
        UserDTO createdUserDTO = mapper.readValue(result, UserDTO.class);
        assertEquals(createdUserDTO.getEmail(), userDTO.getEmail());
        assertEquals(createdUserDTO.getUsername(), userDTO.getUsername());
        assertEquals(createdUserDTO.getFirstName(), userDTO.getFirstName());
        assertEquals(createdUserDTO.getLastName(), userDTO.getLastName());
        assertEquals(createdUserDTO.getEnabled(), userDTO.getEnabled());
        assertEquals(createdUserDTO.getContract().getContractNumber(), userDTO.getContract().getContractNumber());
        assertEquals(createdUserDTO.getContract().getContractName(), userDTO.getContract().getContractName());
        assertEquals(createdUserDTO.getRole(), userDTO.getRole());
    }

    @Test
    public void testCreateDuplicateUser() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(TEST_USER);
        userDTO.setEmail(TEST_USER);
        userDTO.setEnabled(true);
        userDTO.setFirstName("Test");
        userDTO.setLastName("User");
        userDTO.setContract(buildContractDTO(VALID_CONTRACT_NUMBER));
        userDTO.setRole(ADMIN_ROLE);
        Role role = roleService.findRoleByName(ADMIN_ROLE);
        userDTO.setRole(role.getName());

        ObjectMapper mapper = new ObjectMapper();

        this.mockMvc.perform(
                post(API_PREFIX_V1 + ADMIN_PREFIX + USER_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(userDTO))
                        .header("Authorization", "Bearer " + token));

        userDTO.setEmail("anotherEmail@test.com");

        this.mockMvc.perform(
                post(API_PREFIX_V1 + ADMIN_PREFIX + USER_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(userDTO))
                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().is(500))
                        .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                        .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                        .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                        .andExpect(jsonPath("$.issue[0].details.text",
                            Is.is("An internal error occurred")));
        User anotherUser = userRepository.findByUsername(("anotherEmail@test.com"));
        dataSetup.queueForCleanup(anotherUser);
    }

    @Test
    public void testUpdateUser() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(TEST_USER);
        userDTO.setEmail(TEST_USER);
        userDTO.setEnabled(true);
        userDTO.setFirstName("Test");
        userDTO.setLastName("User");
        userDTO.setContract(buildContractDTO(VALID_CONTRACT_NUMBER));
        userDTO.setRole(ADMIN_ROLE);

        ObjectMapper mapper = new ObjectMapper();

        MvcResult mvcResult = this.mockMvc.perform(
                post(API_PREFIX_V1 + ADMIN_PREFIX + USER_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(userDTO))
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        String result = mvcResult.getResponse().getContentAsString();
        UserDTO createdUserDTO = mapper.readValue(result, UserDTO.class);

        createdUserDTO.setRole("SPONSOR");
        createdUserDTO.setEmail("updated@test.com");
        createdUserDTO.setEnabled(false);
        createdUserDTO.setFirstName("Updated");
        createdUserDTO.setLastName("Username");
        createdUserDTO.getContract().setContractNumber(userDTO.getContract().getContractNumber());
        createdUserDTO.setRole(SPONSOR_ROLE);

        MvcResult updateMvcResult = this.mockMvc.perform(
                put(API_PREFIX_V1 + ADMIN_PREFIX + USER_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(createdUserDTO))
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        String updateResult = updateMvcResult.getResponse().getContentAsString();
        UserDTO updatedUserDTO = mapper.readValue(updateResult, UserDTO.class);

        assertEquals(updatedUserDTO.getEmail(), createdUserDTO.getEmail());
        assertEquals(updatedUserDTO.getUsername(), createdUserDTO.getUsername());
        assertEquals(updatedUserDTO.getFirstName(), createdUserDTO.getFirstName());
        assertEquals(updatedUserDTO.getLastName(), createdUserDTO.getLastName());
        assertEquals(updatedUserDTO.getEnabled(), createdUserDTO.getEnabled());
        assertEquals(updatedUserDTO.getContract().getContractNumber(), createdUserDTO.getContract().getContractNumber());
        assertEquals(updatedUserDTO.getRole(), createdUserDTO.getRole());
    }

    @Test
    public void testUpdateNonExistentUser() throws Exception {
        UserDTO userDTO = createUser();

        ObjectMapper mapper = new ObjectMapper();

        this.mockMvc.perform(
                put(API_PREFIX_V1 + ADMIN_PREFIX + USER_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(userDTO))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(404));
    }

    private UserDTO createUser() {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(TEST_USER);
        userDTO.setEmail(TEST_USER);
        userDTO.setEnabled(true);
        userDTO.setFirstName("Test");
        userDTO.setLastName("User");
        userDTO.setContract(buildContractDTO(VALID_CONTRACT_NUMBER));
        userDTO.setRole(SPONSOR_ROLE);

        return userDTO;
    }

    @Test
    public void testCreateUsersJobByContractOnAdminBehalf() throws Exception {
        setupUser("regularUser", true);

        MvcResult mvcResult = this.mockMvc.perform(
                post(API_PREFIX_V1 + ADMIN_PREFIX + "/job/Z0000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        assertEquals(202, mvcResult.getResponse().getStatus());

        String header = mvcResult.getResponse().getHeader(CONT_LOC);

        Job job = jobRepository.findByJobUuid(header.substring(header.indexOf("/Job/") + 5, header.indexOf("/$status")));
        User jobUser = job.getUser();
        dataSetup.queueForCleanup(jobUser);
        dataSetup.queueForCleanup(job);
        assertEquals("regularUser", jobUser.getUsername());
    }

    @Test
    public void enableUser() throws Exception {
        // Ensure user is in right state first
        setupUser(ENABLE_DISABLE_USER, false);

        MvcResult mvcResult = this.mockMvc.perform(
                put(API_PREFIX_V1 + ADMIN_PREFIX + USER_URL + "/" + ENABLE_DISABLE_CONTRACT + "/enable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());

        ObjectMapper mapper = new ObjectMapper();

        String updateResult = mvcResult.getResponse().getContentAsString();
        UserDTO updatedUserDTO = mapper.readValue(updateResult, UserDTO.class);

        assertEquals(true, updatedUserDTO.getEnabled());
    }

    @Test
    public void enableUserNotFound() throws Exception {
        this.mockMvc.perform(
                put(API_PREFIX_V1 + ADMIN_PREFIX + USER_URL + "/baduser/enable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(404));
    }

    @Test
    public void disableUser() throws Exception {
        // Ensure user is in right state first
        setupUser(ENABLE_DISABLE_USER, true);

        MvcResult mvcResult = this.mockMvc.perform(
                put(API_PREFIX_V1 + ADMIN_PREFIX + USER_URL + "/" + ENABLE_DISABLE_CONTRACT + "/disable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());

        ObjectMapper mapper = new ObjectMapper();

        String updateResult = mvcResult.getResponse().getContentAsString();
        UserDTO updatedUserDTO = mapper.readValue(updateResult, UserDTO.class);

        assertEquals(false, updatedUserDTO.getEnabled());
    }

    @Test
    public void disableUserNotFound() throws Exception {
        this.mockMvc.perform(
                put(API_PREFIX_V1 + ADMIN_PREFIX + USER_URL + "/baduser/disable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(404));
    }

    @Test
    public void getUser() throws Exception {
        // Ensure user is in right state first
        setupUser(ENABLE_DISABLE_USER, true);

        MvcResult mvcResult = this.mockMvc.perform(
                get(API_PREFIX_V1 + ADMIN_PREFIX + USER_URL + "/" + ENABLE_DISABLE_CONTRACT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());

        ObjectMapper mapper = new ObjectMapper();

        String getResult = mvcResult.getResponse().getContentAsString();
        UserDTO userDTO = mapper.readValue(getResult, UserDTO.class);

        assertEquals(TEST_USER, userDTO.getEmail());
        assertEquals(ENABLE_DISABLE_USER, userDTO.getUsername());
        assertEquals("test", userDTO.getFirstName());
        assertEquals("user", userDTO.getLastName());
        assertEquals(true, userDTO.getEnabled());
        ContractDTO contractDTO = userDTO.getContract();
        assertEquals(ENABLE_DISABLE_CONTRACT, contractDTO.getContractNumber());
        assertEquals("Test Contract Z0000", contractDTO.getContractName());
        assertNotNull(contractDTO.getAttestedOn());
    }

    @Test
    public void getUserNotFound() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(
                get(API_PREFIX_V1 + ADMIN_PREFIX + USER_URL + "/userNotFound")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        assertEquals(404, mvcResult.getResponse().getStatus());
    }

    private void setupUser(String username, boolean enabled) {
        Contract contract = dataSetup.setupContract(ENABLE_DISABLE_CONTRACT);
        User user = new User();
        user.setUsername(username);
        user.setEmail(TEST_USER);
        user.setFirstName("test");
        user.setLastName("user");
        user.setEnabled(enabled);
        user.setContract(contract);

        User savedUser = userRepository.save(user);
        dataSetup.queueForCleanup(savedUser);
    }

    private ContractDTO buildContractDTO(String contractNumber) {

        ContractDTO contractDTO = new ContractDTO();
        contractDTO.setContractNumber(VALID_CONTRACT_NUMBER);
        contractDTO.setContractName("Test Contract " + VALID_CONTRACT_NUMBER);
        return contractDTO;
    }
}
