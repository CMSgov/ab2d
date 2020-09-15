package gov.cms.ab2d.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.common.dto.SponsorDTO;
import gov.cms.ab2d.common.dto.SponsorIPDTO;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.repository.RoleRepository;
import gov.cms.ab2d.common.repository.SponsorRepository;
import gov.cms.ab2d.common.repository.UserRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
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

import static gov.cms.ab2d.common.util.Constants.ADMIN_PREFIX;
import static gov.cms.ab2d.common.util.Constants.ADMIN_ROLE;
import static gov.cms.ab2d.common.util.Constants.API_PREFIX;
import static gov.cms.ab2d.common.util.Constants.SPONSOR_ROLE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
public class AdminAPIIPTests {

    @Autowired
    private MockMvc mockMvc;

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

    private String token;

    private static final String IP_URL = "/ip";

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
    public void testCreateIPsAndGet() throws Exception {
        SponsorIPDTO sponsorIPDTO = new SponsorIPDTO();
        sponsorIPDTO.setIps(Set.of("127.0.0.1", "33.33.33.33"));
        Sponsor sponsor = sponsorRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();
        sponsorIPDTO.setSponsor(new SponsorDTO(sponsor.getHpmsId(), sponsor.getOrgName()));

        ObjectMapper mapper = new ObjectMapper();

        MvcResult mvcResult = this.mockMvc.perform(
                post(API_PREFIX + ADMIN_PREFIX + IP_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(sponsorIPDTO))
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        Assert.assertEquals(201, mvcResult.getResponse().getStatus());

        String result = mvcResult.getResponse().getContentAsString();
        SponsorIPDTO createdSponsorIPDTO = mapper.readValue(result, SponsorIPDTO.class);
        Assert.assertEquals(createdSponsorIPDTO.getIps(), sponsorIPDTO.getIps());
        Assert.assertEquals(createdSponsorIPDTO.getSponsor(), sponsorIPDTO.getSponsor());

        MvcResult mvcResultSecondCall = this.mockMvc.perform(
                get(API_PREFIX + ADMIN_PREFIX + IP_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(new SponsorDTO(sponsor.getHpmsId(), sponsor.getOrgName())))
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        Assert.assertEquals(200, mvcResultSecondCall.getResponse().getStatus());

        result = mvcResult.getResponse().getContentAsString();
        SponsorIPDTO retrievedSponsorIPDTO = mapper.readValue(result, SponsorIPDTO.class);
        Assert.assertEquals(retrievedSponsorIPDTO.getIps(), sponsorIPDTO.getIps());
        Assert.assertEquals(retrievedSponsorIPDTO.getSponsor(), sponsorIPDTO.getSponsor());
    }

    @Test
    public void testCreateIPsBadSponsor() throws Exception {
        SponsorIPDTO sponsorIPDTO = new SponsorIPDTO();
        sponsorIPDTO.setIps(Set.of("127.0.0.1", "33.33.33.33"));
        Sponsor sponsor = sponsorRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();
        sponsorIPDTO.setSponsor(new SponsorDTO(99999999, sponsor.getOrgName()));

        ObjectMapper mapper = new ObjectMapper();

        MvcResult mvcResult = this.mockMvc.perform(
                post(API_PREFIX + ADMIN_PREFIX + IP_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(sponsorIPDTO))
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        Assert.assertEquals(404, mvcResult.getResponse().getStatus());
    }

    @Test
    public void testCreateIPsBadIP() throws Exception {
        SponsorIPDTO sponsorIPDTO = new SponsorIPDTO();
        sponsorIPDTO.setIps(Set.of("notanip"));
        Sponsor sponsor = sponsorRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();
        sponsorIPDTO.setSponsor(new SponsorDTO(sponsor.getHpmsId(), sponsor.getOrgName()));

        ObjectMapper mapper = new ObjectMapper();

        MvcResult mvcResult = this.mockMvc.perform(
                post(API_PREFIX + ADMIN_PREFIX + IP_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(sponsorIPDTO))
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        Assert.assertEquals(400, mvcResult.getResponse().getStatus());
    }

    @Test
    public void testDeleteIPs() throws Exception {
        SponsorIPDTO sponsorIPDTO = new SponsorIPDTO();
        sponsorIPDTO.setIps(Set.of("127.0.0.1", "33.33.33.33", "44.44.44.44"));
        Sponsor sponsor = sponsorRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();
        sponsorIPDTO.setSponsor(new SponsorDTO(sponsor.getHpmsId(), sponsor.getOrgName()));

        ObjectMapper mapper = new ObjectMapper();

        this.mockMvc.perform(
                post(API_PREFIX + ADMIN_PREFIX + IP_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(sponsorIPDTO))
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        // Set to the ones we want to delete
        sponsorIPDTO.setIps(Set.of("33.33.33.33", "44.44.44.44"));

        MvcResult mvcResult = this.mockMvc.perform(
                delete(API_PREFIX + ADMIN_PREFIX + IP_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(sponsorIPDTO))
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        Assert.assertEquals(202, mvcResult.getResponse().getStatus());

        String result = mvcResult.getResponse().getContentAsString();
        SponsorIPDTO createdSponsorIPDTO = mapper.readValue(result, SponsorIPDTO.class);
        Assert.assertEquals(createdSponsorIPDTO.getIps(), Set.of("127.0.0.1"));
        Assert.assertEquals(createdSponsorIPDTO.getSponsor(), sponsorIPDTO.getSponsor());
    }

    @Test
    public void testDeleteNonExistantIPs() throws Exception {
        SponsorIPDTO sponsorIPDTO = new SponsorIPDTO();
        sponsorIPDTO.setIps(Set.of("127.0.0.1", "33.33.33.33", "44.44.44.44"));
        Sponsor sponsor = sponsorRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();
        sponsorIPDTO.setSponsor(new SponsorDTO(sponsor.getHpmsId(), sponsor.getOrgName()));

        ObjectMapper mapper = new ObjectMapper();

        this.mockMvc.perform(
                post(API_PREFIX + ADMIN_PREFIX + IP_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(sponsorIPDTO))
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        // Set to the ones we want to delete
        sponsorIPDTO.setIps(Set.of("55.55.55.55"));

        MvcResult mvcResult = this.mockMvc.perform(
                delete(API_PREFIX + ADMIN_PREFIX + IP_URL)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(sponsorIPDTO))
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        Assert.assertEquals(404, mvcResult.getResponse().getStatus());
    }

    @AfterEach
    public void tearDown() {
        contractRepository.deleteAll();
        jobRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        sponsorRepository.deleteAll();
    }
}
