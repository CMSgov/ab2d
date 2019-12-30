package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
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

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

import static gov.cms.ab2d.api.util.Constants.*;
import static gov.cms.ab2d.common.service.JobServiceImpl.INITIAL_JOB_STATUS_MESSAGE;
import static gov.cms.ab2d.common.util.DataSetup.*;
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
    private SponsorRepository sponsorRepository;

    @Autowired
    private TestUtil testUtil;

    @Autowired
    private ContractRepository contractRepository;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @BeforeEach
    public void clearUser() {
        contractRepository.deleteAll();
        jobRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        sponsorRepository.deleteAll();
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

        Assert.assertEquals(job.getStatus(), JobStatus.SUBMITTED);
        Assert.assertEquals(job.getStatusMessage(), INITIAL_JOB_STATUS_MESSAGE);
        Assert.assertEquals(job.getProgress(), Integer.valueOf(0));
        Assert.assertEquals(job.getRequestUrl(),
                "http://localhost" + API_PREFIX + FHIR_PREFIX + "/Group/" + contract.getContractNumber() + "/$export");
        Assert.assertEquals(job.getResourceTypes(), null);
        Assert.assertEquals(job.getUser(), userRepository.findByUsername(TEST_USER));
    }
}
