package gov.cms.ab2d.common.repository;

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
import java.util.*;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@Repository
public class CoverageServiceRepository {

    private static final int BATCH_INSERT_SIZE = 10000;
    private static final int BATCH_SELECT_SIZE = 5000;

    private static final String INSERT_COVERAGE = "INSERT INTO coverage " +
            "(bene_coverage_period_id, bene_coverage_search_event_id, beneficiary_id, current_mbi, historic_mbis) " +
            "VALUES(?,?,?,?,?)";

    private static final String SELECT_COVERAGE_BY_SEARCH_COUNT = "SELECT COUNT(*) FROM coverage " +
            " WHERE bene_coverage_search_event_id = ?";

    private static final String SELECT_DISTINCT_COVERAGE_BY_PERIOD_COUNT = "SELECT COUNT(DISTINCT beneficiary_id) FROM coverage" +
            " WHERE bene_coverage_period_id IN(:ids)";

    private static final String SELECT_INTERSECTION = "SELECT COUNT(*) FROM (" +
            " SELECT DISTINCT beneficiary_id FROM coverage WHERE bene_coverage_search_event_id = ? " +
            " INTERSECT " +
            " SELECT DISTINCT beneficiary_id FROM coverage WHERE bene_coverage_search_event_id = ? " +
            ") I";

    private static final String SELECT_BENEFICIARIES_BATCH = "SELECT DISTINCT cov.beneficiary_id FROM coverage cov " +
            " WHERE cov.bene_coverage_period_id IN (:ids)" +
            " ORDER BY cov.beneficiary_id " +
            " LIMIT :limit";

    private static final String SELECT_BENEFICIARIES_BATCH_WITH_CURSOR = "SELECT DISTINCT cov.beneficiary_id FROM coverage cov " +
            " WHERE cov.bene_coverage_period_id IN (:ids) AND cov.beneficiary_id >= :cursor" +
            " ORDER BY cov.beneficiary_id " +
            " LIMIT :limit";

    private static final String SELECT_COVERAGE_INFORMATION =
            "SELECT cov.beneficiary_id, cov.current_mbi, cov.historic_mbis, period.year, period.month " +
            "FROM bene_coverage_period period INNER JOIN " +
            "       (SELECT cov.beneficiary_id, cov.current_mbi, cov.historic_mbis, cov.bene_coverage_period_id " +
            "        FROM coverage cov" +
            "        WHERE cov.bene_coverage_period_id IN (:coveragePeriods) " +
            "           AND cov.beneficiary_id IN (:beneficiaryIds) " +
            "       ) AS cov ON cov.bene_coverage_period_id = period.id ";


    private final DataSource dataSource;
    private final CoveragePeriodRepository coveragePeriodRepo;
    private final CoverageSearchEventRepository coverageSearchEventRepo;

    public CoverageServiceRepository(DataSource dataSource, CoveragePeriodRepository coveragePeriodRepo,
                                     CoverageSearchEventRepository coverageSearchEventRepo) {
        this.dataSource = dataSource;
        this.coverageSearchEventRepo = coverageSearchEventRepo;
        this.coveragePeriodRepo = coveragePeriodRepo;
    }

    public int countBySearchEvent(CoverageSearchEvent searchEvent) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_COVERAGE_BY_SEARCH_COUNT)) {
            statement.setLong(1, searchEvent.getId());
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException sqlException) {
            throw new RuntimeException("failed to count by search event", sqlException);
        }
    }

    public int countIntersection(CoverageSearchEvent searchEvent1, CoverageSearchEvent searchEvent2) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_INTERSECTION)) {
            statement.setLong(1, searchEvent1.getId());
            statement.setLong(2, searchEvent2.getId());

            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException sqlException) {
            throw new RuntimeException("failed to count intersection between searches", sqlException);
        }
    }

    public int countBeneficiariesByPeriods(List<Integer> coveragePeriodIds) {

        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("ids", coveragePeriodIds);

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
    public void insertBatches(CoverageSearchEvent searchEvent, Iterable<Identifiers> beneIds) {

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_COVERAGE)) {
            int processingCount = 0;

            // Prepare a batch of beneficiary ids to be inserted
            // and periodically conduct an insert if the batch size is large enough
            for (Identifiers beneficiary : beneIds) {
                processingCount++;

                prepareCoverageInsertion(statement, searchEvent, beneficiary);

                if (processingCount % BATCH_INSERT_SIZE == 0) {
                    statement.executeBatch();
                    processingCount = 0;
                }
            }

            if (processingCount > 0) {
                statement.executeBatch();
            }

        } catch (SQLException sqlException) {
            throw new RuntimeException("failed to insert coverage information", sqlException);
        }
    }

    private void prepareCoverageInsertion(PreparedStatement statement, CoverageSearchEvent searchEvent, Identifiers beneficiary) throws SQLException {
        statement.setInt(1, searchEvent.getCoveragePeriod().getId());
        statement.setLong(2, searchEvent.getId());
        statement.setString(3, beneficiary.getBeneficiaryId());
        statement.setString(4, beneficiary.getCurrentMbi());

        if (beneficiary.getHistoricMbis().isEmpty()) {
            statement.setString(5, null);
        } else {
            statement.setString(5, String.join(",", beneficiary.getHistoricMbis()));
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
                         " cov.bene_coverage_period_id = ? AND cov.bene_coverage_search_event_id = ?")) {
                statement.setLong(1, period.getId());
                statement.setLong(2, searchEvent.get().getId());
                statement.execute();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }

            vacuumCoverage();
        }
    }

    public CoveragePagingResult pageCoverage(Contract contract, CoveragePagingRequest page) {

        List<Integer> coveragePeriodIds = page.getCoveragePeriodIds();
        List<CoveragePeriod> coveragePeriods = coveragePeriodRepo.findAllById(coveragePeriodIds);

        if (coveragePeriods.size() != coveragePeriodIds.size()) {
            throw new IllegalArgumentException("at least one coverage period id not found in database");
        }

        /*
         * Look up a page of beneficiaries sorted by beneficiary id
         *
         * Grab one extra id to check whether another page of ids exists or not
         */
        List<String> beneficiaries = findActiveBeneficiaryIds(page);

        // If another page exists, the last beneficiary will be the first beneficiary
        // of the next page so ignore it
        Map<String, List<CoverageMembership>> coverageMembership;
        if (beneficiaries.size() > page.getPageSize()) {
            coverageMembership = findCoverageMemberships(beneficiaries.subList(0, beneficiaries.size() - 1), coveragePeriodIds);
        } else {
            coverageMembership = findCoverageMemberships(beneficiaries, coveragePeriodIds);
        }

        List<CoverageSummary> beneficiarySummaries = coverageMembership.entrySet().stream()
                .map(membershipEntry -> summarizeCoverageMembership(contract, membershipEntry))
                .collect(toList());

        CoveragePagingRequest request = null;
        if (beneficiaries.size() > page.getPageSize()) {
            request = new CoveragePagingRequest(page.getPageSize(),
                    beneficiaries.get(beneficiaries.size() - 1), coveragePeriodIds);
        }

        return new CoveragePagingResult(beneficiarySummaries, request);
    }

    /**
     * Find page of beneficiaries then look up all of their coverage information for a range of identified coverage
     * periods.
     */
    private Map<String, List<CoverageMembership>> findCoverageMemberships(List<String> beneficiaries, List<Integer> coveragePeriodIds) {

        // Look up coverage information for BATCH_SELECT_SIZE beneficiaries at a time
        // and populate into hash map
        Map<String, List<CoverageMembership>> coverageMemberships = new HashMap<>(beneficiaries.size());

        for (int subListStart = 0; subListStart < beneficiaries.size(); subListStart += BATCH_SELECT_SIZE) {

            int subListEnd = Math.min(subListStart + BATCH_SELECT_SIZE, beneficiaries.size());

            // Find all coverage membership information for a batch of beneficiary ids
            // over the range [subListStart, subListEnd)

            List<String> subList = beneficiaries.subList(subListStart, subListEnd);
            List<CoverageMembership> membershipInfo = findCoverageInformation(coveragePeriodIds, subList);

            // Group the raw coverage information by beneficiary id
            Map<String, List<CoverageMembership>> coverages = membershipInfo.stream()
                    .collect(groupingBy(membership -> membership.getIdentifiers().getBeneficiaryId()));

            coverageMemberships.putAll(coverages);
        }

        return coverageMemberships;
    }

    /**
     * Find a subset of active beneficiary ids by
     * @return page of beneficiary ids
     */
    public List<String> findActiveBeneficiaryIds(CoveragePagingRequest page) {

        List<Integer> coveragePeriodIds = page.getCoveragePeriodIds();
        int pageSize = page.getPageSize();
        Optional<String> cursor = page.getCursor();

        if (cursor.isEmpty()) {
            return findActiveBeneficiaryIdsWithoutCursor(coveragePeriodIds, pageSize);
        }

        return findActiveBeneficiaryIdsWithCursor(coveragePeriodIds, cursor.get(), pageSize);
    }

    private List<String> findActiveBeneficiaryIdsWithoutCursor(List<Integer> coveragePeriodIds, int pageSize) {
        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("ids", coveragePeriodIds)
                .addValue("limit", pageSize + 1);

        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);

        return template.query(SELECT_BENEFICIARIES_BATCH, parameters,
                (results, rowNum) -> results.getString(1));
    }

    private List<String> findActiveBeneficiaryIdsWithCursor(List<Integer> coveragePeriodIds, String cursor, int pageSize) {
        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("ids", coveragePeriodIds)
                .addValue("limit", pageSize + 1)
                .addValue("cursor", cursor);

        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);

        return template.query(SELECT_BENEFICIARIES_BATCH_WITH_CURSOR, parameters,
                (results, rowNum) -> results.getString(1));
    }

    public List<CoverageMembership> findCoverageInformation(List<Integer> coveragePeriodIds, List<String> beneficiaryIds) {

        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("coveragePeriods", coveragePeriodIds)
                .addValue("beneficiaryIds", beneficiaryIds);

        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);

        return template.query(SELECT_COVERAGE_INFORMATION, parameters, CoverageServiceRepository::asMembership);
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

        return new Identifiers(rs.getString(1), rs.getString(2), historicMbis);
    }

    /**
     * Summarize the coverage of one beneficiary for
     */
    private CoverageSummary summarizeCoverageMembership(Contract contract,
                                                        Map.Entry<String, List<CoverageMembership>> membershipInfo) {

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

    public void vacuumCoverage() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("VACUUM coverage")) {
            statement.execute();
        } catch (SQLException exception) {
            throw new RuntimeException("Could not vacuum coverage table", exception);
        }
    }
}
