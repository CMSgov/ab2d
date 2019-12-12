package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

import static gov.cms.ab2d.api.util.Constants.*;
import static gov.cms.ab2d.common.util.DataSetup.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
public class BulkDataAccessAPIBadData {

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
                .andExpect(status().is(500))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text",
                        Is.is("UserPermissionsException: User " + TEST_USER + " does not have permissions for contract " + contract.getContractNumber())));
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
                .andExpect(status().is(500))
                .andExpect(jsonPath("$.resourceType", Is.is("OperationOutcome")))
                .andExpect(jsonPath("$.issue[0].severity", Is.is("error")))
                .andExpect(jsonPath("$.issue[0].code", Is.is("invalid")))
                .andExpect(jsonPath("$.issue[0].details.text",
                        Is.is("ContractHasNotBeenAttestedException: Contract " + contract.getContractNumber() + " has not been attested for")));
    }
}
