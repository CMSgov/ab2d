package gov.cms.ab2d.api.controller;

import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.repository.RoleRepository;
import gov.cms.ab2d.common.repository.SponsorRepository;
import gov.cms.ab2d.common.repository.UserRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static gov.cms.ab2d.api.controller.TestUtil.TEST_USER;
import static gov.cms.ab2d.api.util.Constants.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class AuthenticationTests {

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

    private String token;

    @Before
    public void setup() throws JwtVerificationException {
        jobRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        sponsorRepository.deleteAll();

        token = testUtil.setupToken(List.of(SPONSOR_ROLE));
    }

    // Negative tests, successful auth tests are essentially done in other suites
    @Test
    public void testNoAuthHeader() throws Exception {
        this.mockMvc.perform(get(API_PREFIX + FHIR_PREFIX + "/Patient/$export")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(401));
    }

    @Test
    public void testBadStartToHeader() throws Exception {
        this.mockMvc.perform(get(API_PREFIX + FHIR_PREFIX + "/Patient/$export")
                .header("Authorization", "NotBearer")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(401));
    }

    @Test
    public void testNoTokenInHeader() throws Exception {
        this.mockMvc.perform(get(API_PREFIX + FHIR_PREFIX + "/Patient/$export")
                .header("Authorization", "Bearer ")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(401));
    }

    @Test
    public void testUserDoesNotExist() throws Exception {
        User user = userRepository.findByUsername(TEST_USER);
        userRepository.delete(user);

        this.mockMvc.perform(get(API_PREFIX + FHIR_PREFIX + "/Patient/$export")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(403));
    }

    @Test
    public void testUserIsNotEnabled() throws Exception {
        User user = userRepository.findByUsername(TEST_USER);
        user.setEnabled(false);
        userRepository.save(user);

        this.mockMvc.perform(get(API_PREFIX + FHIR_PREFIX + "/Patient/$export")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(403));
    }
}
