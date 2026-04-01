package gov.cms.ab2d.coverage.service;

import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.coverage.model.*;
import gov.cms.ab2d.coverage.model.v3.CoverageV3Periods;
import gov.cms.ab2d.coverage.query.CountBeneficiariesByCoveragePeriods;
import gov.cms.ab2d.coverage.query.CoverageV3StagingHelper;
import gov.cms.ab2d.coverage.query.GetCoverageMembership;
import gov.cms.ab2d.coverage.query.GetCoveragePeriodsByContract;
import gov.cms.ab2d.coverage.repository.CoverageServiceRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@Transactional
public class CoverageV3ServiceImpl implements CoverageV3Service {

    /**
     * If true, throw exception if number of expected coverage period is not equal to actual number.
     * In production, this should be set to true.
     */
    public static final boolean THROW_EXCEPTION_FOR_COVERAGE_PERIOD_MISMATCH = false;

    private final DataSource dataSource;
    private final PropertiesService propertiesService;

    public CoverageV3ServiceImpl(DataSource dataSource, PropertiesService propertiesService) {
        this.dataSource = dataSource;
        this.propertiesService = propertiesService;
    }

    @Override
    public CoveragePagingResult pageCoverage(final CoveragePagingRequest page) {

        final ContractForCoverageDTO contract = page.getContract();
        final int expectedCoveragePeriods = CoverageServiceRepository.getExpectedCoveragePeriods(page);

        // Make sure all coverage periods are present so that there isn't any missing coverage data
        // Do not remove this check because it is a fail safe to guarantee that there isn't something majorly
        // wrong with the enrollment data.
        // A missing period = one month of enrollment missing for the contract
        final List<YearMonthRecord> coveragePeriods = getCoveragePeriodsByContract(page.getContractNumber());
        if (coveragePeriods.size() != expectedCoveragePeriods) {
            if (THROW_EXCEPTION_FOR_COVERAGE_PERIOD_MISMATCH) {
                throw new IllegalArgumentException(
                        "at least one coverage period missing from enrollment table for contract "
                                + page.getContract().getContractNumber()
                );
            } else {
                log.warn("Expected coverage periods = {}; Actual coverage periods = {}", expectedCoveragePeriods, coveragePeriods.size());

            }
        }

        // Determine how many records to pull back
        final long limit = CoverageServiceRepository.getCoverageLimit(page.getPageSize(), expectedCoveragePeriods);

        // Query coverage membership from database and collect it
        final List<CoverageMembership> enrollment = queryCoverageMembership(page, limit);

        // Guarantee ordering of results to the order that the beneficiaries were returned from SQL
        final Map<Long, List<CoverageMembership>> enrollmentByBeneficiary =
                CoverageServiceRepository.aggregateEnrollmentByPatient(expectedCoveragePeriods, enrollment);

        // Only summarize page size beneficiaries worth of information and report it
        final List<CoverageSummary> beneficiarySummaries = enrollmentByBeneficiary.entrySet().stream()
                .limit(page.getPageSize())
                .map(membershipEntry -> CoverageServiceRepository.summarizeCoverageMembership(contract, membershipEntry))
                .collect(toList());

        // Get the patient to start from next time
        final Optional<Map.Entry<Long, List<CoverageMembership>>> nextCursor =
                enrollmentByBeneficiary.entrySet().stream().skip(page.getPageSize()).findAny();

        // Build the next request if there is a next patient
        CoveragePagingRequest request = null;
        if (nextCursor.isPresent()) {
            Map.Entry<Long, List<CoverageMembership>> nextCursorBeneficiary = nextCursor.get();
            request = new CoveragePagingRequest(page.getPageSize(), nextCursorBeneficiary.getKey(), contract, page.getJobStartTime());
        }

        return new CoveragePagingResult(beneficiarySummaries, request);
    }

    @Override
    public boolean copyFromStagingTable(String contract) {
        val stagingHelper = new CoverageV3StagingHelper(dataSource);
        val rowsInStaging = executeTimedQuery(
            format("getCoveragePeriodCountForCoverageV3Staging contract=%s", contract),
            () -> stagingHelper.getCoveragePeriodCountForCoverageV3Staging(contract)
        );
        log.info("Found {} rows in staging table for contract {}", rowsInStaging, contract);
        if (rowsInStaging == 0) {
            return true;
        }

        val rowsInCoverageBeforeCopy = executeTimedQuery(
            format("getCoveragePeriodCountForCoverageV3 contract=%s", contract),
            () -> stagingHelper.getCoveragePeriodCountForCoverageV3(contract)
        );
        log.info("Found {} rows in coverage table for contract {}", rowsInCoverageBeforeCopy, contract);

        log.info("Preparing to delete rows in coverage table for contract {}...", contract);
        val rowsInCoverageDeleted = executeTimedQuery(
            format("deleteFromCoverageAndGetRowsDeleted contract=%s", contract),
            () -> stagingHelper.deleteFromCoverageAndGetRowsDeleted(contract)
        );
        log.info("Deleted {} rows in coverage table for contract {}", rowsInCoverageDeleted, contract);

        log.info("Preparing to copy rows from staging to coverage for contract {}...", contract);
        val rowsInserted = executeTimedQuery(
            format("copyFromStagingToCoverage contract=%s", contract),
            () -> stagingHelper.copyFromStagingToCoverage(contract)
        );
        log.info("Copied {} rows from staging to coverage for contract {}", rowsInserted, contract);

        val rowsInCoverageAfterCopy = executeTimedQuery(
            format("getCoveragePeriodCountForCoverageV3 contract=%s", contract),
            () -> stagingHelper.getCoveragePeriodCountForCoverageV3(contract)
        );
        log.info("Coverage table now contains {} rows for contract {}", rowsInCoverageAfterCopy, contract);

        if (!rowsInStaging.equals(rowsInCoverageAfterCopy)) {
            log.error("Row count in staging ({}) != row count in coverage ({}) for contract {}",
                rowsInStaging,
                rowsInCoverageBeforeCopy,
                contract
            );
            return false;
        }

        log.info("Preparing to delete rows in staging for contract {}", contract);
        val rowsInStagingDeleted = executeTimedQuery(
            format("deleteFromStagingAndGetRowsDeleted contract=%s", contract),
            () -> stagingHelper.deleteFromStagingAndGetRowsDeleted(contract)
        );

        if (!rowsInStagingDeleted.equals(rowsInCoverageAfterCopy)) {
            log.error("Row count deleted from staging ({}) != row count in coverage ({}) for contract {}",
                rowsInStagingDeleted,
                rowsInCoverageAfterCopy,
                contract
            );
            return false;
        }

        return true;
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

    private <T> T executeTimedQuery(String queryDescription, Supplier<T> supplier) {
        val start = LocalDateTime.now();
        val result = supplier.get();
        val end = LocalDateTime.now();
        val duration = ChronoUnit.MILLIS.between(start, end);
        val durationSeconds = duration / 1000.0;
        log.info("Query completed in {}s: {}", durationSeconds, queryDescription);
        return result;
    }

}
