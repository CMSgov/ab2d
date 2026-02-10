package gov.cms.ab2d.coverage.service;

import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.coverage.model.*;
import gov.cms.ab2d.coverage.model.v3.CoverageV3Periods;
import gov.cms.ab2d.coverage.query.CountBeneficiariesByCoveragePeriods;
import gov.cms.ab2d.coverage.query.GetCoverageMembership;
import gov.cms.ab2d.coverage.query.GetCoveragePeriodsByContract;
import gov.cms.ab2d.coverage.repository.CoverageServiceRepository;
import gov.cms.ab2d.coverage.repository.v3.CoverageV3HistoricalRepository;
import gov.cms.ab2d.coverage.repository.v3.CoverageV3Repository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private final CoverageV3Repository coverageV3Repository;
    private final CoverageV3HistoricalRepository coverageV3HistoricalRepository;

    public CoverageV3ServiceImpl(
            DataSource dataSource,
            PropertiesService propertiesService,
            CoverageV3Repository coverageV3Repository,
            CoverageV3HistoricalRepository coverageV3HistoricalRepository
    ) {
        this.dataSource = dataSource;
        this.propertiesService = propertiesService;
        this.coverageV3Repository = coverageV3Repository;
        this.coverageV3HistoricalRepository = coverageV3HistoricalRepository;
    }

    public int countBeneficiariesByCoveragePeriod(final CoverageV3Periods periods, final String contract) {
        return new CountBeneficiariesByCoveragePeriods(dataSource).countBeneficiaries(contract, periods, isOptOutOn());
    }

    @Override
    public CoveragePagingResult pageCoverage(final CoveragePagingRequest page) {

        final ContractForCoverageDTO contract = page.getContract();
        final int expectedCoveragePeriods = CoverageServiceRepository.getExpectedCoveragePeriods(page);

        // Make sure all coverage periods are present so that there isn't any missing coverage data
        // Do not remove this check because it is a fail safe to guarantee that there isn't something majorly
        // wrong with the enrollment data.
        // A missing period = one month of enrollment missing for the contract
        final List<YearMonthRecord> coveragePeriods = new GetCoveragePeriodsByContract(dataSource).getCoveragePeriodsForContract(contract.getContractNumber());
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


    private List<CoverageMembership> queryCoverageMembership(CoveragePagingRequest page, long limit) {
        return new GetCoverageMembership(dataSource).getCoverageMembership(
            page.getContractNumber(),
            CoverageServiceRepository.YEARS,
            isOptOutOn(),
            limit,
            page.getCursor().orElse(null)
        );
    }

    protected boolean isOptOutOn() {
        return propertiesService.isToggleOn("OptOutOn", false);
    }

    protected boolean throwExceptionForCoveragePeriodMismatch() {
        return propertiesService.isToggleOn("OptOutOn", false);
    }
}
