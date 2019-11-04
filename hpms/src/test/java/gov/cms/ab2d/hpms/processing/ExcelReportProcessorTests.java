package gov.cms.ab2d.hpms.processing;

import gov.cms.ab2d.common.model.Attestation;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.repository.AttestationRepository;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.repository.SponsorRepository;
import gov.cms.ab2d.hpms.SpringBootApp;
import lombok.Value;
import org.junit.Assert;
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
import java.util.*;

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

    @Value
    private class SponsorData {

        private final Integer parentId;
        private final Set<String> contractIds;
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
            put(927, new SponsorData(1, Set.of("S8182")));
            put(39, new SponsorData(1, Set.of("S5596")));
            put(462, new SponsorData(1, Set.of("S5960")));
            put(60, new SponsorData(386, Set.of("S2893")));
            put(774, new SponsorData(642, Set.of("S6986")));
            put(521, new SponsorData(28, Set.of("S5743")));
            put(558, new SponsorData(447, Set.of("S6506")));
            put(63, new SponsorData(310, Set.of("S5726")));
            put(70, new SponsorData(25, Set.of("S5584", "S5585")));
        }};

        Assert.assertEquals(16, sponsors.size());

        List<Contract> contracts = contractRepository.findAll();

        Assert.assertEquals(10, contracts.size());

        List<Attestation> attestations = attestationRepository.findAll();

        Assert.assertEquals(10, attestations.size());

        for(Sponsor sponsor : sponsors) {
            if(sponsor.getParent() != null) {
                SponsorData sponsorData = sponsorHpmsIdsToData.get(sponsor.getHpmsId());
                Assert.assertEquals(sponsorData.getParentId(), sponsor.getParent().getHpmsId());
                Set<String> usedContractIds = new HashSet<>();
                for(Attestation attestation : sponsor.getAttestations()) {
                    String contractId = attestation.getContract().getContractId();
                    usedContractIds.add(contractId);
                }
                Assert.assertEquals(sponsorData.getContractIds(), usedContractIds);
                sponsorHpmsIdsToData.remove(sponsor.getHpmsId());
            }
        }

        Assert.assertEquals(0, sponsorHpmsIdsToData.size());
    }
}
