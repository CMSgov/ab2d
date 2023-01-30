package gov.cms.ab2d.worker.service.coveragesnapshot;

import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.repository.PdpClientRepository;
import gov.cms.ab2d.common.repository.RoleRepository;
import gov.cms.ab2d.common.service.PdpClientService;
import gov.cms.ab2d.common.util.AB2DLocalstackContainer;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.AB2DSQSMockConfig;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.coverage.model.ContractForCoverageDTO;
import gov.cms.ab2d.coverage.model.CoverageCount;
import gov.cms.ab2d.coverage.model.CoverageJobStatus;
import gov.cms.ab2d.coverage.model.CoveragePeriod;
import gov.cms.ab2d.coverage.model.CoverageSearch;
import gov.cms.ab2d.coverage.model.CoverageSearchEvent;
import gov.cms.ab2d.coverage.model.Identifiers;
import gov.cms.ab2d.coverage.repository.CoveragePeriodRepository;
import gov.cms.ab2d.coverage.repository.CoverageSearchRepository;
import gov.cms.ab2d.coverage.repository.CoverageServiceRepository;
import gov.cms.ab2d.coverage.service.CoverageService;
import gov.cms.ab2d.coverage.util.CoverageDataSetup;
import gov.cms.ab2d.snsclient.messages.AB2DServices;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;


@SpringBootTest
@Testcontainers
@Import(AB2DSQSMockConfig.class)
public class CoverageSnapshotTest {

    @SuppressWarnings("rawtypes")
    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DPostgresqlContainer();

    @Container
    private static final AB2DLocalstackContainer LOCALSTACK_CONTAINER = new AB2DLocalstackContainer();

    @Autowired
    CoverageSnapshotService coverageSnapshotService;

    @Autowired
    RoleRepository roleRepository;
    @Autowired
    PdpClientRepository pdpClientRepository;
    @Autowired
    ContractRepository contractRepository;
    @Autowired
    CoverageServiceRepository coverageServiceRepository;

    @Autowired
    CoverageSearchRepository coverageSearchRepo;

    @SpyBean
    CoverageServiceRepository coverageServiceRepo;
    @Autowired
    CoverageService coverageService;

    @Autowired
    private PdpClientService pdpClientService;
    @Autowired
    CoverageDataSetup dataSetup;

    @Autowired
    private CoveragePeriodRepository coveragePeriodRepo;


    @Test
    void sendCoverageCounts() {
        CoverageService coverageService = Mockito.mock(CoverageService.class);
        Mockito.when(coverageService.countBeneficiariesForContracts(any()))
                .thenReturn(Arrays.asList(new CoverageCount("test", 1, 1, 1, 1, 1), new CoverageCount("test", 1, 1, 1, 1, 1)));
        coverageSnapshotService.sendCoverageCounts(AB2DServices.AB2D, Set.of("test"));
    }

    @Test
    void sendCoverageCountsFromDB() throws InterruptedException, NoSuchMethodException {
        Contract contract = pdpClientService.getAllEnabledContracts()
                .stream()
                .findFirst()
                .orElseThrow();
        String contractNumber = contract.getContractNumber();


        int lastYear = LocalDate.now()
                .minusYears(1)
                .getYear();
        List<CoveragePeriod> updates = List.of(dataSetup.createCoveragePeriod(contractNumber, 1, lastYear),
                dataSetup.createCoveragePeriod(contractNumber, 2, lastYear),
                dataSetup.createCoveragePeriod(contractNumber, 3, lastYear),
                dataSetup.createCoveragePeriod(contractNumber, 4, lastYear),
                dataSetup.createCoveragePeriod(contractNumber, 5, lastYear),
                dataSetup.createCoveragePeriod(contractNumber, 6, lastYear),
                dataSetup.createCoveragePeriod(contractNumber, 7, lastYear),
                dataSetup.createCoveragePeriod(contractNumber, 8, lastYear));

        updates.forEach(update -> {
            coverageService.submitSearch(update.getId(), "testing");

        });

        CoverageSearchEvent inProgress = startSearchAndPullEvent();
        // Last page will have only one id
        int totalBeneficiaries = 501;

        // Add 700 beneficiaries to
        Set<Identifiers> identifiers = new LinkedHashSet<>();
        for (long idx = 0; idx < totalBeneficiaries; idx++) {
            identifiers.add(createIdentifier(idx));
        }
        CoverageSearchEvent savedTo = coverageService.insertCoverage(inProgress.getId(), identifiers);

        updates.forEach(update -> {
            CoveragePeriod period = coverageService.getCoveragePeriod(new ContractForCoverageDTO(contractNumber, contract.getAttestedOn(), ContractForCoverageDTO.ContractType.valueOf(contract.getContractType()
                    .toString())), update.getMonth(), lastYear);
            period.setStatus(CoverageJobStatus.SUCCESSFUL);
            coveragePeriodRepo.saveAndFlush(period);
        });

        coverageSnapshotService.sendCoverageCounts(AB2DServices.AB2D, Set.of(contractNumber));
    }

    private CoverageSearchEvent startSearchAndPullEvent() {
        Optional<CoverageSearch> search = coverageSearchRepo.findFirstByOrderByCreatedAsc();
        coverageSearchRepo.delete(search.get());
        return coverageService.startSearch(search.get(), "testing")
                .get()
                .getCoverageSearchEvent();
    }

    private Identifiers createIdentifier(Long suffix) {
        return new Identifiers(suffix, "mbi-" + suffix, new LinkedHashSet<>());
    }

}
