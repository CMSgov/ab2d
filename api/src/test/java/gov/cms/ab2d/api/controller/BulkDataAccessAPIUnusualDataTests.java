package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.events.*;
import gov.cms.ab2d.eventlogger.reports.sql.DoAll;
import gov.cms.ab2d.eventlogger.utils.UtilMethods;
import org.junit.Assert;
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

import static gov.cms.ab2d.common.model.JobStatus.SUBMITTED;
import static gov.cms.ab2d.common.service.JobServiceImpl.INITIAL_JOB_STATUS_MESSAGE;
import static gov.cms.ab2d.common.util.Constants.*;
import static gov.cms.ab2d.common.util.DataSetup.*;
import static junit.framework.TestCase.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
public class BulkDataAccessAPIUnusualDataTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TestUtil testUtil;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private DoAll doAll;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @BeforeEach
    public void clearUser() {
        jobRepository.deleteAll();
        contractRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        doAll.delete();
    }

    @Test
    public void testPatientExportWithContractNotTiedToUser() throws Exception {
        String token = testUtil.setupBadSponsorUserData(List.of(SPONSOR_ROLE));
        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(BAD_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();
        this.mockMvc.perform(get(API_PREFIX + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(403));
        List<LoggableEvent> apiRequestEvents = doAll.load(ApiRequestEvent.class);
        ApiRequestEvent requestEvent = (ApiRequestEvent) apiRequestEvents.get(0);

        List<LoggableEvent> apiResponseEvents = doAll.load(ApiResponseEvent.class);
        ApiResponseEvent responseEvent = (ApiResponseEvent) apiResponseEvents.get(0);
        assertEquals(requestEvent.getRequestId(), responseEvent.getRequestId());

        List<LoggableEvent> errorEvents = doAll.load(ErrorEvent.class);
        ErrorEvent errorEvent = (ErrorEvent) errorEvents.get(0);
        assertEquals(errorEvent.getErrorType(), ErrorEvent.ErrorType.UNAUTHORIZED_CONTRACT);

        assertTrue(UtilMethods.allEmpty(
                doAll.load(ReloadEvent.class),
                doAll.load(ContractBeneSearchEvent.class),
                doAll.load(JobStatusChangeEvent.class),
                doAll.load(FileEvent.class)
        ));
    }

    @Test
    public void testPatientExportWithNoAttestation() throws Exception {
        // Valid contract number for sponsor, but no attestation
        String token = testUtil.setupContractWithNoAttestation(List.of(SPONSOR_ROLE));
        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();
        this.mockMvc.perform(get(API_PREFIX + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(403));
        List<LoggableEvent> apiRequestEvents = doAll.load(ApiRequestEvent.class);
        ApiRequestEvent requestEvent = (ApiRequestEvent) apiRequestEvents.get(0);

        List<LoggableEvent> apiResponseEvents = doAll.load(ApiResponseEvent.class);
        ApiResponseEvent responseEvent = (ApiResponseEvent) apiResponseEvents.get(0);
        assertEquals(requestEvent.getRequestId(), responseEvent.getRequestId());

        List<LoggableEvent> errorEvents = doAll.load(ErrorEvent.class);
        ErrorEvent errorEvent = (ErrorEvent) errorEvents.get(0);
        assertEquals(errorEvent.getErrorType(), ErrorEvent.ErrorType.UNAUTHORIZED_CONTRACT);

        assertTrue(UtilMethods.allEmpty(
                doAll.load(ReloadEvent.class),
                doAll.load(ContractBeneSearchEvent.class),
                doAll.load(JobStatusChangeEvent.class),
                doAll.load(FileEvent.class)));
    }

    @Test
    public void testPatientExportWithOnlyParentAttestation() throws Exception {
        String token = testUtil.setupContractSponsorForParentUserData(List.of(SPONSOR_ROLE));

        Optional<Contract> contractOptional = contractRepository.findContractByContractNumber(VALID_CONTRACT_NUMBER);
        Contract contract = contractOptional.get();
        ResultActions resultActions = this.mockMvc.perform(
                get(API_PREFIX + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token))
                .andDo(print());
        Job job = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).iterator().next();

        String statusUrl =
                "http://localhost" + API_PREFIX + FHIR_PREFIX + "/Job/" + job.getJobUuid() + "/$status";

        resultActions.andExpect(status().isAccepted())
                .andExpect(header().string("Content-Location", statusUrl));

        Assert.assertEquals(job.getStatus(), SUBMITTED);
        Assert.assertEquals(job.getStatusMessage(), INITIAL_JOB_STATUS_MESSAGE);
        Assert.assertEquals(job.getProgress(), Integer.valueOf(0));
        Assert.assertEquals(job.getRequestUrl(),
                "http://localhost" + API_PREFIX + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export");
        Assert.assertNull(job.getResourceTypes());
        Assert.assertEquals(job.getUser(), userRepository.findByUsername(TEST_USER));

    }
}
