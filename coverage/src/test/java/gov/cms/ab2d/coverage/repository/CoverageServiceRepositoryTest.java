package gov.cms.ab2d.coverage.repository;

import gov.cms.ab2d.common.feign.ContractFeignClient;
import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.common.util.LiquibaseTestConfig;
import gov.cms.ab2d.coverage.model.ContractForCoverageDTO;
import gov.cms.ab2d.coverage.model.CoverageMembership;
import gov.cms.ab2d.coverage.model.CoveragePagingRequest;
import gov.cms.ab2d.coverage.model.CoveragePeriod;
import gov.cms.ab2d.coverage.model.CoverageSearch;
import gov.cms.ab2d.coverage.model.CoverageSearchEvent;
import gov.cms.ab2d.coverage.model.Identifiers;
import gov.cms.ab2d.coverage.service.CoverageService;
import gov.cms.ab2d.coverage.util.AB2DCoverageLocalstackContainer;
import gov.cms.ab2d.coverage.util.AB2DCoveragePostgressqlContainer;
import gov.cms.ab2d.coverage.util.CoverageDataSetup;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static gov.cms.ab2d.common.util.PropertyConstants.OPT_OUT_ON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@SpringBootTest
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@EntityScan(basePackages = {"gov.cms.ab2d.common.model", "gov.cms.ab2d.coverage.model"})
@EnableJpaRepositories({"gov.cms.ab2d.common.repository", "gov.cms.ab2d.coverage.repository"})
@Testcontainers
@TestPropertySource(locations = "/application.coverage.properties")
@EnableFeignClients(clients = {ContractFeignClient.class})
@Import(LiquibaseTestConfig.class)
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
        insertCurrentMbi("mbi-1231", true);
        insertCurrentMbi("mbi-4561", true);
        insertCurrentMbi("mbi-7891", true);
        Assertions.assertEquals(3, coverageServiceRepository.countBeneficiariesByPeriods(List.of(period1Jan.getId()), "TST-12"));
    }

    @DisplayName("Calculate the number of beneficiaries when OptOut is enabled and share_data is false")
    @Test
    void countBeneficiariesByPeriodsWithOptOutTest() {
        Mockito.when(mockPropertiesService.isToggleOn(OPT_OUT_ON, false)).thenReturn(true);
        insertCurrentMbi("mbi-1231", false);
        insertCurrentMbi("mbi-4561", false);
        insertCurrentMbi("mbi-7891", false);
        Assertions.assertEquals(0, coverageServiceRepository.countBeneficiariesByPeriods(List.of(period1Jan.getId()), "TST-12"));
    }

    @DisplayName("Calculate the number of beneficiaries when OptOut is enabled and share_data is null")
    @Test
    void countBeneficiariesByPeriodsWithNullShareDataTest() {
        Mockito.when(mockPropertiesService.isToggleOn(OPT_OUT_ON, false)).thenReturn(true);
        insertCurrentMbi("mbi-1231", null);
        insertCurrentMbi("mbi-4561", null);
        insertCurrentMbi("mbi-7891", null);
        Assertions.assertEquals(3, coverageServiceRepository.countBeneficiariesByPeriods(List.of(period1Jan.getId()), "TST-12"));
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

    @Test
    void testAsMembership() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt(4)).thenReturn(2000);
        when(rs.getInt(5)).thenReturn(12);
        when(rs.getLong(1)).thenReturn(9L);
        when(rs.getString(2)).thenReturn("currentMbi");
        when(rs.getString(3)).thenReturn("historic,mbi,string");

        CoverageMembership coverageMembership = CoverageServiceRepository.asMembership(rs, 0);

        assertEquals(2000, coverageMembership.getYear());
        assertEquals(12, coverageMembership.getMonth());
        assertEquals(9L, coverageMembership.getIdentifiers().getBeneficiaryId());
        assertEquals("currentMbi", coverageMembership.getIdentifiers().getCurrentMbi());
        assertEquals(
            new LinkedHashSet<String>(Arrays.asList("historic", "mbi", "string")),
            coverageMembership.getIdentifiers().getHistoricMbis()
        );
    }

    @Test
    void testPageCoverage() {
        OffsetDateTime now = OffsetDateTime.of(1999, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC);
        ContractForCoverageDTO contract = new ContractForCoverageDTO(
            "stuff", now, ContractForCoverageDTO.ContractType.NORMAL
        );
        CoveragePagingRequest page = new CoveragePagingRequest(1, 1L, contract, now);

        assertThrows(
            IllegalArgumentException.class,
            () -> {coverageServiceRepository.pageCoverage(page);}
        );
    }

    private Identifiers createIdentifier(Long suffix) {
        return new Identifiers(suffix, "mbi-" + suffix, new LinkedHashSet<>());
    }

    private CoverageSearchEvent startSearchAndPullEvent() {
        Optional<CoverageSearch> search = coverageSearchRepo.findFirstByOrderByCreatedAsc();
        coverageSearchRepo.delete(search.get());
        return coverageService.startSearch(search.get(), "testing").get().getCoverageSearchEvent();
    }

    private void insertCurrentMbi(String mbi, Boolean shareData) {
        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);
        template.update(
                "delete from current_mbi where mbi = :mbi",
                new MapSqlParameterSource().addValue("mbi", mbi)
        );
        template.update(
                "insert into current_mbi (mbi, share_data) values (:mbi, :shareData)",
                new MapSqlParameterSource()
                        .addValue("mbi", mbi)
                        .addValue("shareData", shareData)
        );
    }
}
