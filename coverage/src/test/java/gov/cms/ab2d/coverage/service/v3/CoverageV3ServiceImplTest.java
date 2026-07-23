package gov.cms.ab2d.coverage.service.v3;

import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.contracts.model.ContractDTO;
import gov.cms.ab2d.coverage.CoverageV3PostgresContainer;
import gov.cms.ab2d.coverage.model.YearMonthRecord;
import gov.cms.ab2d.coverage.service.v3.audit.CoverageV3AuditLog;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Testcontainers
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class CoverageV3ServiceImplTest {

    @Container
    private static final CoverageV3PostgresContainer container = new CoverageV3PostgresContainer();

    @Mock
    PropertiesService propertiesService;

    @Mock
    Lock lock;

    @Mock
    CoverageV3LockWrapper lockWrapper;

    @Mock
    CoverageV3AuditLog audit;

    @Mock
    CoverageV3SyncMetrics metrics;

    CoverageV3ServiceImpl service;

    CoverageV3SyncServiceImpl syncService;

    @BeforeEach
    void setUp() {
        service = new CoverageV3ServiceImpl(container.getDataSource(), propertiesService, syncService);
        syncService = new CoverageV3SyncServiceImpl(container.getDataSource(), lockWrapper, lockWrapper, audit, metrics, propertiesService);
    }

    @Test
    void test() {
        assertTrue(
            service.shouldDeleteAggregatedTable(
                "coverage_v3_aggregated_z0000",
                List.of("z0001")
            )
        );

        assertFalse(
            service.shouldDeleteAggregatedTable(
                "coverage_v3_aggregated_z0001",
                List.of("z0001")
            )
        );

        assertFalse(
            service.shouldDeleteAggregatedTable(
                "coverage_v3_aggregated_z0001",
                List.of("Z0001")
            )
        );
    }


    @Test
    void blah() {
        val contractZ9999 = "Z9999";
        val contractDtoZ9999 = new ContractDTO();
        contractDtoZ9999.setContractNumber(contractZ9999);

        val contractZ7777 = "Z7777";
        val contractDtoZ7777 = new ContractDTO();
        contractDtoZ7777.setContractNumber(contractZ7777);

        syncService.populateHistorySummaryForContract(contractZ9999);
        syncService.populateHistorySummaryCoveragePeriodsForContract(contractZ9999);

        syncService.populateHistorySummaryForContract(contractZ7777);
        syncService.populateHistorySummaryCoveragePeriodsForContract(contractZ7777);

        val coveragePeriods = service.getCoveragePeriods(List.of(contractDtoZ9999, contractDtoZ7777));

        assertEquals(coveragePeriods.get("Z7777").toString(), "[YearMonthRecord(year=2026, month=2), YearMonthRecord(year=2026, month=1), YearMonthRecord(year=2025, month=12)]");
        assertEquals(coveragePeriods.get("Z9999").toString(), "[YearMonthRecord(year=2026, month=2), YearMonthRecord(year=2026, month=1), YearMonthRecord(year=2025, month=12), YearMonthRecord(year=2025, month=11), YearMonthRecord(year=2025, month=10), YearMonthRecord(year=2025, month=9)]");

    }

}
