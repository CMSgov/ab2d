package gov.cms.ab2d.hpms.processing;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.repository.SponsorRepository;
import gov.cms.ab2d.common.repository.UserRepository;
import gov.cms.ab2d.common.repository.RoleRepository;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.hpms.SpringBootApp;
import lombok.Value;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringBootApp.class)
@TestPropertySource(locations = "/application.hpms.properties")
public class OrgStructureReportProcessorTests {

    @Autowired
    @Qualifier("orgStructureReportProcessor")
    private ExcelReportProcessor excelReportProcessor;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private SponsorRepository sponsorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JobRepository jobRepository;

    @Value
    private class SponsorData {

        private final Integer parentId;
        private final Map<String, String> contractNumbersToNames;
        private final String orgName;
    }

    @Before
    public void cleanup() {
        contractRepository.deleteAll();
        jobRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        sponsorRepository.deleteAll();
    }

    @Test
    @Transactional // Used so collections that are lazily loaded can be accessed
    public void testProcessReport() throws IOException {
        InputStream testFileStream = this.getClass().getResourceAsStream("/parent_org_and_legal_entity_20191031_111812.xls");
        excelReportProcessor.processReport(testFileStream, ExcelType.fromFileType("parent_org_and_legal_entity_20191031_111812.xls"));
        checkResults();

        // Stream gets closed so open again. This file should be able to be processed over and over and not overwrite anything
        testFileStream = this.getClass().getResourceAsStream("/parent_org_and_legal_entity_20191031_111812.xls");
        excelReportProcessor.processReport(testFileStream, ExcelType.fromFileType("parent_org_and_legal_entity_20191031_111812.xls"));
        checkResults();
    }

    private void checkResults() {
        List<Sponsor> sponsors = sponsorRepository.findAll();

        // This should look like the relationships between a sponsor and the parent, and the sponsor's contract id
        Map<Integer, SponsorData> sponsorHpmsIdsToData = new HashMap<>() {{
            put(927, new SponsorData(1, Map.of("S8182", "AMERIGROUP INSURANCE COMPANY"), "AMERIGROUP INSURANCE COMPANY"));
            put(39, new SponsorData(1, Map.of("S5596", "ANTHEM INSURANCE COMPANIES, INC."), "ANTHEM INSURANCE COMPANIES, INC."));
            put(462, new SponsorData(1, Map.of("S5960", "UNICARE LIFE & HEALTH INSURANCE COMPANY"), "UNICARE LIFE & HEALTH INSURANCE COMPANY"));
            put(60, new SponsorData(386, Map.of("S2893", "ANTHEM INSURANCE CO. & BCBSMA & BCBSRI & BCBSVT"), "ANTHEM INSURANCE CO. & BCBSMA & BCBSRI & BCBSVT"));
            put(774, new SponsorData(642, Map.of("S6986", "MII LIFE INSURANCE, INCORPORATED"), "MII LIFE INSURANCE, INCORPORATED"));
            put(521, new SponsorData(28, Map.of("S5743", "WELLMARK IA & SD, & BCBS MN, MT, NE, ND,& WY"), "WELLMARK IA & SD, & BCBS MN, MT, NE, ND,& WY"));
            put(558, new SponsorData(447, Map.of("S6506", "BLUE CROSS AND BLUE SHIELD ARIZONA, INC."), "BLUE CROSS AND BLUE SHIELD ARIZONA, INC."));
            put(63, new SponsorData(310, Map.of("S5726", "BLUE CROSS AND BLUE SHIELD OF KANSAS"), "BLUE CROSS AND BLUE SHIELD OF KANSAS"));
            put(70, new SponsorData(25, Map.of("S5584", "BCBS OF MICHIGAN MUTUAL INSURANCE COMPANY",
                    "S5585", "BCBS OF MICHIGAN MUTUAL INSURANCE COMPANY 2"), "BLUE CROSS BLUE SHIELD OF MICHIGAN MUTUAL INSURANCE COMPANY"));
            put(365, new SponsorData(25, Map.of("S5586", "BCBS OF MICHIGAN MUTUAL INSURANCE COMPANY 3"), "BLUE CROSS BLUE SHIELD OF MICHIGAN MUTUAL INSURANCE COMPANY"));
        }};

        Assert.assertEquals(17, sponsors.size());

        List<Contract> contracts = contractRepository.findAll();

        Assert.assertEquals(11, contracts.size());

        for(Sponsor sponsor : sponsors) {
            if(sponsor.getParent() != null) {
                SponsorData sponsorData = sponsorHpmsIdsToData.get(sponsor.getHpmsId());
                Assert.assertEquals(sponsorData.getParentId(), sponsor.getParent().getHpmsId());
                Map<String, String> usedContractNumbersToNames = new HashMap<>();
                for(Contract contract : sponsor.getContracts()) {
                    Assert.assertNull(contract.getAttestedOn());
                    usedContractNumbersToNames.put(contract.getContractNumber(), contract.getContractName());
                }
                Assert.assertEquals(sponsorData.getContractNumbersToNames(), usedContractNumbersToNames);
                Assert.assertEquals(sponsorData.getOrgName(), sponsor.getOrgName());
                sponsorHpmsIdsToData.remove(sponsor.getHpmsId());
            }
        }

        Assert.assertEquals(0, sponsorHpmsIdsToData.size());
    }
}
