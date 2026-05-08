package gov.cms.ab2d.coverage.service.v3;

import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.coverage.model.*;
import gov.cms.ab2d.coverage.model.v3.CoverageV3Count;
import gov.cms.ab2d.coverage.model.v3.CoverageV3Periods;
import gov.cms.ab2d.coverage.query.*;
import gov.cms.ab2d.coverage.repository.CoverageServiceRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static java.lang.String.format;

@Slf4j
@Service
@Transactional
public class CoverageV3ServiceImpl implements CoverageV3Service {

    private final DataSource dataSource;
    private final PropertiesService propertiesService;
    private final CoverageV3SyncService coverageV3SyncService;

    public CoverageV3ServiceImpl(
            DataSource dataSource,
            PropertiesService propertiesService,
            CoverageV3SyncService coverageV3SyncService) {
        this.dataSource = dataSource;
        this.propertiesService = propertiesService;
        this.coverageV3SyncService = coverageV3SyncService;
    }

    @Override
    public CoveragePagingResult pageCoverage(final CoveragePagingRequest page) {
        final ContractForCoverageDTO contract = page.getContract();

        // For v3, perform coverage periods check only once (expected vs actual coverage periods) instead for every batch
        // on subsequent batches, page.getCursor() will be populated -- only on first batch will it be absent
        if (page.getCursor().isEmpty()) {

            final int expectedCoveragePeriods = CoverageServiceRepository.getExpectedCoveragePeriods(page);

            // Make sure all coverage periods are present so that there isn't any missing coverage data
            // Do not remove this check because it is a fail safe to guarantee that there isn't something majorly
            // wrong with the enrollment data.
            // A missing period = one month of enrollment missing for the contract

            //final int coveragePeriodsSize = getCoveragePeriodsByContract(page.getContractNumber()).size();
            final int coveragePeriodsSize = new GetAggregatedCoverageMembership(dataSource).getCoveragePeriodsInAggregatedTable(contract.getContractNumber());

            log.info("coveragePeriods.size() = {}; expectedCoveragePeriods = {}", coveragePeriodsSize, expectedCoveragePeriods);
            if (coveragePeriodsSize != expectedCoveragePeriods) {
                val contractNumber = page.getContract().getContractNumber();
                val message = "[V3] Expected coverage periods (%d) and actual coverage periods (%d) do not match for contract %s"
                        .formatted(expectedCoveragePeriods, coveragePeriodsSize, contractNumber);
                log.warn(message);

                // TODO -- re-enable this check; disabled while testing in ephemeral environment
                /*
                if (!contractNumber.startsWith("Z")) {
                    throw new IllegalArgumentException(message);
                }
                 */
            }
        }


        // Determine how many records to pull back
        //final long limit = CoverageServiceRepository.getCoverageLimit(page.getPageSize(), expectedCoveragePeriods);
        final long limit = 1000;

        // Query coverage membership from database and collect it
        /*
        final List<CoverageMembership> enrollment = queryCoverageMembership(page, limit);
        log.info("[V3] List<CoverageMembership> enrollment size = {}", enrollment.size());

        // Guarantee ordering of results to the order that the beneficiaries were returned from SQL
        final Map<Long, List<CoverageMembership>> enrollmentByBeneficiary =
                CoverageServiceRepository.aggregateEnrollmentByPatient(expectedCoveragePeriods, enrollment);
        log.info("[V3] enrollmentByBeneficiary size = {}", enrollmentByBeneficiary.size());

        // Only summarize page size beneficiaries worth of information and report it
        final List<CoverageSummary> beneficiarySummaries = enrollmentByBeneficiary.entrySet().stream()
                .limit(page.getPageSize())
                .map(membershipEntry -> CoverageServiceRepository.summarizeCoverageMembership(contract, membershipEntry))
                .collect(toList());
         */
        val beneficiarySummaries = queryAggregatedCoverageMembership(page, limit);
        log.info("[V3] List<CoverageSummary> beneficiarySummaries size = {}", beneficiarySummaries.size());


        // Get the patient to start from next time
//        final Optional<Map.Entry<Long, List<CoverageMembership>>> nextCursor =
//                enrollmentByBeneficiary.entrySet().stream().skip(page.getPageSize()).findAny();

        // Build the next request if there is a next patient
        CoveragePagingRequest request = null;
        if (!beneficiarySummaries.isEmpty()) {
            val nextCursor = beneficiarySummaries.get(beneficiarySummaries.size()-1).getIdentifiers().getPatientIdV3();
            request = CoveragePagingRequest.ofV3(page.getPageSize(), nextCursor, contract, page.getJobStartTime());
        }

        return new CoveragePagingResult(beneficiarySummaries, request);
    }

    @Override
    public Map<String, List<CoverageV3Count>>  getCoverageCount() {
        return new GetCoverageV3Count(dataSource).coverageCounts();
    }

    @Override
    public boolean idrImportInProgress() {
        return coverageV3SyncService.idrImporterInProgress();
    }

    @Override
    public void createAggregatedAttributionTable(String contract) {
        new GetAggregatedCoverageMembership(dataSource).createAggregatedAttributionTable(contract);
    }

    @Override
    public void deleteAggregatedAttributionTable(String contract) {
        new GetAggregatedCoverageMembership(dataSource).deleteAggregatedTable(contract);
    }

    @Override
    public void deleteAggregatedTable(String tableName) {

    }

    @Override
    public int getAggregatedTableRowCount(String contract) {
        return new GetAggregatedCoverageMembership(dataSource).getAggregatedTableRowCount(contract);
    }

    @Override
    public int getCoveragePeriodsInAggregatedTable(String contract) {
        return new GetAggregatedCoverageMembership(dataSource).getCoveragePeriodsInAggregatedTable(contract);
    }

    @Override
    public void checkForAggregatedTablesToBeDeleted() {
        val query =
        """
        SELECT table_name
        FROM information_schema.tables
        WHERE table_schema = 'v3'
          AND table_type = 'BASE TABLE'
          AND table_name like 'coverage_v3_aggregated%';
        """;

        val aggregatedTables = new JdbcTemplate(dataSource).queryForList(query, String.class);
        val contractsWithActiveJobs = coverageV3SyncService.getContractsWithActiveV3Jobs();

        for (String aggregatedTable : aggregatedTables) {
            if (shouldDeleteAggregatedTable(aggregatedTable, contractsWithActiveJobs)) {
                log.info("Deleting unused aggregated table {}", aggregatedTable);
                deleteAggregatedAttributionTable(aggregatedTable);
                log.info("Deleted table {}", aggregatedTable);
            }
        }
    }

    protected boolean shouldDeleteAggregatedTable(final String tableName, final List<String> contactsWithActiveJobs) {
        for (String contactsWithActiveJob : contactsWithActiveJobs) {
            // Keep table for debugging purposes if true
            val keepTable = propertiesService.isToggleOn("%s.keep".formatted(tableName), false);
            if (keepTable) {
                log.info("Skipping deletion for aggregated table {}", tableName);
                return false;
            }

            if (tableName.toLowerCase().endsWith(contactsWithActiveJob.toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    @Override
    @Transactional
    public CoverageV3SyncResult moveFromStagingToRecentCoverage(String contract, CoverageV3SyncSource source) {
        return coverageV3SyncService.copyFromStagingTablesToRecent(contract, source);
    }

    @Override
    @Transactional
    public CoverageV3SyncResult moveOldCoverageToHistoricalCoverage(String contract, CoverageV3SyncSource source) {
	    return coverageV3SyncService.moveToHistorical(contract, source);
    }


    @Override
    public int countBeneficiariesByCoveragePeriod(final CoverageV3Periods periods, final String contract) {
        return executeTimedQuery(
            format("countBeneficiariesByCoveragePeriod historicalCoverage=%s; recentCoverage=%s; contract=%s",
                    periods.getHistoricalCoverage(),
                    periods.getRecentCoverage(),
                    contract
            ),
            () -> new CountBeneficiariesByCoveragePeriods(dataSource)
                    .countBeneficiaries(contract, periods, isOptOutOn())
        );
    }

    private List<CoverageSummary> queryAggregatedCoverageMembership(CoveragePagingRequest page, long limit) {
        return executeTimedQuery(
            format("queryAggregatedCoverageMembership page=%s", page),
            () -> new GetAggregatedCoverageMembership(dataSource).fetchAggregatedData(
                    page.getContract(),
                    limit,
                    page.getCursor()
            )
        );
    }

    private List<CoverageMembership> queryCoverageMembership(CoveragePagingRequest page, long limit) {
        return executeTimedQuery(
            format("queryCoverageMembership page=%s", page),
            () -> new GetCoverageMembership(dataSource).getCoverageMembership(
                page.getContractNumber(),
                CoverageServiceRepository.YEARS,
                isOptOutOn(),
                limit,
                page.getCursor().orElse(null)
            )
        );
    }

    private List<YearMonthRecord> getCoveragePeriodsByContract(final String contract) {
        return executeTimedQuery(
            format("getCoveragePeriodsByContract contract=%s", contract),
            () -> new GetCoveragePeriodsByContract(dataSource).getCoveragePeriodsForContract(contract)
        );
    }

    private boolean isOptOutOn() {
        return propertiesService.isToggleOn("OptOutOn", false);
    }

    public static <T> T executeTimedQuery(String queryDescription, Supplier<T> supplier) {
        val start = LocalDateTime.now();
        val result = supplier.get();
        val end = LocalDateTime.now();
        val duration = ChronoUnit.MILLIS.between(start, end);
        val durationSeconds = duration / 1000.0;
        log.info("[V3] Query completed in {}s: {}", durationSeconds, queryDescription);
        return result;
    }

}
