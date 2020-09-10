package gov.cms.ab2d.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.common.dto.ContractDTO;
import gov.cms.ab2d.common.dto.SponsorDTO;
import gov.cms.ab2d.common.dto.UserDTO;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.model.Role;
import gov.cms.ab2d.common.service.RoleService;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Set;

import static gov.cms.ab2d.common.util.Constants.*;
import static gov.cms.ab2d.common.util.Constants.ADMIN_ROLE;
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

    @Autowired
    private DataSetup dataSetup;

    @Autowired
    private RoleService roleService;

    private String token;

    private static final String USER_URL = "/user";

    private static final String ENABLE_DISABLE_USER = "enableDisableUser";

    @BeforeEach
    public void setup() throws JwtVerificationException {
        contractRepository.deleteAll();
        jobRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        sponsorRepository.deleteAll();

        token = testUtil.setupToken(List.of(ADMIN_ROLE, SPONSOR_ROLE));
    }

    @Test
    public void testCreateUser() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("test@test.com");
        userDTO.setEmail("test@test.com");
        userDTO.setEnabled(true);
        userDTO.setFirstName("Test");
        userDTO.setLastName("User");
        Sponsor sponsor = sponsorRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();
        userDTO.setSponsor(new SponsorDTO(sponsor.getHpmsId(), sponsor.getOrgName()));
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
        Assert.assertEquals(createdUserDTO.getSponsor().getHpmsId(), userDTO.getSponsor().getHpmsId());
        Assert.assertEquals(createdUserDTO.getSponsor().getOrgName(), userDTO.getSponsor().getOrgName());
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
        Sponsor sponsor = sponsorRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();
        userDTO.setSponsor(new SponsorDTO(sponsor.getHpmsId(), sponsor.getOrgName()));
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
        Sponsor sponsor = sponsorRepository.findByHpmsIdAndOrgName(123, "Test").get();
        userDTO.setSponsor(new SponsorDTO(sponsor.getHpmsId(), sponsor.getOrgName()));
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
        createdUserDTO.getSponsor().setHpmsId(sponsor.getParent().getHpmsId());
        createdUserDTO.getSponsor().setOrgName(sponsor.getParent().getOrgName());
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
        Assert.assertEquals(updatedUserDTO.getSponsor().getHpmsId(), createdUserDTO.getSponsor().getHpmsId());
        Assert.assertEquals(updatedUserDTO.getSponsor().getOrgName(), createdUserDTO.getSponsor().getOrgName());
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
        Sponsor sponsor = sponsorRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();
        userDTO.setSponsor(new SponsorDTO(sponsor.getHpmsId(), sponsor.getOrgName()));
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
        Assert.assertEquals(userDTO.getSponsor().getHpmsId(), Integer.valueOf(123543));
        Assert.assertEquals(userDTO.getSponsor().getOrgName(), "Test 1");
        ContractDTO contractDTO = userDTO.getContracts().iterator().next();
        Assert.assertEquals(contractDTO.getContractNumber(), "Z0000");
        Assert.assertEquals(contractDTO.getContractName(), "Test Contract");
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
        Sponsor savedSponsor = dataSetup.createSponsor("Parent Corp. 1", 34534, "Test 1", 123543);
        Contract contract = dataSetup.setupContract(savedSponsor, "Z0000");
        savedSponsor.setContracts(Set.of(contract));
        User user = new User();
        user.setUsername(ENABLE_DISABLE_USER);
        user.setEmail("test@test.com");
        user.setFirstName("test");
        user.setLastName("user");
        user.setEnabled(enabled);
        user.setSponsor(savedSponsor);

        userRepository.save(user);
    }
}
