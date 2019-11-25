package gov.cms.ab2d.hpms.processing;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.repository.SponsorRepository;
import gov.cms.ab2d.common.repository.UserRepository;
import gov.cms.ab2d.common.service.SponsorService;
import gov.cms.ab2d.hpms.SpringBootApp;
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
import java.time.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringBootApp.class)
@TestPropertySource(locations = "/application.hpms.properties")
public class AttestationReportProcessorTests {

    @Autowired
    @Qualifier("attestationReportProcessor")
    private AttestationReportProcessor attestationReportProcessor;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private SponsorService sponsorService;

    @Autowired
    private SponsorRepository sponsorRepository;

    @Autowired
    private UserRepository userRepository;

    // There has to be an existing contract in order for this report to be able to process data
    private void createData(String contractId, String contractName, String sponsorName, int hpmsId) {
        Contract contract = new Contract();
        contract.setContractNumber(contractId);
        contract.setContractName(contractName);

        Sponsor sponsor = new Sponsor();
        sponsor.setHpmsId(hpmsId);
        sponsor.setLegalName(sponsorName);
        sponsor.setOrgName(sponsorName);

        contract.setAttestedOn(OffsetDateTime.of(LocalDateTime.of(2018, 10, 10, 9, 17), ZoneOffset.UTC));

        sponsor.getContracts().add(contract);

        contract.setSponsor(sponsor);

        sponsorService.saveSponsor(sponsor);
    }

    @Before
    public void setup() {
        contractRepository.deleteAll();
        userRepository.deleteAll();
        sponsorRepository.deleteAll();
    }

    @Test
    @Transactional // Used so collections that are lazily loaded can be accessed
    public void testProcessReport() throws IOException {
        createData("S5617", "SB Corp", "SB Corp", 123);
        createData("S5660", "United Ins.", "United Ins.", 456);
        createData("S1234", "Med Contract", "Ins. Co.", 789);
        createData("S5678", "United HC Contract", "Ins. Co. 1", 321);

        InputStream testFileStream = this.getClass().getResourceAsStream("/Attestation_Report_Sample.xlsx");
        attestationReportProcessor.processReport(testFileStream, ExcelType.fromFileType("Attestation_Report_Sample.xlsx"));
        checkResults();

        // Open stream again since it gets auto closed
        testFileStream = this.getClass().getResourceAsStream("/Attestation_Report_Sample.xlsx");
        attestationReportProcessor.processReport(testFileStream, ExcelType.fromFileType("Attestation_Report_Sample.xlsx"));
        checkResults();
    }

    private void checkResults() {
        List<Contract> contracts = contractRepository.findAll();

        ZoneOffset nyZoneOffset = ZoneId.of("America/New_York").getRules().getOffset(Instant.now());

        Map<String, OffsetDateTime> attestationData = new HashMap<>();
        attestationData.put("S5617", null);
        attestationData.put("S5660", null);
        attestationData.put("S1234", OffsetDateTime.of(LocalDateTime.of(2019, 9, 19, 4, 0),
                nyZoneOffset));
        attestationData.put("S5678", null);

        for(Contract contract : contracts) {
            OffsetDateTime offsetDateTime = attestationData.get(contract.getContractNumber());
            Assert.assertEquals(offsetDateTime, contract.getAttestedOn());
        }
    }
}
