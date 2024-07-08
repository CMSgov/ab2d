package gov.cms.ab2d.coverage.repository;

import gov.cms.ab2d.common.feign.ContractFeignClient;
import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.coverage.model.CoveragePeriod;
import gov.cms.ab2d.coverage.model.CoverageSearch;
import gov.cms.ab2d.coverage.model.CoverageSearchEvent;
import gov.cms.ab2d.coverage.model.Identifiers;
import gov.cms.ab2d.coverage.service.CoverageService;
import gov.cms.ab2d.coverage.util.AB2DCoverageLocalstackContainer;
import gov.cms.ab2d.coverage.util.AB2DCoveragePostgressqlContainer;
import gov.cms.ab2d.coverage.util.CoverageDataSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

import java.lang.reflect.Field;
import java.util.*;

import static gov.cms.ab2d.common.util.PropertyConstants.OPT_OUT_ON;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@SpringBootTest
@EntityScan(basePackages = {"gov.cms.ab2d.common.model", "gov.cms.ab2d.coverage.model"})
@EnableJpaRepositories({"gov.cms.ab2d.common.repository", "gov.cms.ab2d.coverage.repository"})
@Testcontainers
@TestPropertySource(locations = "/application.coverage.properties")
@EnableFeignClients(clients = {ContractFeignClient.class})
class CoverageServiceRepositoryTest {

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DCoveragePostgressqlContainer();

    @Container
    private static final AB2DCoverageLocalstackContainer localstackContainer = new AB2DCoverageLocalstackContainer();

    @Autowired
    DataSource dataSource;

    @Autowired
    CoveragePeriodRepository coveragePeriodRepo;
    @Autowired
    CoverageSearchRepository coverageSearchRepo;
    @Autowired
    CoverageSearchEventRepository coverageSearchEventRepo;
    @Autowired
    CoverageService coverageService;
    @Autowired
    CoverageDataSetup dataSetup;
    @Autowired
    PropertiesService propertiesService;
    @Mock
    private PropertiesService mockPropertiesService;

    private CoverageServiceRepository coverageServiceRepository;

    private CoveragePeriod period1Jan;
    private Set<Identifiers> results;

    @BeforeEach
    public void insertContractAndDefaultCoveragePeriod() {
        // OptOut is disabled
        Mockito.when(mockPropertiesService.isToggleOn(OPT_OUT_ON, false)).thenReturn(false);
        period1Jan = dataSetup.createCoveragePeriod("TST-12", 1, 2020);

        coverageService.submitSearch(period1Jan.getId(), "testing");
        results = Set.of(createIdentifier(1231L),
                createIdentifier(4561L), createIdentifier(7891L));

        CoverageSearchEvent inProgress1 = startSearchAndPullEvent();
        coverageService.insertCoverage(inProgress1.getId(), results);

        coverageServiceRepository = new CoverageServiceRepository(dataSource, coveragePeriodRepo, coverageSearchEventRepo, mockPropertiesService);
    }

    @DisplayName("Calculate the number of beneficiaries when OptOut is disabled")
    @Test
    void countBeneficiariesByPeriodsWithoutOptOutTest() {
        Assertions.assertEquals(3, coverageServiceRepository.countBeneficiariesByPeriods(List.of(period1Jan.getId()), "TST-12"));
    }

    @DisplayName("Calculate the number of beneficiaries when OptOut is enabled")
    @Test
    void countBeneficiariesByPeriodsWithOptOutTest() {
        Mockito.when(mockPropertiesService.isToggleOn(OPT_OUT_ON, false)).thenReturn(true);
       //The expected number is 3, and is the same as in previous test, since switching opt_out_flag from false to true is only available in OptOutLambda.
        //Here all beneficiaries have opt_out_flag equals false by default.
        Assertions.assertEquals(0, coverageServiceRepository.countBeneficiariesByPeriods(List.of(period1Jan.getId()), "TST-12"));
    }

    @Test
    void yearsTest() throws NoSuchFieldException, IllegalAccessException {
        Field field = CoverageServiceRepository.class.getDeclaredField("YEARS");
        field.setAccessible(true);
        Assertions.assertNotNull(field.get(null));
        Assertions.assertTrue(field.get(null).toString().startsWith("[2020, 2021, 2022, 2023, 2024"));
    }

    @Test
    void testVacuumCoverage() {
        assertDoesNotThrow(() -> {
            coverageServiceRepository.vacuumCoverage();
        });

        String vacuumCoverage = (String) ReflectionTestUtils.getField(coverageServiceRepository, "vacuumCoverage");
        ReflectionTestUtils.setField(coverageServiceRepository, "vacuumCoverage", "broken");

        assertThrows(RuntimeException.class, () -> {
            coverageServiceRepository.vacuumCoverage();
        });

        ReflectionTestUtils.setField(coverageServiceRepository, "vacuumCoverage", vacuumCoverage);
    }

    private Identifiers createIdentifier(Long suffix) {
        return new Identifiers(suffix, "mbi-" + suffix, new LinkedHashSet<>());
    }

    private CoverageSearchEvent startSearchAndPullEvent() {
        Optional<CoverageSearch> search = coverageSearchRepo.findFirstByOrderByCreatedAsc();
        coverageSearchRepo.delete(search.get());
        return coverageService.startSearch(search.get(), "testing").get().getCoverageSearchEvent();
    }
}
