package gov.cms.ab2d.api.controller;

import com.okta.jwt.JwtVerificationException;
import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.repository.*;
import gov.cms.ab2d.common.service.SponsorService;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.events.ApiRequestEvent;
import gov.cms.ab2d.eventlogger.events.ApiResponseEvent;
import gov.cms.ab2d.eventlogger.events.ReloadEvent;
import gov.cms.ab2d.eventlogger.reports.sql.DeleteObjects;
import gov.cms.ab2d.eventlogger.reports.sql.LoadObjects;
import gov.cms.ab2d.eventlogger.utils.UtilMethods;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.InputStream;
import java.time.*;
import java.util.List;

import static gov.cms.ab2d.api.util.Constants.*;
import static gov.cms.ab2d.common.util.Constants.ADMIN_PREFIX;
import static gov.cms.ab2d.common.util.Constants.API_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
public class AdminAPIFileUploadTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SponsorService sponsorService;

    @Autowired
    private LoadObjects loadObjects;

    @Autowired
    private DeleteObjects deleteObjects;

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

    @BeforeEach
    public void setup() throws JwtVerificationException {
        contractRepository.deleteAll();
        jobRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        sponsorRepository.deleteAll();
        deleteObjects.deleteAllApiRequestEvent();
        deleteObjects.deleteAllApiResponseEvent();
        deleteObjects.deleteAllReloadEvent();
        deleteObjects.deleteAllContractBeneSearchEvent();
        deleteObjects.deleteAllErrorEvent();
        deleteObjects.deleteAllFileEvent();
        deleteObjects.deleteAllJobStatusChangeEvent();

        token = testUtil.setupToken(List.of(ADMIN_ROLE));
    }

    @Test
    public void testUploadOrgStructureFile() throws Exception {
        // Simple test to test API, more detailed test is found in service test
        String fileName = "parent_org_and_legal_entity_20191031_111812.xls";
        InputStream inputStream = this.getClass().getResourceAsStream("/" + fileName);

        MockMultipartFile mockMultipartFile = new MockMultipartFile("file", fileName, "application/vnd.ms-excel", inputStream);
        this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_PREFIX + ADMIN_PREFIX + "/uploadOrgStructureReport")
                .file(mockMultipartFile).contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(202));

        List<LoggableEvent> apiReqEvents = loadObjects.loadAllApiRequestEvent();
        assertEquals(1, apiReqEvents.size());
        ApiRequestEvent requestEvent = (ApiRequestEvent) apiReqEvents.get(0);

        List<LoggableEvent> apiResEvents = loadObjects.loadAllApiResponseEvent();
        assertEquals(1, apiResEvents.size());
        ApiResponseEvent responseEvent = (ApiResponseEvent) apiResEvents.get(0);

        List<LoggableEvent> reloadEvents = loadObjects.loadAllReloadEvent();
        assertEquals(1, reloadEvents.size());
        ReloadEvent reloadEvent = (ReloadEvent) reloadEvents.get(0);
        assertEquals(ReloadEvent.FileType.UPLOAD_ORG_STRUCTURE_REPORT, reloadEvent.getFileType());
        assertEquals(fileName, reloadEvent.getFileName());

        assertTrue(reloadEvent.getTimeOfEvent().isAfter(requestEvent.getTimeOfEvent()));
        assertTrue(responseEvent.getTimeOfEvent().isAfter(reloadEvent.getTimeOfEvent()));

        assertTrue(UtilMethods.allEmpty(
                loadObjects.loadAllContractBeneSearchEvent(),
                loadObjects.loadAllErrorEvent(),
                loadObjects.loadAllFileEvent(),
                loadObjects.loadAllJobStatusChangeEvent()
        ));
    }

    @Test
    public void testUploadAttestationFile() throws Exception {
        createData("S1234", "Med Contract", "Ins. Co.", 789);
        createData("S5660", "MEDCO CONTAINMENT LIFE AND MEDCO CONTAINMENT NY", "Ins. Co. 1", 321);
        createData("S5617", "CIGNA HEALTH AND LIFE INSURANCE COMPANY", "Ins. Co. 2", 456);

        // Simple test to test API, more detailed test is found in service test
        String fileName = "Attestation_Report_Sample.xlsx";
        InputStream inputStream = this.getClass().getResourceAsStream("/" + fileName);

        MockMultipartFile mockMultipartFile = new MockMultipartFile("file", fileName, "application/vnd.ms-excel", inputStream);
        this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_PREFIX + ADMIN_PREFIX + "/uploadAttestationReport")
                .file(mockMultipartFile).contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(202));

        List<LoggableEvent> apiReqEvents = loadObjects.loadAllApiRequestEvent();
        assertEquals(1, apiReqEvents.size());
        ApiRequestEvent requestEvent = (ApiRequestEvent) apiReqEvents.get(0);

        List<LoggableEvent> apiResEvents = loadObjects.loadAllApiResponseEvent();
        assertEquals(1, apiResEvents.size());
        ApiResponseEvent responseEvent = (ApiResponseEvent) apiResEvents.get(0);

        List<LoggableEvent> reloadEvents = loadObjects.loadAllReloadEvent();
        assertEquals(1, reloadEvents.size());
        ReloadEvent reloadEvent = (ReloadEvent) reloadEvents.get(0);
        assertEquals(ReloadEvent.FileType.ATTESTATION_REPORT, reloadEvent.getFileType());
        assertEquals(fileName, reloadEvent.getFileName());

        assertTrue(reloadEvent.getTimeOfEvent().isAfter(requestEvent.getTimeOfEvent()));
        assertTrue(responseEvent.getTimeOfEvent().isAfter(reloadEvent.getTimeOfEvent()));

        assertTrue(UtilMethods.allEmpty(
                loadObjects.loadAllContractBeneSearchEvent(),
                loadObjects.loadAllErrorEvent(),
                loadObjects.loadAllFileEvent(),
                loadObjects.loadAllJobStatusChangeEvent()
        ));

    }

    // There has to be an existing contract in order for this report to be able to process data
    private void createData(String contractId, String contractName, String sponsorName, int hpmsId) {
        Contract contract = new Contract();
        contract.setContractNumber(contractId);
        contract.setContractName(contractName);
        contract.setAttestedOn(OffsetDateTime.of(LocalDateTime.of(2018, 10, 10, 9, 17), ZoneOffset.UTC));

        Sponsor sponsor = new Sponsor();
        sponsor.setHpmsId(hpmsId);
        sponsor.setLegalName(sponsorName);
        sponsor.setOrgName(sponsorName);
        sponsor.getContracts().add(contract);

        contract.setSponsor(sponsor);

        sponsorService.saveSponsor(sponsor);
    }
}
