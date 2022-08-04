package gov.cms.ab2d.api.controller;

import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.util.AB2DLocalstackContainer;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.eventlogger.reports.sql.LoggerEventRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static gov.cms.ab2d.common.model.Role.SPONSOR_ROLE;
import static gov.cms.ab2d.common.util.Constants.*;
import static gov.cms.ab2d.common.util.DataSetup.TEST_PDP_CLIENT;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
public class InvalidTokenTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUtil testUtil;

    @Autowired
    private DataSetup dataSetup;

    @Autowired
    private PdpClientRepository pdpClientRepository;

    @Autowired
    ContractRepository contractRepository;

    @Autowired
    LoggerEventRepository loggerEventRepository;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    private String token;

    @BeforeEach
    public void setup() throws JwtVerificationException {
        token = testUtil.setupInvalidToken(List.of(SPONSOR_ROLE));
    }

    @AfterEach
    public void tearDown() {
        dataSetup.cleanup();
        loggerEventRepository.delete();
    }

    // Moved this test to here to avoid using @Before annotation of other Auth tests
    @Test
    public void testInvalidToken() throws Exception {
        PdpClient pdpClient = pdpClientRepository.findByClientId(TEST_PDP_CLIENT);
        assertNotNull(pdpClient);
        pdpClient.setEnabled(false);
        pdpClientRepository.save(pdpClient);

        this.mockMvc.perform(get(API_PREFIX_V1 + FHIR_PREFIX + "/Patient/$export")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(403));
    }
}
