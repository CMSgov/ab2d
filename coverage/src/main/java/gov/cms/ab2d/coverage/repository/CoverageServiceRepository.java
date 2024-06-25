package gov.cms.ab2d.coverage.repository;

import com.newrelic.api.agent.Trace;
import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.coverage.model.ContractForCoverageDTO;
import gov.cms.ab2d.coverage.model.CoverageCount;
import gov.cms.ab2d.coverage.model.CoverageJobStatus;
import gov.cms.ab2d.coverage.model.CoverageMembership;
import gov.cms.ab2d.coverage.model.CoveragePagingRequest;
import gov.cms.ab2d.coverage.model.CoveragePagingResult;
import gov.cms.ab2d.coverage.model.CoveragePeriod;
import gov.cms.ab2d.coverage.model.CoverageSearchEvent;
import gov.cms.ab2d.coverage.model.CoverageSummary;
import gov.cms.ab2d.coverage.model.Identifiers;
import gov.cms.ab2d.filter.FilterOutByDate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;


import static gov.cms.ab2d.common.util.DateUtil.AB2D_EPOCH;
import static gov.cms.ab2d.common.util.DateUtil.AB2D_ZONE;
import static java.util.stream.Collectors.toList;

/**
 * Vanilla SQL interface with the coverage table. The class is designed to be performant and work with the
 * partitioning strategies in the database. Partitioning is mainly done to speed up bulk inserts.
 *
 * IMPORTANT: The coverage table is partitioned by contract and then by year.
 *
 * With Postgres, this means that all queries to the coverage table must contain contract or year, otherwise those queries
 * will trigger a full table scan.
 *
 * Indexes on the "coverage" table to speed up queries
 *
 *      - beneficiary_id, contract, year, month, bene_coverage_search_event_id
 *      - beneficiary_coverage_period_id, beneficiary_id, contract, year
 *      - beneficiary_coverage_search_event_id, beneficiary_id, contract, year
 */
@Slf4j
@Repository
public class CoverageServiceRepository {
    private static final String CONTRACT_STRING = "contract";
    private static final String YEARS_STRING = "years";

    private static final int BATCH_INSERT_SIZE = 10000;

    // List of years from 2020 to current year
    private static final List<Integer> YEARS = IntStream.rangeClosed(2020, Calendar.getInstance().get(Calendar.YEAR)).boxed().toList();

    /**
     * Assign a beneficiary as being a member of a contract during a year and month {@link CoveragePeriod}
     * and record what update from BFD this record is associated with {@link CoverageSearchEvent}.
     *
     * Insertion will break if a patient is tied to the same contract, year, month, and search event more than once.
     * Likewise for coverage period.
     *
     * The contract and year must be included to take advantage of partitioning. The month is used
     * to improve indexing.
     */
    private static final String INSERT_COVERAGE = "INSERT INTO coverage " +
            "(bene_coverage_period_id, bene_coverage_search_event_id, contract, year, month, beneficiary_id, current_mbi, historic_mbis) " +
            "VALUES(?,?,?,?,?,?,?,?)";

    /**
     * Return a count of all beneficiaries associated with an {@link CoveragePeriod} by a specific update from BFD
     * {@link CoverageSearchEvent}.
     *
     * The count is not distinct because constraints will break if a patient is assigned to the same
     * {@link CoverageSearchEvent}
     *
     * The contract and year must be included to take advantage of the partitions and prevent a table scan
     */
    private static final String SELECT_COVERAGE_BY_SEARCH_COUNT = "SELECT COUNT(*) FROM coverage c " +
            " join current_mbi m on  c.current_mbi=m.mbi" +
            " WHERE bene_coverage_search_event_id = :id AND contract = :contract AND year IN (:years) AND opt_out_flag is not false AND current_mbi is not null";

    /**
     * Return a count of all beneficiaries associated with an {@link CoveragePeriod}
     * from any event.
     */
    private static final String SELECT_DISTINCT_COVERAGE_BY_PERIOD_COUNT = "SELECT COUNT(DISTINCT beneficiary_id) FROM coverage" +
            " WHERE bene_coverage_period_id IN(:ids) AND contract = :contract AND year IN (:years) and current_mbi is not null";

    /**
     * Return a count of all beneficiaries who aggred to share their data associated with an {@link CoveragePeriod}
     * from any event. For those beneficiaries out_out_flag equals false in the public.coverage table.
     */

    private static final String SELECT_DISTINCT_OPTOUT_COVERAGE_BY_PERIOD_COUNT = "SELECT COUNT(DISTINCT beneficiary_id) FROM coverage c " +
            " join current_mbi m on  c.current_mbi=m.mbi" +
            " WHERE bene_coverage_period_id IN(:ids) AND contract = :contract AND year IN (:years) AND opt_out_flag is not false and current_mbi is not null";

    /**
     * Delete all coverage associated with a single update from BFD {@link CoverageSearchEvent}
     *
     * The contract and year must be included to take advantage of the partitions and prevent a table scan.
     */
    private static final String DELETE_SEARCH = "DELETE FROM coverage cov " +
            "WHERE cov.bene_coverage_search_event_id = :searchEvent" +
            "   AND contract = :contract AND year IN (:years)";

    /**
     * Delete all coverage associated with a list of updates from BFD {@link CoverageSearchEvent}
     *
     * The contract and year must be included to take advantage of the partitions and prevent a table scan.
     */
    private static final String DELETE_PREVIOUS_SEARCHES = "DELETE FROM coverage cov " +
            "WHERE cov.bene_coverage_search_event_id IN (:searchEvents)" +
            "   AND contract = :contract AND year IN (:years)";

    /**
     * List out the patients present in one {@link CoverageSearchEvent} but not present in another {@link CoverageSearchEvent}
     *
     * Every time a {@link CoveragePeriod} is updated after the first time coverage is pulled from BFD, this query is
     * run to determine the drift in enrollment over time. The results of this query are inserted into a records
     * table as part of the {@link CoverageDeltaRepository}
     *
     * The contract and year must be included to take advantage of the partitions and prevent a table scan.
     */
    static final String SELECT_DELTA =
            "SELECT cov1.bene_coverage_period_id, cov1.beneficiary_id, :type as entryType, CURRENT_TIMESTAMP as created" +
            " FROM coverage cov1" +
            " WHERE cov1.bene_coverage_search_event_id = :search1 AND NOT EXISTS" +
                " (SELECT cov2.beneficiary_id FROM coverage cov2" +
                " WHERE cov1.beneficiary_id = cov2.beneficiary_id and bene_coverage_search_event_id = :search2 )";

    /**
     * Count the number of beneficiaries shared between two updates from {@link CoverageSearchEvent} for the same
     * {@link CoveragePeriod}.
     *
     * This number summarizes the detailed information provided by the {@link #SELECT_DELTA} query.
     *
     * The contract and year must be included to take advantage of the partitions and prevent a table scan.
     */
    private static final String SELECT_INTERSECTION = "SELECT COUNT(*) FROM (" +
            " SELECT DISTINCT beneficiary_id FROM coverage WHERE bene_coverage_search_event_id = :search1 AND contract = :contract AND year IN (:years) and current_mbi is not null" +
            " INTERSECT " +
            " SELECT DISTINCT beneficiary_id FROM coverage WHERE bene_coverage_search_event_id = :search2 AND contract = :contract AND year IN (:years) and current_mbi is not null" +
            ") I";

    /**
     * Select a limited number of records from the coverage table associated with a specific contract. This is the
     * first call to get records, all subsequent calls require a cursor.
     *
     * Without a limit this query will typically return millions of results maybe even tens of millions.
     *
     * The contract and year must be included to take advantage of the partitions and prevent a table scan.
     */
    private static final String SELECT_COVERAGE_WITHOUT_CURSOR =
            "SELECT beneficiary_id, current_mbi, historic_mbis, year, month " +
            " FROM coverage " +
            " WHERE contract = :contract and year IN (:years) and current_mbi is not null" +
            " ORDER BY beneficiary_id " +
            " LIMIT :limit";

    private static final String SELECT_OPTOUT_COVERAGE_WITHOUT_CURSOR =
            "SELECT beneficiary_id, current_mbi, historic_mbis, year, month " +
                    " FROM coverage c join current_mbi m on  c.current_mbi=m.mbi " +
                    " WHERE contract = :contract and year IN (:years) and opt_out_flag is not false and current_mbi is not null" +
                    " ORDER BY beneficiary_id " +
                    " LIMIT :limit";

    /**
     * Select a limited number of records starting from a beneficiary (cursor)
     * from the coverage table associated with a specific contract.
     *
     * This is used to page through the enrollment related to a contract and starts where the last page ended.
     *
     * The contract and year must be included to take advantage of the partitions and prevent a table scan.
     */
    private static final String SELECT_COVERAGE_WITH_CURSOR =
            "SELECT beneficiary_id, current_mbi, historic_mbis, year, month " +
            " FROM coverage " +
            " WHERE contract = :contract and year IN (:years) and current_mbi is not null AND beneficiary_id >= :cursor " +
            " ORDER BY beneficiary_id " +
            " LIMIT :limit";

    private static final String SELECT_OPTOUT_COVERAGE_WITH_CURSOR =
            "SELECT beneficiary_id, current_mbi, historic_mbis, year, month " +
                    " FROM coverage c join current_mbi m on  c.current_mbi=m.mbi" +
                    " WHERE contract = :contract and year IN (:years) and  current_mbi is not null and opt_out_flag is not false AND beneficiary_id >= :cursor " +
                    " ORDER BY beneficiary_id " +
                    " LIMIT :limit";

    /**
     * Given a list of contracts, for each contract and all {@link CoveragePeriod}s that contract has been active for,
     * count the number of beneficiaries covered by the contract and report those results.
     *
     * Results are split by {@link CoverageSearchEvent} to detect when duplicate enrollment is present.
     *
     * The results contain the ContractNumber, year, month, {@link CoveragePeriod} id,
     * and {@link CoverageSearchEvent} id
     *
     * The contract and year must be included to take advantage of the partitions and prevent a table scan.
     */
    private static final String SELECT_COUNT_CONTRACT =
            " SELECT coverage.contract, coverage.year, coverage.month, coverage.bene_coverage_period_id," +
                    " coverage.bene_coverage_search_event_id, COUNT(*) as bene_count " +
            " FROM coverage INNER JOIN bene_coverage_period bcp ON coverage.bene_coverage_period_id = bcp.id " +
            " WHERE bcp.status = 'SUCCESSFUL' AND coverage.contract IN (:contracts)  and coverage.current_mbi is not null AND coverage.year IN (:years) " +
            " GROUP BY coverage.contract, coverage.year, coverage.month, " +
                    " coverage.bene_coverage_period_id, coverage.bene_coverage_search_event_id " +
            " ORDER BY coverage.contract, coverage.year, coverage.month, " +
                    " coverage.bene_coverage_period_id, coverage.bene_coverage_search_event_id ";

    private final DataSource dataSource;
    private final CoveragePeriodRepository coveragePeriodRepo;
    private final CoverageSearchEventRepository coverageSearchEventRepo;
    private final PropertiesService propertiesService;

    public CoverageServiceRepository(DataSource dataSource, CoveragePeriodRepository coveragePeriodRepo,
                                     CoverageSearchEventRepository coverageSearchEventRepo, PropertiesService propertiesService) {
        this.dataSource = dataSource;
        this.coverageSearchEventRepo = coverageSearchEventRepo;
        this.coveragePeriodRepo = coveragePeriodRepo;
        this.propertiesService = propertiesService;
    }

    /**
     * Count the number of beneficiaries in the coverage table which are associated with a specific
     * search of BFD {@link CoverageSearchEvent}.
     *
     * Coverage is only associated with {@link CoverageJobStatus#IN_PROGRESS} search events. So submitting any other search event
     * will result in no coverage being reported.
     *
     * @param searchEvent a specific search done at some point which we need numbers for
     * @return the number of beneficiaries related to the event, may be zero
     * @throws RuntimeException if no coverage is found
     */
    @Trace
    public int countBySearchEvent(CoverageSearchEvent searchEvent) {

        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("id", searchEvent.getId())
                .addValue(CONTRACT_STRING, searchEvent.getCoveragePeriod().getContractNumber())
                .addValue(YEARS_STRING, YEARS);

        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);

        return template.queryForList(SELECT_COVERAGE_BY_SEARCH_COUNT, parameters, Integer.class)
                .stream().findFirst().orElseThrow(() -> new RuntimeException("no coverage information found for " +
                        "the coverage search event"));
    }

    /**
     * Compare the beneficiaries in two searches and count the number of beneficiaries shared between the searches. This
     * query should only be used when comparing the number of beneficaries for the two most recent {@link CoverageSearchEvent}s
     * associated with a {@link CoveragePeriod}.
     *
     * This is used to calculate the difference between an old set of enrollment for a given contract, year, and month,
     * and the most recent update.
     *
     * This query only makes sense when both {@link CoverageSearchEvent}s provided are
     * associated with the same {@link CoveragePeriod}. Any other usage will cause misleading results.
     *
     * This query should only be
     *
     * Coverage is only associated with {@link CoverageJobStatus#IN_PROGRESS} search events. So submitting any other search event
     * will result in no data for the comparison.
     *
     * @param searchEvent1 the first search event
     * @param searchEvent2 the second search event
     * @return the number of beneficiaries common between the two searches
     * @throws RuntimeException on failure to find any results from the query for any reason
     */
    @Trace
    public int countIntersection(CoverageSearchEvent searchEvent1, CoverageSearchEvent searchEvent2) {

        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("search1", searchEvent1.getId())
                .addValue("search2", searchEvent2.getId())
                .addValue(CONTRACT_STRING, searchEvent1.getCoveragePeriod().getContractNumber())
                .addValue(YEARS_STRING, YEARS);

        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);

        return template.queryForList(SELECT_INTERSECTION, parameters, Integer.class)
                .stream().findFirst().orElseThrow(() -> new RuntimeException("no coverage information found for any" +
                        "of the search events provided"));
    }

    /**
     * Calculate the unique number of beneficiaries associated with a period of enrollment.
     *
     * @param coveragePeriodIds list of coverage periods {@link CoveragePeriod}s associated with an
     * @param contractNum a five character String representing an
     * @return total number of unique beneficiaries
     */
    @Trace
    public int countBeneficiariesByPeriods(List<Integer> coveragePeriodIds, String contractNum) {

        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("ids", coveragePeriodIds)
                .addValue(CONTRACT_STRING, contractNum)
                .addValue(YEARS_STRING, YEARS);

        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);
        //If OptOut is enabled, count beneficiaries who agreed to share their data
        String query = (propertiesService.isToggleOn("OptOutOn", false)) ? SELECT_DISTINCT_OPTOUT_COVERAGE_BY_PERIOD_COUNT : SELECT_DISTINCT_COVERAGE_BY_PERIOD_COUNT;

        return template.queryForList(query, parameters, Integer.class)
                .stream().findFirst().orElseThrow(() -> new RuntimeException("no coverage information found for any " +
                                "of the coverage periods provided"));
    }

    /**
     * Calculate exact numbers of beneficiaries enrolled for each month of a contract for each provided contract and return
     * a list of results {@link CoverageCount}.
     *
     * This method provides the statistics necessary to verify that the coverage data in the database meets business
     * requirements.
     *
     * @param contracts list of {@link ContractForCoverageDTO}s
     * @return counts of the coverage for a given coverage period
     */
    @Trace
    public List<CoverageCount> countByContractCoverage(List<ContractForCoverageDTO> contracts) {
        List<String> contractNumbers = contracts.stream().map(ContractForCoverageDTO::getContractNumber).collect(toList());
        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("contracts", contractNumbers)
                .addValue(YEARS_STRING, YEARS);

        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);

        return template.query(SELECT_COUNT_CONTRACT, parameters, CoverageServiceRepository::asCoverageCount);
    }

    /**
     * Mark benficiaries as enrolled in a contract for a specific search event, contract, month, and year using plain JDBC.
     *
     * The enrollment is related to a search against BFD {@link CoverageSearchEvent} that we've done for a contract,
     * month, and year. We may have other older enrollment from previous searches against BFD
     * also in the database when this insertion is done.
     *
     * @param searchEvent the search event to add coverage in relation to
     * @param beneIds Collection of beneficiary ids to be added as a batch
     * @throws RuntimeException if insertion fails due to a syntax or timeout issue with Postgres.
     */
    @Trace
    public void insertBatches(CoverageSearchEvent searchEvent, Iterable<Identifiers> beneIds) {

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_COVERAGE)) {

            int processingCount = 0;

            String contractNum = searchEvent.getCoveragePeriod().getContractNumber();
            int year = searchEvent.getCoveragePeriod().getYear();
            int month = searchEvent.getCoveragePeriod().getMonth();

            // Prepare a batch of beneficiary ids to be inserted
            // and periodically conduct an insert if the batch size is large enough
            for (Identifiers beneficiary : beneIds) {
                processingCount++;

                prepareCoverageInsertion(statement, contractNum, year, month, searchEvent, beneficiary);

                // Insert a batch of beneficiaries to avoid large, slow, insertions
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

    /**
     * This method exists so that NewRelic can identify this component of the transaction and time it explicitly.
     *
     * @param statement a batch statement with tens of thousands
     * @throws SQLException on failure to make the insertion
     */
    @Trace
    private void executeBatch(PreparedStatement statement) throws SQLException {
        statement.executeBatch();
    }

    /**
     * Add a single beneficiary to the insertion statement.
     *
     * @throws SQLException on failure to add single beneficiary to batch
     */
    private void prepareCoverageInsertion(PreparedStatement statement, String contractNum, int year, int month,
                                          CoverageSearchEvent searchEvent, Identifiers beneficiary) throws SQLException {
        // Fields uniquely identifying a search
        statement.setInt(1, searchEvent.getCoveragePeriod().getId());
        statement.setLong(2, searchEvent.getId());

        // Fields necessary to support partitioning
        statement.setString(3, contractNum);
        statement.setInt(4, year);

        statement.setInt(5, month);

        // Fields identifying a beneficiary
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
     * Delete all coverage information related to the results of a single coverage period
     * defined by an offset into the past.
     *
     * An offset of 0 finds the current IN_PROGRESS search event and deletes all coverage information associated
     * with that event.
     *
     * @param period coverage period to remove
     */
    public void deleteCurrentSearch(CoveragePeriod period) {

        Optional<CoverageSearchEvent> searchEvent = coverageSearchEventRepo.findByPeriodDesc(period.getId(), 100)
                .stream().filter(event -> event.getNewStatus() == CoverageJobStatus.IN_PROGRESS).findFirst();

        // Only delete previous search if a previous search exists.
        // For performance reasons this is done via jdbc
        if (searchEvent.isPresent()) {
            MapSqlParameterSource parameterSource = new MapSqlParameterSource()
                    .addValue("searchEvent", searchEvent.get().getId())
                    .addValue(CONTRACT_STRING, period.getContractNumber())
                    .addValue(YEARS_STRING, YEARS);

            NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);
            template.update(DELETE_SEARCH, parameterSource);

            vacuumCoverage();
        }
    }

    /**
     * Delete all coverage information related to the results of any search in the past beyond an offset.
     *
     * Ex. if you want to delete all past enrollment besides the most recent search
     *
     * @param period coverage period to remove
     * @param offset offset into the past 0 is last search done successfully, 1 is search before that, etc.
     */
    public void deletePreviousSearches(CoveragePeriod period, int offset) {

        // Delete with prejudice
        List<CoverageSearchEvent> events = coverageSearchEventRepo.findByPeriodDesc(period.getId(), 100);

        // Get all in progress events after offset and delete any enrollment associated with them
        List<Long> inProgressEvents = events.stream().filter(event -> event.getNewStatus() == CoverageJobStatus.IN_PROGRESS)
                .skip(offset).map(CoverageSearchEvent::getId).collect(toList());

        // Only delete previous search if a previous search exists.
        // For performance reasons this is done via jdbc
        if (!inProgressEvents.isEmpty()) {

            MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                    .addValue("searchEvents", inProgressEvents)
                    .addValue(CONTRACT_STRING, period.getContractNumber())
                    .addValue(YEARS_STRING, YEARS);

            NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);
            template.update(DELETE_PREVIOUS_SEARCHES, sqlParameterSource);

            vacuumCoverage();
        }
    }

    /**
     * Page through coverage records in database by beneficiary and aggregate those records into a single object
     * for beneficiary.
     *
     * The paging request will contain a contract, a page size (number of beneficiaries to pull), and a cursor with the
     * last patient pulled.
     *
     * The coverage table contains more than one coverage record per beneficiary. Each coverage record corresponds
     * to a beneficiary belonging to a contract for a specific month and year. These records must be aggregated
     * for each patient that has ever been a member of the contract to calculate a list of date ranges for their
     * membership.
     *
     * The paging is done by beneficiary, not by enrollment record. A beneficiary may have dozens of enrollment records
     * in the database.
     *
     * For example if
     *
     *      1. The page size is 10, and
     *      2. A contract has been active for two years
     * Then each beneficiary could have a record for every month they were active in the contract. Assuming each beneficiary
     * was active for every month of the contract that would be (10 beneficiaries) * (24 months per beneficiary) = 240 records
     *
     * Internally the pageCoverage method will pull more beneficiaries' records than it needs for a complete page and then
     * truncate the results down to the expected page size.
     *
     * The page coverage method will also include the cursor with the result for the next page of beneficiaries.
     *
     * Step by step what is involved in this method:
     *
     * 1. Check that contract has enrollment for all necessary months before retrieving a {@link CoveragePagingRequest#getPageSize()}.
     *    If a contract does not have enrollment for every month except the current month, then it violates a business requirement
     *    {@link #getExpectedCoveragePeriods(CoveragePagingRequest)}
     * 2. Calculate number of coverage records which correspond to page size patients to pull from database. There should be
     *    at most one coverage record per beneficiary per month the contract has been active
     *    (pageSize * months * beneficiaries) {@link #getCoverageLimit(int, long)}
     * 3. Conduct query to receive coverage records for a {@link #getCoverageLimit(int, long)} of enrollment information
     *    without processing the results. Each record in the results contains all known identifiers associated
     *    with a patient and specifies a  month and year that the beneficiaries
     *    are a member of the contract {@link #queryCoverageMembership(CoveragePagingRequest, long)}
     * 4. Group the previous queries' results by patient {@link #aggregateEnrollmentByPatient(int, List)}
     * 5. For each patient condense enrollment down to a single set of date ranges {@link #summarizeCoverageMembership(ContractForCoverageDTO, Map.Entry)}
     * 6. Determine whether another page of results is necessary
     * 7. If another page of results is necessary create a {@link CoveragePagingRequest}
     * 8. Collect the {@link CoverageSummary} and next {@link CoveragePagingRequest }into a single {@link CoveragePagingResult}
     *
     * @param page request for paging coverage
     * @return the result of paging with a cursor to the next request
     */
    public CoveragePagingResult pageCoverage(CoveragePagingRequest page) {

        ContractForCoverageDTO contract = page.getContract();
        int expectedCoveragePeriods = getExpectedCoveragePeriods(page);

        // Make sure all coverage periods are present so that there isn't any missing coverage data
        // Do not remove this check because it is a fail safe to guarantee that there isn't something majorly
        // wrong with the enrollment data.
        // A missing period = one month of enrollment missing for the contract
        List<CoveragePeriod> coveragePeriods = coveragePeriodRepo.findAllByContractNumber(contract.getContractNumber());
        if (coveragePeriods.size() != expectedCoveragePeriods) {
            throw new IllegalArgumentException("at least one coverage period missing from enrollment table for contract "
                    + page.getContract().getContractNumber());
        }

        // Determine how many records to pull back
        long limit = getCoverageLimit(page.getPageSize(), expectedCoveragePeriods);

        // Query coverage membership from database and collect it
        List<CoverageMembership> enrollment = queryCoverageMembership(page, limit);

        // Guarantee ordering of results to the order that the beneficiaries were returned from SQL
        Map<Long, List<CoverageMembership>> enrollmentByBeneficiary =
                aggregateEnrollmentByPatient(expectedCoveragePeriods, enrollment);

        // Only summarize page size beneficiaries worth of information and report it
        List<CoverageSummary> beneficiarySummaries = enrollmentByBeneficiary.entrySet().stream()
                .limit(page.getPageSize())
                .map(membershipEntry -> summarizeCoverageMembership(contract, membershipEntry))
                .collect(toList());

        // Get the patient to start from next time
        Optional<Map.Entry<Long, List<CoverageMembership>>> nextCursor =
                enrollmentByBeneficiary.entrySet().stream().skip(page.getPageSize()).findAny();

        // Build the next request if there is a next patient
        CoveragePagingRequest request = null;
        if (nextCursor.isPresent()) {
            Map.Entry<Long, List<CoverageMembership>> nextCursorBeneficiary = nextCursor.get();
            request = new CoveragePagingRequest(page.getPageSize(), nextCursorBeneficiary.getKey(), contract, page.getJobStartTime());
        }

        return new CoveragePagingResult(beneficiarySummaries, request);
    }

    /**
     * Query the database for enrollment for beneficiaries but do not aggregate the data
     *
     * @param page request with cursor and contract
     * @param limit number of records to pull back
     * @return records pulled back
     */
    private List<CoverageMembership> queryCoverageMembership(CoveragePagingRequest page, long limit) {

        Optional<Long> pageCursor = page.getCursor();

        MapSqlParameterSource sqlParameterSource = new MapSqlParameterSource()
                .addValue(CONTRACT_STRING, page.getContractNumber())
                .addValue(YEARS_STRING, YEARS)
                .addValue("limit", limit);

        pageCursor.ifPresent(cursor -> sqlParameterSource.addValue("cursor", cursor));

        boolean isOptOutOn = propertiesService.isToggleOn("OptOutOn", false);
        // Grab the enrollment
        List<CoverageMembership> enrollment;
        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);
        if (pageCursor.isPresent()) {
            String queryWithCursor = (isOptOutOn) ? SELECT_OPTOUT_COVERAGE_WITH_CURSOR : SELECT_COVERAGE_WITH_CURSOR;
            enrollment = template.query(queryWithCursor, sqlParameterSource,
                    CoverageServiceRepository::asMembership);
        } else {
            String queryWithoutCursor = (isOptOutOn) ? SELECT_OPTOUT_COVERAGE_WITHOUT_CURSOR : SELECT_COVERAGE_WITHOUT_CURSOR;
            enrollment = template.query(queryWithoutCursor, sqlParameterSource,
                    CoverageServiceRepository::asMembership);
        }
        return enrollment;
    }

    /**
     * Get the number of enrollment entries expected per patient assuming each patient is
     * a member of the contract since it was attested for.
     *
     * Basically expect one record per patient per month, tack on an additional patient to buffer the results.
     *
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

    private Map<Long, List<CoverageMembership>> aggregateEnrollmentByPatient(int expectedCoveragePeriods, List<CoverageMembership> enrollment) {
        Map<Long, List<CoverageMembership>> enrollmentByBeneficiary = new LinkedHashMap<>();

        // Guarantee insertion order. Could use functional API in future.
        for (CoverageMembership coverageMembership : enrollment) {
            // If not present add to mapping
            long beneficiaryId = coverageMembership.getIdentifiers().getBeneficiaryId();
            enrollmentByBeneficiary.putIfAbsent(beneficiaryId,
                    new ArrayList<>(expectedCoveragePeriods));
            enrollmentByBeneficiary.get(beneficiaryId).add(coverageMembership);
        }

        return enrollmentByBeneficiary;
    }

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

    private static CoverageCount asCoverageCount(ResultSet rs, int rowNum) throws SQLException {

        String contractNum = rs.getString(1);
        int year = rs.getInt(2);
        int month = rs.getInt(3);

        int periodId = rs.getInt(4);
        int eventId = rs.getInt(5);
        int beneCount = rs.getInt(6);

        return new CoverageCount(contractNum, year, month, periodId, eventId, beneCount);
    }

    /**
     * Summarize the coverage of one beneficiary for
     */
    private CoverageSummary summarizeCoverageMembership(ContractForCoverageDTO contract,
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

    /**
     * Clean up indexes between major changes in coverage to keep queries performant.
     *
     * Calling this method introduces significant overhead so make sure it is only called after significant events.
     */
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
