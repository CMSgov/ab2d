package gov.cms.ab2d.coverage.service.v3;

import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.contracts.model.ContractDTO;
import gov.cms.ab2d.coverage.CoverageV3PostgresContainer;
import gov.cms.ab2d.coverage.model.ContractForCoverageDTO;
import gov.cms.ab2d.coverage.model.CoveragePagingRequest;
import gov.cms.ab2d.coverage.model.CoveragePagingResult;
import gov.cms.ab2d.coverage.model.YearMonthRecord;
import gov.cms.ab2d.coverage.model.v3.CoverageV3Periods;
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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;

import static gov.cms.ab2d.common.util.PropertyConstants.V3_ON;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    CoverageV3ServiceImpl service;

    CoverageV3SyncServiceImpl syncService;

    @BeforeEach
    void setUp() {
        syncService = new CoverageV3SyncServiceImpl(container.getDataSource(), lockWrapper, lockWrapper, audit, propertiesService);
        service = new CoverageV3ServiceImpl(container.getDataSource(), propertiesService, syncService);
    }

    @Test
    void testPageCoverage_patientsOptedOut() {
        val contract = "Z9999";
        val contractDto = new ContractForCoverageDTO();
        contractDto.setContractNumber(contract);
        val coveragePagingRequest = CoveragePagingRequest.ofV3(100, -1L, contractDto, OffsetDateTime.now());

        syncService.populateHistorySummaryForContract(contract);
        service.createAggregatedAttributionTable(contract);
        // single patient has share_data=false, so nothing should be returned
        val result = service.pageCoverage(coveragePagingRequest);
        assertEquals(0, result.size());
    }

    @Test
    void testPageCoverage() {
        val contract = "Z0000";
        val contractDto = new ContractForCoverageDTO();
        contractDto.setContractNumber(contract);
        val coveragePagingRequest = CoveragePagingRequest.ofV3(10, -1L, contractDto, OffsetDateTime.now());

        syncService.populateHistorySummaryForContract(contract);
        service.createAggregatedAttributionTable(contract);

        var result = service.pageCoverage(coveragePagingRequest);
        assertEquals(1, result.size());

        assertTrue(result.getNextRequest().isPresent());
        result = service.pageCoverage(result.getNextRequest().get());
        assertEquals(0, result.size());

    }

    @Test
    void testGetCoveragePeriods() {
        val contract = "Z0000";
        val contractDto = new ContractDTO();
        contractDto.setContractNumber(contract);
        syncService.populateHistorySummaryForContract(contract);
        syncService.populateHistorySummaryCoveragePeriodsForContract(contract);
        val coveragePeriods = service.getCoveragePeriods(List.of(contractDto));
        assertEquals(9, coveragePeriods.get(contract).size());
    }

    @Test
    void testDeleteAggregatedTable() {
        val contract = "Z0000";
        syncService.populateHistorySummaryForContract(contract);
        service.createAggregatedAttributionTable(contract);

        val tableName = "v3.coverage_v3_aggregated_z0000";

        assertTrue(tableExists(tableName));

        service.deleteAggregatedTable(tableName);
        assertFalse(tableExists(tableName));
    }

    @Test
    void testDeleteAggregatedTableForContract() {
        val contract = "Z0000";
        syncService.populateHistorySummaryForContract(contract);
        service.createAggregatedAttributionTable(contract);

        val tableName = "v3.coverage_v3_aggregated_z0000";
        assertTrue(tableExists(tableName));

        service.deleteAggregatedTableForContract(contract, Optional.empty());
        assertFalse(tableExists(tableName));
    }

    @Test
    void testShouldDeleteAggregatedTable() {
        val tableName = "v3.coverage_v3_aggregated_z0000";
        // no running jobs for z0000 - should delete
        assertTrue(service.shouldDeleteAggregatedTable(tableName.toLowerCase(), List.of()));

        // one job for z0000 - should not delete
        assertFalse(service.shouldDeleteAggregatedTable(tableName, List.of("Z0000")));

        // property is set to prevent deletion of table - should not delete
        val property = "v3.coverage_v3_aggregated_z0000.keep";
        when(propertiesService.isToggleOn(property, false)).thenReturn(true);
        assertFalse(service.shouldDeleteAggregatedTable(tableName, List.of()));
    }

    @Test
    void testGetCoveragePeriodsInAggregatedTable() {
        val contract = "Z0000";
        syncService.populateHistorySummaryForContract(contract);
        service.createAggregatedAttributionTable(contract);
        assertEquals(9, service.getCoveragePeriodsInAggregatedTable(contract));
    }

    @Test
    void testCheckForAggregatedTablesToBeDeleted() {
        val contract = "Z0000";
        syncService.populateHistorySummaryForContract(contract);
        service.createAggregatedAttributionTable(contract);

        val tableName = "v3.coverage_v3_aggregated_z0000";
        assertTrue(tableExists(tableName));

        service.checkForAggregatedTablesToBeDeleted();
        assertFalse(tableExists(tableName));
    }

    @Test
    void testGetDistinctPatientCount() {
        val contract = "Z0000";
        syncService.populateHistorySummaryForContract(contract);
        service.createAggregatedAttributionTable(contract);
        assertEquals(1, service.getDistinctPatientCount(contract));
    }

    boolean tableExists(String tableName)  {
        val query = "SELECT to_regclass('%s') IS NOT NULL".formatted(tableName);
        return new JdbcTemplate(container.getDataSource()).queryForObject(query, Boolean.class);
    }

}
