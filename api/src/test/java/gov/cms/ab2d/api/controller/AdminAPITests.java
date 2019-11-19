package gov.cms.ab2d.api.controller;

import gov.cms.ab2d.api.SpringBootApp;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.service.SponsorService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static gov.cms.ab2d.api.util.Constants.API_PREFIX;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringBootApp.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class AdminAPITests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SponsorService sponsorService;

    @Autowired
    private TestUtil testUtil;

    private Map<String, String> headerMap;

    @Before
    public void setup() throws IOException, InterruptedException {
        headerMap = testUtil.setupToken();
    }

    @Test
    public void testUploadOrgStructureFile() throws Exception {
        // Simple test to test API, more detailed test is found in service test
        String fileName = "parent_org_and_legal_entity_20191031_111812.xls";
        InputStream inputStream = this.getClass().getResourceAsStream("/" + fileName);

        MockMultipartFile mockMultipartFile = new MockMultipartFile("file", fileName, "application/vnd.ms-excel", inputStream);
        this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_PREFIX + "/uploadOrgStructureReport")
                .file(mockMultipartFile).contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Authorization", "Bearer " + headerMap.get("access_token")))
                .andExpect(status().is(202));
    }

    @Test
    public void testUploadAttestationFile() throws Exception {
        createData("S1234", "Med Contract", "Ins. Co.", 789);
        createData("S5678", "United HC Contract", "Ins. Co. 1", 321);

        // Simple test to test API, more detailed test is found in service test
        String fileName = "Attestation_Report_Sample.xlsx";
        InputStream inputStream = this.getClass().getResourceAsStream("/" + fileName);

        MockMultipartFile mockMultipartFile = new MockMultipartFile("file", fileName, "application/vnd.ms-excel", inputStream);
        this.mockMvc.perform(MockMvcRequestBuilders.multipart(API_PREFIX + "/uploadAttestationReport")
                .file(mockMultipartFile).contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Authorization", "Bearer " + headerMap.get("access_token")))
                .andExpect(status().is(202));
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
