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
import org.hamcrest.core.Is;
import org.junit.Assert;
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

import static gov.cms.ab2d.common.util.Constants.*;
import static gov.cms.ab2d.common.util.Constants.ADMIN_ROLE;
import static gov.cms.ab2d.common.util.DataSetup.VALID_CONTRACT_NUMBER;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
public class AdminAPIUserTests {

    @Autowired
    private MockMvc mockMvc;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

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

    @Autowired
    private DataSetup dataSetup;

    @Autowired
    private RoleService roleService;

    private String token;

    private static final String USER_URL = "/user";

    private static final String ENABLE_DISABLE_USER = "enableDisableUser";

    @BeforeEach
    public void setup() throws JwtVerificationException {
        jobRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        contractRepository.deleteAll();

        token = testUtil.setupToken(List.of(ADMIN_ROLE, SPONSOR_ROLE, ATTESTOR_ROLE));
    }

    @Test
    public void testCreateUser() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("test@test.com");
        userDTO.setEmail("test@test.com");
        userDTO.setEnabled(true);
        userDTO.setFirstName("Test");
        userDTO.setLastName("User");
        userDTO.setContract(buildContractDTO());
        userDTO.setRole(ADMIN_ROLE);
        Role role = roleService.findRoleByName(ADMIN_ROLE);
        userDTO.setRole(role.getName());

        ObjectMapper mapper = new ObjectMapper();

        MvcResult mvcResult = this.mockMvc.perform(
                post(API_PREFIX + ADMIN_PREFIX + USER_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(userDTO))
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        Assert.assertEquals(201, mvcResult.getResponse().getStatus());

        String result = mvcResult.getResponse().getContentAsString();
        UserDTO createdUserDTO = mapper.readValue(result, UserDTO.class);
        Assert.assertEquals(createdUserDTO.getEmail(), userDTO.getEmail());
        Assert.assertEquals(createdUserDTO.getUsername(), userDTO.getUsername());
        Assert.assertEquals(createdUserDTO.getFirstName(), userDTO.getFirstName());
        Assert.assertEquals(createdUserDTO.getLastName(), userDTO.getLastName());
        Assert.assertEquals(createdUserDTO.getEnabled(), userDTO.getEnabled());
        Assert.assertEquals(createdUserDTO.getContract().getContractNumber(), userDTO.getContract().getContractNumber());
        Assert.assertEquals(createdUserDTO.getContract().getContractName(), userDTO.getContract().getContractName());
        Assert.assertEquals(createdUserDTO.getRole(), userDTO.getRole());
    }

    @Test
    public void testCreateUserAttestor() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("test@test.com");
        userDTO.setEmail("test@test.com");
        userDTO.setEnabled(true);
        userDTO.setFirstName("Test");
        userDTO.setLastName("User");
        userDTO.setContract(buildContractDTO());
        Role role = roleService.findRoleByName(ATTESTOR_ROLE);
        userDTO.setRole(role.getName());

        ObjectMapper mapper = new ObjectMapper();

        MvcResult mvcResult = this.mockMvc.perform(
                post(API_PREFIX + ADMIN_PREFIX + USER_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(userDTO))
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        Assert.assertEquals(201, mvcResult.getResponse().getStatus());

        String result = mvcResult.getResponse().getContentAsString();
        UserDTO createdUserDTO = mapper.readValue(result, UserDTO.class);
        Assert.assertEquals(createdUserDTO.getEmail(), userDTO.getEmail());
        Assert.assertEquals(createdUserDTO.getUsername(), userDTO.getUsername());
        Assert.assertEquals(createdUserDTO.getFirstName(), userDTO.getFirstName());
        Assert.assertEquals(createdUserDTO.getLastName(), userDTO.getLastName());
        Assert.assertEquals(createdUserDTO.getEnabled(), userDTO.getEnabled());
        Assert.assertEquals(createdUserDTO.getContract().getContractNumber(), userDTO.getContract().getContractNumber());
        Assert.assertEquals(createdUserDTO.getContract().getContractName(), userDTO.getContract().getContractName());
        Assert.assertEquals(createdUserDTO.getRole(), userDTO.getRole());
    }

    @Test
    public void testCreateDuplicateUser() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("test@test.com");
        userDTO.setEmail("test@test.com");
        userDTO.setEnabled(true);
        userDTO.setFirstName("Test");
        userDTO.setLastName("User");
        userDTO.setContract(buildContractDTO());
        userDTO.setRole(ADMIN_ROLE);
        Role role = roleService.findRoleByName(ADMIN_ROLE);
        userDTO.setRole(role.getName());

        ObjectMapper mapper = new ObjectMapper();

        this.mockMvc.perform(
                post(API_PREFIX + ADMIN_PREFIX + USER_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(userDTO))
                        .header("Authorization", "Bearer " + token));

        userDTO.setEmail("anotherEmail@test.com");

        this.mockMvc.perform(
                post(API_PREFIX + ADMIN_PREFIX + USER_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(userDTO))
                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().is(500))
                        .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                        .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                        .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                        .andExpect(jsonPath("$.issue[0].details.text",
                            Is.is("An internal error occurred")));
    }

    @Test
    public void testUpdateUser() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("test@test.com");
        userDTO.setEmail("test@test.com");
        userDTO.setEnabled(true);
        userDTO.setFirstName("Test");
        userDTO.setLastName("User");
        userDTO.setContract(buildContractDTO());
        userDTO.setRole(ADMIN_ROLE);

        ObjectMapper mapper = new ObjectMapper();

        MvcResult mvcResult = this.mockMvc.perform(
                post(API_PREFIX + ADMIN_PREFIX + USER_URL)
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
                put(API_PREFIX + ADMIN_PREFIX + USER_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(createdUserDTO))
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        String updateResult = updateMvcResult.getResponse().getContentAsString();
        UserDTO updatedUserDTO = mapper.readValue(updateResult, UserDTO.class);

        Assert.assertEquals(updatedUserDTO.getEmail(), createdUserDTO.getEmail());
        Assert.assertEquals(updatedUserDTO.getUsername(), createdUserDTO.getUsername());
        Assert.assertEquals(updatedUserDTO.getFirstName(), createdUserDTO.getFirstName());
        Assert.assertEquals(updatedUserDTO.getLastName(), createdUserDTO.getLastName());
        Assert.assertEquals(updatedUserDTO.getEnabled(), createdUserDTO.getEnabled());
        Assert.assertEquals(updatedUserDTO.getContract().getContractNumber(), createdUserDTO.getContract().getContractNumber());
        Assert.assertEquals(updatedUserDTO.getRole(), createdUserDTO.getRole());
    }

    @Test
    public void testUpdateNonExistentUser() throws Exception {
        UserDTO userDTO = createUser();

        ObjectMapper mapper = new ObjectMapper();

        this.mockMvc.perform(
                put(API_PREFIX + ADMIN_PREFIX + USER_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(userDTO))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(404));
    }

    private UserDTO createUser() {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("test@test.com");
        userDTO.setEmail("test@test.com");
        userDTO.setEnabled(true);
        userDTO.setFirstName("Test");
        userDTO.setLastName("User");
        userDTO.setContract(buildContractDTO());
        userDTO.setRole(SPONSOR_ROLE);

        return userDTO;
    }

    @Test
    public void testCreateUsersJobOnAdminBehalf() throws Exception {
        UserDTO userDTO = createUser();

        ObjectMapper mapper = new ObjectMapper();

        this.mockMvc.perform(
                post(API_PREFIX + ADMIN_PREFIX + USER_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(userDTO))
                        .header("Authorization", "Bearer " + token));

        MvcResult mvcResult = this.mockMvc.perform(
                post(API_PREFIX + ADMIN_PREFIX + USER_URL + "/" + userDTO.getUsername() + "/job")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        Assert.assertEquals(mvcResult.getResponse().getStatus(), 202);

        String header = mvcResult.getResponse().getHeader("Content-Location");

        Job job = jobRepository.findByJobUuid(header.substring(header.indexOf("/Job/") + 5, header.indexOf("/$status")));
        User jobUser = job.getUser();
        Assert.assertEquals(jobUser.getUsername(), userDTO.getUsername());
    }

    @Test
    public void testCreateUsersJobByContractOnAdminBehalf() throws Exception {
        UserDTO userDTO = createUser();

        ObjectMapper mapper = new ObjectMapper();

        this.mockMvc.perform(
                post(API_PREFIX + ADMIN_PREFIX + USER_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(userDTO))
                        .header("Authorization", "Bearer " + token));

        MvcResult mvcResult = this.mockMvc.perform(
                post(API_PREFIX + ADMIN_PREFIX + USER_URL + "/" + userDTO.getUsername() + "/job/ABC123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        Assert.assertEquals(mvcResult.getResponse().getStatus(), 202);

        String header = mvcResult.getResponse().getHeader("Content-Location");

        Job job = jobRepository.findByJobUuid(header.substring(header.indexOf("/Job/") + 5, header.indexOf("/$status")));
        User jobUser = job.getUser();
        Assert.assertEquals(jobUser.getUsername(), userDTO.getUsername());
    }

    @Test
    public void enableUser() throws Exception {
        // Ensure user is in right state first
        setupUser(false);

        MvcResult mvcResult = this.mockMvc.perform(
                put(API_PREFIX + ADMIN_PREFIX + USER_URL + "/" + ENABLE_DISABLE_USER + "/enable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        Assert.assertEquals(mvcResult.getResponse().getStatus(), 200);

        ObjectMapper mapper = new ObjectMapper();

        String updateResult = mvcResult.getResponse().getContentAsString();
        UserDTO updatedUserDTO = mapper.readValue(updateResult, UserDTO.class);

        Assert.assertEquals(updatedUserDTO.getEnabled(), true);
    }

    @Test
    public void enableUserNotFound() throws Exception {
        this.mockMvc.perform(
                put(API_PREFIX + ADMIN_PREFIX + USER_URL + "/baduser/enable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(404));
    }

    @Test
    public void disableUser() throws Exception {
        // Ensure user is in right state first
        setupUser(true);

        MvcResult mvcResult = this.mockMvc.perform(
                put(API_PREFIX + ADMIN_PREFIX + USER_URL + "/" + ENABLE_DISABLE_USER + "/disable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        Assert.assertEquals(mvcResult.getResponse().getStatus(), 200);

        ObjectMapper mapper = new ObjectMapper();

        String updateResult = mvcResult.getResponse().getContentAsString();
        UserDTO updatedUserDTO = mapper.readValue(updateResult, UserDTO.class);

        Assert.assertEquals(updatedUserDTO.getEnabled(), false);
    }

    @Test
    public void disableUserNotFound() throws Exception {
        this.mockMvc.perform(
                put(API_PREFIX + ADMIN_PREFIX + USER_URL + "/baduser/disable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(404));
    }

    @Test
    public void getUser() throws Exception {
        // Ensure user is in right state first
        setupUser(true);

        MvcResult mvcResult = this.mockMvc.perform(
                get(API_PREFIX + ADMIN_PREFIX + USER_URL + "/" + ENABLE_DISABLE_USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        Assert.assertEquals(mvcResult.getResponse().getStatus(), 200);

        ObjectMapper mapper = new ObjectMapper();

        String getResult = mvcResult.getResponse().getContentAsString();
        UserDTO userDTO = mapper.readValue(getResult, UserDTO.class);

        Assert.assertEquals(userDTO.getEmail(), "test@test.com");
        Assert.assertEquals(userDTO.getUsername(), ENABLE_DISABLE_USER);
        Assert.assertEquals(userDTO.getFirstName(), "test");
        Assert.assertEquals(userDTO.getLastName(), "user");
        Assert.assertEquals(userDTO.getEnabled(), true);
        ContractDTO contractDTO = userDTO.getContract();
        Assert.assertEquals(contractDTO.getContractNumber(), "Z0000");
        Assert.assertEquals("Test Contract Z0000", contractDTO.getContractName());
        Assert.assertNotNull(contractDTO.getAttestedOn());
    }

    @Test
    public void getUserNotFound() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(
                get(API_PREFIX + ADMIN_PREFIX + USER_URL + "/userNotFound")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        Assert.assertEquals(mvcResult.getResponse().getStatus(), 404);
    }

    private void setupUser(boolean enabled) {
        Contract contract = dataSetup.setupContract("Z0000");
        User user = new User();
        user.setUsername(ENABLE_DISABLE_USER);
        user.setEmail("test@test.com");
        user.setFirstName("test");
        user.setLastName("user");
        user.setEnabled(enabled);
        user.setContract(contract);

        userRepository.save(user);
    }

    private ContractDTO buildContractDTO() {

        ContractDTO contractDTO = new ContractDTO();
        contractDTO.setContractNumber(VALID_CONTRACT_NUMBER);
        contractDTO.setContractName("Test Contract " + VALID_CONTRACT_NUMBER);
        return contractDTO;
    }
}
