package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.repository.SponsorRepository;
import gov.cms.ab2d.common.repository.UserRepository;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.Map;

import static gov.cms.ab2d.api.controller.TestUtil.TEST_USER;
import static gov.cms.ab2d.api.util.Constants.API_PREFIX;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
    private JobRepository jobRepository;

    private Map<String, String> headerMap;

    @Before
    public void setup() throws IOException, InterruptedException {
        jobRepository.deleteAll();
        userRepository.deleteAll();
        sponsorRepository.deleteAll();

        headerMap = testUtil.setupToken();
    }

    // Negative tests, successful auth tests are essentially done in other suites
    @Test
    public void testNoAuthHeader() throws Exception {
        this.mockMvc.perform(get(API_PREFIX + "/Patient/$export")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(401))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text",
                        Is.is("InvalidAuthHeaderException: Authorization header for token was not present")));
    }

    @Test
    public void testBadStartToHeader() throws Exception {
        this.mockMvc.perform(get(API_PREFIX + "/Patient/$export")
                .header("Authorization", "NotBearer")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(401))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text",
                        Is.is("InvalidAuthHeaderException: Authorization header must start with Bearer ")));
    }

    @Test
    public void testNoTokenInHeader() throws Exception {
        this.mockMvc.perform(get(API_PREFIX + "/Patient/$export")
                .header("Authorization", "Bearer ")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(401))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text",
                        Is.is("MissingTokenException: Token was not present")));
    }

    @Test
    public void testUserDoesNotExist() throws Exception {
        User user = userRepository.findByUserName(TEST_USER);
        userRepository.delete(user);

        this.mockMvc.perform(get(API_PREFIX + "/Patient/$export")
                .header("Authorization", "Bearer " + headerMap.get("access_token"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(403))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text",
                        Is.is("UserNotFoundException: User " + TEST_USER + " is not present in our database")));
    }

    @Test
    public void testUserIsNotEnabled() throws Exception {
        User user = userRepository.findByUserName(TEST_USER);
        user.setEnabled(false);
        userRepository.save(user);

        this.mockMvc.perform(get(API_PREFIX + "/Patient/$export")
                .header("Authorization", "Bearer " + headerMap.get("access_token"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(403))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text",
                        Is.is("UserNotEnabledException: User " + TEST_USER + " is not enabled")));
    }

    @Test
    public void testBadToken() throws Exception {
        this.mockMvc.perform(get(API_PREFIX + "/Patient/$export")
                .header("Authorization", "Bearer BadToken")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(403))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text",
                        Is.is("JwtVerificationException: Token did not contain signature")));
    }
}
