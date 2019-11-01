package gov.cms.ab2d.hpms.processing;

import gov.cms.ab2d.common.model.Attestation;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.repository.AttestationRepository;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.repository.SponsorRepository;
import gov.cms.ab2d.hpms.SpringBootApp;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringBootApp.class)
@TestPropertySource(locations = "/hpms-it.properties")
public class ExcelReportProcessorTests {

    @Autowired
    @Qualifier("hpmsExcelReportProcessor")
    private ExcelReportProcessor excelReportProcessor;

    @Autowired
    private SponsorRepository sponsorRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private AttestationRepository attestationRepository;

    @Test
    public void testProcessReport() throws IOException {
        InputStream testFileStream = this.getClass().getResourceAsStream("/parent_org_and_legal_entity_20191031_111812.xls");

        excelReportProcessor.processReport(testFileStream);

        List<Sponsor> sponsors = sponsorRepository.findAll();

        for(Sponsor sponsor : sponsors) {
            System.out.println(sponsor);
        }

        Assert.assertEquals(sponsors.size(), 113);

        List<Contract> contracts = contractRepository.findAll();

        Assert.assertEquals(contracts.size(), 65);

        List<Attestation> attestations = attestationRepository.findAll();

        Assert.assertEquals(attestations.size(), 65);
    }
}
