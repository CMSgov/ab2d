package gov.cms.ab2d.coverage.service;

import gov.cms.ab2d.common.feign.ContractFeignClient;
import gov.cms.ab2d.coverage.model.CoveragePeriod;
import gov.cms.ab2d.coverage.model.CoverageSearch;
import gov.cms.ab2d.coverage.model.CoverageSearchEvent;
import gov.cms.ab2d.coverage.repository.*;
import gov.cms.ab2d.coverage.util.AB2DCoverageLocalstackContainer;
import gov.cms.ab2d.coverage.util.AB2DCoveragePostgressqlContainer;
import gov.cms.ab2d.coverage.util.CoverageDataSetup;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@EntityScan(basePackages = {"gov.cms.ab2d.common.model", "gov.cms.ab2d.coverage.model"})
@EnableJpaRepositories({"gov.cms.ab2d.common.repository", "gov.cms.ab2d.coverage.repository"})
@Testcontainers
@TestPropertySource(locations = "/application.coverage.properties")
@EnableFeignClients(clients = {ContractFeignClient.class})
public class CoverageServiceExceptionTest {

    @Container
    private static final PostgreSQLContainer postgreSQLContainer = new AB2DCoveragePostgressqlContainer();

    @Container
    private static final AB2DCoverageLocalstackContainer localstackContainer = new AB2DCoverageLocalstackContainer();

    @Autowired
    CoveragePeriodRepository coveragePeriodRepo;

    @Autowired
    CoverageSearchEventRepository coverageSearchEventRepo;

    @Autowired
    CoverageSearchRepository coverageSearchRepo;

    @Autowired
    CoverageDeltaRepository coverageDeltaRepository;

    @Autowired
    CoverageDeltaTestRepository coverageDeltaTestRepository;

    @Autowired
    CoverageService coverageService;

    @MockitoSpyBean
    CoverageServiceRepository coverageServiceRepo;

    @MockitoSpyBean
    SQSEventClient eventLogger;

    @Autowired
    CoverageDataSetup dataSetup;

    private CoveragePeriod period1Jan;


    @BeforeEach
    public void insertContractAndDefaultCoveragePeriod() {
        period1Jan = dataSetup.createCoveragePeriod("TST-12", 1, 2020);
    }

    @AfterEach
    public void cleanUp() {
        dataSetup.cleanup();
    }

    @DisplayName("Coverage period searches can be cancelled")
    @Test
    void failSearchDeleteAlerts() {

        Mockito.doThrow(RuntimeException.class).when(coverageServiceRepo).deleteCurrentSearch(ArgumentMatchers.any());

        coverageService.submitSearch(period1Jan.getId(), "testing");
        startSearchAndPullEvent();

        assertThrows(RuntimeException.class, () -> coverageService.failSearch(period1Jan.getId(), "testing"));

        Mockito.verify(eventLogger, Mockito.times(1)).alert(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @DisplayName("Delete previous search on completion failure triggers alert")
    @Test
    void deletePreviousSearchOnCompletionFailure() {

        Mockito.doThrow(new RuntimeException()).when(coverageServiceRepo).deletePreviousSearches(ArgumentMatchers.nullable(CoveragePeriod.class), ArgumentMatchers.nullable(Integer.class));

        coverageService.submitSearch(period1Jan.getId(), "testing");
        startSearchAndPullEvent();

        assertThrows(RuntimeException.class, () -> coverageService.completeSearch(period1Jan.getId(), "testing"));

        Mockito.verify(eventLogger, Mockito.times(1)).alert(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    private CoverageSearchEvent startSearchAndPullEvent() {
        Optional<CoverageSearch> search = coverageSearchRepo.findFirstByOrderByCreatedAsc();
        coverageSearchRepo.delete(search.get());
        return coverageService.startSearch(search.get(), "testing").get().getCoverageSearchEvent();
    }
}
