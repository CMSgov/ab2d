package gov.cms.ab2d.common.repository;

import com.newrelic.api.agent.Trace;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.CoverageMembership;
import gov.cms.ab2d.common.model.CoveragePagingRequest;
import gov.cms.ab2d.common.model.CoveragePagingResult;
import gov.cms.ab2d.common.model.CoveragePeriod;
import gov.cms.ab2d.common.model.CoverageSearchEvent;
import gov.cms.ab2d.common.model.CoverageSummary;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.model.Identifiers;
import gov.cms.ab2d.common.util.FilterOutByDate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static gov.cms.ab2d.common.util.DateUtil.AB2D_EPOCH;
import static gov.cms.ab2d.common.util.DateUtil.AB2D_ZONE;
import static java.util.stream.Collectors.toList;

@Slf4j
@Repository
public class CoverageServiceRepository {

    private static final int BATCH_INSERT_SIZE = 10000;
    private static final List<Integer> YEARS = List.of(2020, 2021, 2022, 2023);

    private static final String INSERT_COVERAGE = "INSERT INTO coverage " +
            "(bene_coverage_period_id, bene_coverage_search_event_id, contract, year, month, beneficiary_id, current_mbi, historic_mbis) " +
            "VALUES(?,?,?,?,?,?,?,?)";

    private static final String SELECT_COVERAGE_BY_SEARCH_COUNT = "SELECT COUNT(*) FROM coverage " +
            " WHERE bene_coverage_search_event_id = :id AND contract = :contract AND year IN (:years)";

    private static final String SELECT_DISTINCT_COVERAGE_BY_PERIOD_COUNT = "SELECT COUNT(DISTINCT beneficiary_id) FROM coverage" +
            " WHERE bene_coverage_period_id IN(:ids) AND contract = :contract AND year IN (:years)";

    static final String SELECT_DELTA =
            "SELECT cov1.bene_coverage_period_id, cov1.beneficiary_id, :type as entryType, CURRENT_TIMESTAMP as created" +
            " FROM coverage cov1" +
            " WHERE cov1.bene_coverage_search_event_id = :search1 AND NOT EXISTS" +
                " (SELECT cov2.beneficiary_id FROM coverage cov2" +
                " WHERE cov1.beneficiary_id = cov2.beneficiary_id and bene_coverage_search_event_id = :search2 )";

    private static final String SELECT_INTERSECTION = "SELECT COUNT(*) FROM (" +
            " SELECT DISTINCT beneficiary_id FROM coverage WHERE bene_coverage_search_event_id = :search1 AND contract = :contract AND year IN (:years)" +
            " INTERSECT " +
            " SELECT DISTINCT beneficiary_id FROM coverage WHERE bene_coverage_search_event_id = :search2 AND contract = :contract AND year IN (:years)" +
            ") I";

    private static final String SELECT_COVERAGE_WITHOUT_CURSOR =
            "SELECT beneficiary_id, current_mbi, historic_mbis, year, month " +
            " FROM coverage " +
            " WHERE contract = :contract and year IN (:years) " +
            " ORDER BY beneficiary_id " +
            " LIMIT :limit";

    private static final String SELECT_COVERAGE_WITH_CURSOR =
            "SELECT beneficiary_id, current_mbi, historic_mbis, year, month " +
            " FROM coverage " +
            " WHERE contract = :contract and year IN (:years) AND beneficiary_id >= :cursor " +
            " ORDER BY beneficiary_id " +
            " LIMIT :limit";

    private final DataSource dataSource;
    private final CoveragePeriodRepository coveragePeriodRepo;
    private final CoverageSearchEventRepository coverageSearchEventRepo;

    public CoverageServiceRepository(DataSource dataSource, CoveragePeriodRepository coveragePeriodRepo,
                                     CoverageSearchEventRepository coverageSearchEventRepo) {
        this.dataSource = dataSource;
        this.coverageSearchEventRepo = coverageSearchEventRepo;
        this.coveragePeriodRepo = coveragePeriodRepo;
    }

    @Trace
    public int countBySearchEvent(CoverageSearchEvent searchEvent) {

        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("id", searchEvent.getId())
                .addValue("contract", searchEvent.getCoveragePeriod().getContract().getContractNumber())
                .addValue("years", YEARS);

        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);

        return template.queryForList(SELECT_COVERAGE_BY_SEARCH_COUNT, parameters, Integer.class)
                .stream().findFirst().orElseThrow(() -> new RuntimeException("no coverage information found for any" +
                "of the coverage periods provided"));
    }

    @Trace
    public int countIntersection(CoverageSearchEvent searchEvent1, CoverageSearchEvent searchEvent2) {

        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("search1", searchEvent1.getId())
                .addValue("search2", searchEvent2.getId())
                .addValue("contract", searchEvent1.getCoveragePeriod().getContract().getContractNumber())
                .addValue("years", YEARS);

        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);

        return template.queryForList(SELECT_INTERSECTION, parameters, Integer.class)
                .stream().findFirst().orElseThrow(() -> new RuntimeException("no coverage information found for any" +
                        "of the coverage periods provided"));
    }

    public int countBeneficiariesByPeriods(List<Integer> coveragePeriodIds, String contractNum) {

        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("ids", coveragePeriodIds)
                .addValue("contract", contractNum)
                .addValue("years", YEARS);

        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);

        return template.queryForList(SELECT_DISTINCT_COVERAGE_BY_PERIOD_COUNT, parameters, Integer.class)
                .stream().findFirst().orElseThrow(() -> new RuntimeException("no coverage information found for any" +
                                "of the coverage periods provided"));
    }

    /**
     * Insert a batch of patients using plain jdbc
     * @param searchEvent the search event to add coverage in relation to
     * @param beneIds Collection of beneficiary ids to be added as a batch
     */
    @Trace
    public void insertBatches(CoverageSearchEvent searchEvent, Iterable<Identifiers> beneIds) {

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_COVERAGE)) {

            int processingCount = 0;

            String contractNum = searchEvent.getCoveragePeriod().getContract().getContractNumber();
            int year = searchEvent.getCoveragePeriod().getYear();
            int month = searchEvent.getCoveragePeriod().getMonth();

            // Prepare a batch of beneficiary ids to be inserted
            // and periodically conduct an insert if the batch size is large enough
            for (Identifiers beneficiary : beneIds) {
                processingCount++;

                prepareCoverageInsertion(statement, contractNum, year, month, searchEvent, beneficiary);

                if (processingCount % BATCH_INSERT_SIZE == 0) {
                    executeBatch(statement);
                    processingCount = 0;
                }
            }

            if (processingCount > 0) {
                executeBatch(statement);
            }

        } catch (SQLException sqlException) {
            throw new RuntimeException("failed to insert coverage information", sqlException);
        }
    }

    @Trace
    private void executeBatch(PreparedStatement statement) throws SQLException {
        statement.executeBatch();
    }

    private void prepareCoverageInsertion(PreparedStatement statement, String contractNum, int year, int month, CoverageSearchEvent searchEvent, Identifiers beneficiary) throws SQLException {
        statement.setInt(1, searchEvent.getCoveragePeriod().getId());
        statement.setLong(2, searchEvent.getId());
        statement.setString(3, contractNum);
        statement.setInt(4, year);
        statement.setInt(5, month);
        statement.setLong(6, beneficiary.getBeneficiaryId());
        statement.setString(7, beneficiary.getCurrentMbi());

        if (beneficiary.getHistoricMbis().isEmpty()) {
            statement.setString(8, null);
        } else {
            statement.setString(8, String.join(",", beneficiary.getHistoricMbis()));
        }

        statement.addBatch();
    }

    /**
     * Delete all coverage information related to the results of a search defined by an offset into the past.
     *
     * An offset of 0 finds the current IN_PROGRESS search event and deletes all coverage information associated
     * with that event.
     *
     * @param period coverage period to remove
     * @param offset offset into the past 0 is last search done successfully, 1 is search before that, etc.
     */
    public void deletePreviousSearch(CoveragePeriod period, int offset) {

        Optional<CoverageSearchEvent> searchEvent = coverageSearchEventRepo
                .findSearchEventWithOffset(period.getId(), JobStatus.IN_PROGRESS.name(), offset);

        // Only delete previous search if a previous search exists.
        // For performance reasons this is done via jdbc
        if (searchEvent.isPresent()) {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("DELETE FROM coverage cov WHERE " +
                         "cov.bene_coverage_search_event_id = ?")) {
                statement.setLong(1, searchEvent.get().getId());
                statement.execute();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }

            vacuumCoverage();
        }
    }

    public CoveragePagingResult pageCoverage(CoveragePagingRequest page) {

        Contract contract = page.getContract();
        int expectedCoveragePeriods = getExpectedCoveragePeriods(page);

        List<CoveragePeriod> coveragePeriods = coveragePeriodRepo.findAllByContractId(contract.getId());
        if (coveragePeriods.size() != expectedCoveragePeriods) {
            throw new IllegalArgumentException("at least one coverage period missing from enrollment table for contract "
                    + page.getContract().getContractNumber());
        }

        long limit = getCoverageLimit(page.getPageSize(), expectedCoveragePeriods);
        Optional<Long> pageCursor = page.getCursor();

        MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue("contract", page.getContractNumber())
                .addValue("years", YEARS)
                .addValue("limit", limit);

        pageCursor.ifPresent((cursor) -> {
            sqlParameterSource.addValue("cursor", cursor);
        });

        List<CoverageMembership> enrollment;
        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);
        if (pageCursor.isPresent()) {
            enrollment = template.query(SELECT_COVERAGE_WITH_CURSOR, sqlParameterSource,
                    CoverageServiceRepository::asMembership);
        } else {
            enrollment = template.query(SELECT_COVERAGE_WITHOUT_CURSOR, sqlParameterSource,
                    CoverageServiceRepository::asMembership);
        }

        // Guarantee ordering of results to the order that the beneficiaries were returned from SQL
        Map<Long, List<CoverageMembership>> enrollmentByBeneficiary = new LinkedHashMap<>();

        // Guarantee insertion order. Could use functional API in future.
        Iterator<CoverageMembership> enrollmentIterator = enrollment.iterator();
        while (enrollmentIterator.hasNext()) {
            CoverageMembership coverageMembership = enrollmentIterator.next();

            // If not present add to mapping
            Long beneficiaryId = coverageMembership.getIdentifiers().getBeneficiaryId();
            enrollmentByBeneficiary.putIfAbsent(beneficiaryId,
                    new ArrayList<>(expectedCoveragePeriods));
            enrollmentByBeneficiary.get(beneficiaryId).add(coverageMembership);
        }

        // Only summarize page size beneficiaries worth of information and report it
        List<CoverageSummary> beneficiarySummaries = enrollmentByBeneficiary.entrySet().stream()
                .limit(page.getPageSize())
                .map(membershipEntry -> summarizeCoverageMembership(contract, membershipEntry))
                .collect(toList());

        Optional<Map.Entry<Long, List<CoverageMembership>>> nextCursor =
                enrollmentByBeneficiary.entrySet().stream().skip(page.getPageSize()).findAny();

        CoveragePagingRequest request = null;
        if (nextCursor.isPresent()) {
            Map.Entry<Long, List<CoverageMembership>> nextCursorBeneficiary = nextCursor.get();
            request = new CoveragePagingRequest(page.getPageSize(), nextCursorBeneficiary.getKey(), contract, page.getJobStartTime());
        }

        return new CoveragePagingResult(beneficiarySummaries, request);
    }

    /**
     * Get the number of enrollment entries expected per patient assuming each patient is
     * a member of the contract since it was attested for.
     * @return the maximum number of entries required from the database to a pageSize worth of beneficiaries
     */
    private long getCoverageLimit(int pageSize, long expectedCoveragePeriods) {
        return expectedCoveragePeriods * (pageSize + 1);
    }

    private int getExpectedCoveragePeriods(CoveragePagingRequest pagingRequest) {
        OffsetDateTime jobStartTime = pagingRequest.getJobStartTime();

        ZonedDateTime startTime = pagingRequest.getContract().getESTAttestationTime();
        if (startTime.isBefore(AB2D_EPOCH)) {
            startTime = AB2D_EPOCH;
        }

        // MONTHS.between is exclusive and only counts full months so
        // January 15th - February 1st would return 0 and not 2 like it needs
        // We have a coverage period for each month
        // January 1st - March 1st
        startTime = startTime.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        ZonedDateTime endTime = jobStartTime.atZoneSameInstant(AB2D_ZONE)
                .plusMonths(1).truncatedTo(ChronoUnit.DAYS).plusSeconds(1);
        return (int) ChronoUnit.MONTHS.between(startTime, endTime);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    public static CoverageMembership asMembership(ResultSet rs, int rowNum) throws SQLException {
        Identifiers identifiers = asIdentifiers(rs);
        return new CoverageMembership(identifiers, rs.getInt(4), rs.getInt(5));
    }

    private static Identifiers asIdentifiers(ResultSet rs) throws SQLException {

        LinkedHashSet<String> historicMbis = new LinkedHashSet<>();

        String historicMbiString = rs.getString(3);
        if (StringUtils.isNotBlank(historicMbiString)) {
            String[] mbis = historicMbiString.split(",");

            historicMbis.addAll(Arrays.asList(mbis));
        }

        return new Identifiers(rs.getLong(1), rs.getString(2), historicMbis);
    }

    /**
     * Summarize the coverage of one beneficiary for
     */
    private CoverageSummary summarizeCoverageMembership(Contract contract,
                                                        Map.Entry<Long, List<CoverageMembership>> membershipInfo) {

        List<CoverageMembership> membershipMonths = membershipInfo.getValue();
        Identifiers identifiers = membershipInfo.getValue().get(0).getIdentifiers();

        if (membershipMonths.size() == 1) {
            LocalDate start = fromRawResults(membershipMonths.get(0));
            FilterOutByDate.DateRange range = asDateRange(start, start);
            return new CoverageSummary(identifiers, contract, Collections.singletonList(range));
        }

        List<FilterOutByDate.DateRange> dateRanges = new ArrayList<>();

        // Remove is dangerous but more efficient than subList or skip
        LocalDate begin = fromRawResults(membershipMonths.remove(0));
        LocalDate last = begin;
        for (CoverageMembership membership : membershipMonths) {
            LocalDate next = fromRawResults(membership);

            if (!next.isEqual(last.plusMonths(1))) {
                dateRanges.add(asDateRange(begin, last));
                begin = next;
                // Extend the date range by one month
            }

            last = next;
        }

        if (begin.equals(last)) {
            dateRanges.add(asDateRange(begin, begin));
        } else {
            dateRanges.add(asDateRange(begin, last));
        }

        return new CoverageSummary(identifiers, contract, dateRanges);

    }

    /**
     * Convert raw array results into object
     */
    private LocalDate fromRawResults(CoverageMembership result) {
        return LocalDate.of(result.getYear(), result.getMonth(), 1);
    }

    private FilterOutByDate.DateRange asDateRange(LocalDate localStartDate, LocalDate localEndDate)  {
        return FilterOutByDate.getDateRange(localStartDate.getMonthValue(), localStartDate.getYear(),
                localEndDate.getMonthValue(), localEndDate.getYear());
    }

    @Trace
    public void vacuumCoverage() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("VACUUM coverage")) {
            statement.execute();
        } catch (SQLException exception) {
            throw new RuntimeException("Could not vacuum coverage table", exception);
        }
    }
}
