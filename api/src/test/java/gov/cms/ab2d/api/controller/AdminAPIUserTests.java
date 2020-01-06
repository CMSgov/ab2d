package gov.cms.ab2d.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.common.dto.SponsorDTO;
import gov.cms.ab2d.common.dto.UserDTO;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.model.Role;
import gov.cms.ab2d.common.service.RoleService;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
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

import static gov.cms.ab2d.api.util.Constants.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    private RoleService roleService;

    private String token;

    private static final String USER_URL = "/user";

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

        this.mockMvc.perform(
                post(API_PREFIX + ADMIN_PREFIX + USER_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(userDTO))
                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().is(500))
                        .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                        .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                        .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                        .andExpect(jsonPath("$.issue[0].details.text",
                            Is.is("PSQLException: ERROR: duplicate key value violates unique constraint \"uc_user_account_username\"\n  Detail: Key (username)=(test@test.com) already exists.")));
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

        this.mockMvc.perform(
                put(API_PREFIX + ADMIN_PREFIX + USER_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(userDTO))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(404));
    }
}
