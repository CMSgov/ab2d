package gov.cms.ab2d.coverage.service;

import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.coverage.model.*;
import gov.cms.ab2d.coverage.model.v3.CoverageV3Periods;
import gov.cms.ab2d.coverage.query.CountBeneficiariesByCoveragePeriods;
import gov.cms.ab2d.coverage.query.CoverageV3StagingService;
import gov.cms.ab2d.coverage.query.GetCoverageMembership;
import gov.cms.ab2d.coverage.query.GetCoveragePeriodsByContract;
import gov.cms.ab2d.coverage.repository.CoverageServiceRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.integration.support.locks.RenewableLockRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
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
    private final CoverageV3StagingService coverageV3StagingService;

    public CoverageV3ServiceImpl(
            DataSource dataSource,
            PropertiesService propertiesService,
            CoverageV3StagingService coverageV3StagingService) {
        this.dataSource = dataSource;
        this.propertiesService = propertiesService;
        this.coverageV3StagingService = coverageV3StagingService;
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
    @Transactional
    public boolean moveFromStagingToRecentCoverage(String contract) {
        return coverageV3StagingService.copyFromStagingTablesToRecent(contract);
    }

    @Override
    @Transactional
    public boolean moveFromStagingToHistoricalCoverage(String contract) {
        // TODO check for v3 jobs running
        // TODO acquire lock?

	    return coverageV3StagingService.copyFromStagingTablesToHistorical(contract);
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

    // TODO move this into a utility class
    public static <T> T executeTimedQuery(String queryDescription, Supplier<T> supplier) {
        val start = LocalDateTime.now();
        val result = supplier.get();
        val end = LocalDateTime.now();
        val duration = ChronoUnit.MILLIS.between(start, end);
        val durationSeconds = duration / 1000.0;
        log.info("Query completed in {}s: {}", durationSeconds, queryDescription);
        return result;
    }

}
