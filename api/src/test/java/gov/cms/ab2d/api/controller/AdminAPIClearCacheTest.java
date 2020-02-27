package gov.cms.ab2d.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.common.repository.RoleRepository;
import gov.cms.ab2d.common.repository.SponsorRepository;
import gov.cms.ab2d.common.repository.UserRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static gov.cms.ab2d.api.util.Constants.ADMIN_ROLE;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
public class AdminAPIClearCacheTest {

    private static final String API_URL = "/api/v1/admin/coverage/clearCache";
    private static final String INVALID_MONTH_ERROR = "invalid value for month. Month must be between 1 and 12";

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @MockBean
    private ClearCoverageCacheService clearCoverageCacheService;

    @Autowired private MockMvc mockMvc;
    @Autowired private SponsorRepository sponsorRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private TestUtil testUtil;
    @Autowired private ObjectMapper objectMapper;

    private String token;


    @BeforeEach
    void setup() throws JwtVerificationException {
        userRepository.deleteAll();
        roleRepository.deleteAll();
        sponsorRepository.deleteAll();

        token = testUtil.setupToken(List.of(ADMIN_ROLE));
    }

    @Test
    void should_return_no_content_status_on_success() throws Exception {
        ClearCoverageCacheRequest request = new ClearCoverageCacheRequest();
        request.setContractNumber("CONTRACT_NUMBER_0000");
        request.setMonth(1);

        mockMvc.perform(doPost(request))
                .andExpect(status().is(204));
    }

    @Test
    void should_return_400_status_when_contract_not_found() throws Exception {
        ClearCoverageCacheRequest request = new ClearCoverageCacheRequest();
        request.setContractNumber("CONTRACT_NUMBER_0000");
        request.setMonth(1);

        var errMsg = "Contract not found";
        doThrow(toException(errMsg))
                .when(clearCoverageCacheService).clearCache(any());

        mockMvc.perform(doPost(request))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.resourceType", is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", is("error")))
                .andExpect(jsonPath("$.issue[0].code", is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text", is(errMsg)));
    }


    @Test
    void should_return_400_status_when_month_is_less_than_1() throws Exception {
        ClearCoverageCacheRequest request = new ClearCoverageCacheRequest();
        request.setMonth(0);

        doThrow(toException(INVALID_MONTH_ERROR))
                .when(clearCoverageCacheService).clearCache(any());

        mockMvc.perform(doPost(request))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.resourceType", is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", is("error")))
                .andExpect(jsonPath("$.issue[0].code", is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text", is(INVALID_MONTH_ERROR)));
    }

    @Test
    void should_return_400_status_when_month_is_greater_than_12() throws Exception {
        ClearCoverageCacheRequest request = new ClearCoverageCacheRequest();
        request.setMonth(13);

        doThrow(toException(INVALID_MONTH_ERROR))
                .when(clearCoverageCacheService).clearCache(any());

        mockMvc.perform(doPost(request))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.resourceType", is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", is("error")))
                .andExpect(jsonPath("$.issue[0].code", is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text", is(INVALID_MONTH_ERROR)));
    }

    private MockHttpServletRequestBuilder doPost(ClearCoverageCacheRequest request) throws Exception {
        return post(API_URL)
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content(toRequestBody(request));
    }

    private byte[] toRequestBody(ClearCoverageCacheRequest request) throws Exception {
        return objectMapper.writeValueAsBytes(request);
    }

    private InvalidUserInputException toException(String errMsg) {
        return new InvalidUserInputException(errMsg);
    }


}
