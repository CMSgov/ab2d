package gov.cms.ab2d.api.controller;

import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.repository.PdpClientRepository;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.DataSetup;
import gov.cms.ab2d.eventlogger.reports.sql.LoggerEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static gov.cms.ab2d.common.service.JobServiceImpl.INITIAL_JOB_STATUS_MESSAGE;
import static gov.cms.ab2d.common.util.Constants.API_PREFIX_V2;
import static gov.cms.ab2d.common.util.Constants.FHIR_PREFIX;
import static gov.cms.ab2d.common.util.Constants.SPONSOR_ROLE;
import static gov.cms.ab2d.common.util.DataSetup.TEST_PDP_CLIENT;
import static gov.cms.ab2d.common.util.DataSetup.VALID_CONTRACT_NUMBER;
import static gov.cms.ab2d.fhir.BundleUtils.EOB;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.http.HttpHeaders.CONTENT_LOCATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
public class BulkDataAccessAPIV2IntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PdpClientRepository pdpClientRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @Autowired
    private TestUtil testUtil;

    @Autowired
    private DataSetup dataSetup;

    @Autowired
    private LoggerEventRepository loggerEventRepository;

    private String token;

    public static final String PATIENT_EXPORT_PATH = "/Patient/$export";

    @BeforeEach
    public void setup() throws JwtVerificationException {
        testUtil.turnMaintenanceModeOff();
        token = testUtil.setupToken(List.of(SPONSOR_ROLE));
    }

    @AfterEach
    public void cleanup() {
        jobRepository.findAll().forEach(job -> dataSetup.queueForCleanup(job));  // catches implicitly generated jobs
        loggerEventRepository.delete();
        dataSetup.cleanup();
    }

    @Test
    void testBasicPatientExportWithHttps() throws Exception {
        ResultActions resultActions = this.mockMvc.perform(
                get(API_PREFIX_V2 + FHIR_PREFIX + PATIENT_EXPORT_PATH).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Forwarded-Proto", "https"));
        // .andDo(print());
        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();

        String statusUrl =
                "https://localhost" + API_PREFIX_V2 + FHIR_PREFIX + "/Job/" + job.getJobUuid() + "/$status";

        resultActions.andExpect(status().isAccepted())
                .andExpect(header().string(CONTENT_LOCATION, statusUrl));

        assertEquals(JobStatus.SUBMITTED, job.getStatus());
        assertEquals(INITIAL_JOB_STATUS_MESSAGE, job.getStatusMessage());
        assertEquals(Integer.valueOf(0), job.getProgress());
        assertEquals("https://localhost" + API_PREFIX_V2 + FHIR_PREFIX + PATIENT_EXPORT_PATH, job.getRequestUrl());
        assertEquals(EOB, job.getResourceTypes());
        assertEquals(pdpClientRepository.findByClientId(TEST_PDP_CLIENT), job.getPdpClient());
    }

    @Test
    void testBasicPatientExportWithContractWithHttps() throws Exception {
        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();
        ResultActions resultActions = this.mockMvc.perform(
                get(API_PREFIX_V2 + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Forwarded-Proto", "https"));
        // .andDo(print());
        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();

        String statusUrl =
                "https://localhost" + API_PREFIX_V2 + FHIR_PREFIX + "/Job/" + job.getJobUuid() + "/$status";
        assertNotNull(statusUrl);

        resultActions.andExpect(status().isAccepted())
                .andExpect(header().string(CONTENT_LOCATION, statusUrl));

        assertEquals(JobStatus.SUBMITTED, job.getStatus());
        assertEquals(INITIAL_JOB_STATUS_MESSAGE, job.getStatusMessage());
        assertEquals(Integer.valueOf(0), job.getProgress());
        assertEquals("https://localhost" + API_PREFIX_V2 + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export", job.getRequestUrl());
        assertNull(job.getResourceTypes());
        assertEquals(pdpClientRepository.findByClientId(TEST_PDP_CLIENT), job.getPdpClient());
    }
}
