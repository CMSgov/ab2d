package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.CoverageMembership;
import gov.cms.ab2d.common.model.CoveragePeriod;
import gov.cms.ab2d.common.model.CoverageSearchDiff;
import gov.cms.ab2d.common.model.CoverageSearchEvent;
import gov.cms.ab2d.common.model.CoverageSummary;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.repository.CoveragePeriodRepository;
import gov.cms.ab2d.common.repository.CoverageRepository;
import gov.cms.ab2d.common.repository.CoverageSearchEventRepository;
import gov.cms.ab2d.common.util.FilterOutByDate;
import gov.cms.ab2d.common.util.FilterOutByDate.DateRange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.*;
import java.util.*;

import static gov.cms.ab2d.common.util.Constants.AB2D_EPOCH;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

/**
 * Interact with Coverage, CoveragePeriod, and CoverageSearchEvent entities/tables including
 * bulk insertions, reads, and deletes.
 *
 * Warning: {@link org.springframework.data.jpa.repository.JpaRepository#getOne(Object)}
 * loads lazily which causes issues when an object is returned by the {@link CoverageServiceImpl}
 * to another context which may not have a {@link Transactional} context.
 *
 * To prevent issues getOne is not used in this service at all.
 */
@Slf4j
@Service
@Transactional
@SuppressWarnings("PMD.UnusedImports")
public class CoverageServiceImpl implements CoverageService {

    private static final int BATCH_INSERT_SIZE = 10000;
    private static final int BATCH_SELECT_SIZE = 5000;

    private static final String INSERT_COVERAGE = "INSERT INTO coverage " +
            "(bene_coverage_period_id, bene_coverage_search_event_id, beneficiary_id) VALUES(?,?,?)";

    private static final String SELECT_BENEFICIARIES_BATCH = "SELECT DISTINCT cov.beneficiary_id FROM coverage cov " +
            " WHERE cov.bene_coverage_period_id IN (:ids) " +
            " ORDER BY cov.beneficiary_id " +
            " OFFSET :offset " +
            " LIMIT :limit";

    private static final String SELECT_COVERAGE_INFORMATION = "SELECT cov.beneficiary_id, period.year, period.month " +
            " FROM bene_coverage_period period INNER JOIN " +
            "       (SELECT cov.beneficiary_id, cov.bene_coverage_period_id " +
            "        FROM coverage cov" +
            "        WHERE cov.bene_coverage_period_id IN (:coveragePeriods) " +
            "           AND cov.beneficiary_id IN (:beneficiaryIds) " +
             "       ) AS cov ON cov.bene_coverage_period_id = period.id ";

    @Autowired
    private CoverageRepository coverageRepo;

    @Autowired
    private CoveragePeriodRepository coveragePeriodRepo;

    @Autowired
    private CoverageSearchEventRepository coverageSearchEventRepo;

    @Autowired
    private ContractRepository contractRepo;

    @Autowired
    private DataSource dataSource;

    @Override
    public CoveragePeriod getCoveragePeriod(long contractId, int month, int year) {
        checkMonthAndYear(month, year);
        return coveragePeriodRepo.getByContractIdAndMonthAndYear(contractId, month, year);
    }

    @Override
    public boolean isCoveragePeriodInProgress(int periodId) {
        CoveragePeriod period = findCoveragePeriod(periodId);
        JobStatus jobStatus = period.getStatus();
        return jobStatus == JobStatus.IN_PROGRESS;
    }

    @Override
    public boolean canEOBSearchBeStarted(int periodId) {
        return !isCoveragePeriodInProgress(periodId);
    }

    @Override
    public JobStatus getSearchStatus(int periodId) {
        CoveragePeriod coverageSearch = findCoveragePeriod(periodId);
        return coverageSearch.getStatus();
    }

    @Override
    public Optional<CoverageSearchEvent> findLastEvent(int periodId) {
        CoveragePeriod period = findCoveragePeriod(periodId);
        return findLastEvent(period);
    }

    private Optional<CoverageSearchEvent> findLastEvent(CoveragePeriod period) {
        return coverageSearchEventRepo.findFirstByCoveragePeriodOrderByCreatedDesc(period);
    }

    private CoverageSearchEvent getLastEvent(CoveragePeriod period) {
        return findLastEvent(period).orElse(null);
    }

    @Override
    public CoverageSearchEvent insertCoverage(int periodId, long searchEventId, Set<String> beneficiaryIds) {

        // Make sure that coverage period and searchEvent actually exist in the database before inserting
        List<String> beneficiariesList = new ArrayList<>(beneficiaryIds);
        findCoveragePeriod(periodId);
        CoverageSearchEvent searchEvent = findCoverageSearchEvent(searchEventId);

        for (int subListStart = 0; subListStart < beneficiaryIds.size(); subListStart += BATCH_INSERT_SIZE) {

            int subListEnd = Math.min(subListStart + BATCH_INSERT_SIZE, beneficiaryIds.size());

            List<String> batch = beneficiariesList.subList(subListStart, subListEnd);

            insertBatch(periodId, searchEventId, batch);
        }

        // Update indices after every batch of insertions to make sure speed of database is preserved
        vacuumCoverage();

        return searchEvent;
    }

    /**
     * Insert a batch of ids as {@link gov.cms.ab2d.common.model.Coverage} objects using plain jdbc
     * @param periodId coverage period id (foreign key)
     * @param searchEventId coverage search event id (foreign key)
     * @param batch list of beneficiary ids to be added as a batch
     */
    private void insertBatch(int periodId, long searchEventId, List<String> batch) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_COVERAGE)) {

            // Prepare a batch of beneficiary ids to be inserted
            for (String beneficiaryId : batch) {
                statement.setInt(1, periodId);
                statement.setLong(2, searchEventId);
                statement.setString(3, beneficiaryId);

                statement.addBatch();
            }

            statement.executeBatch();

        } catch (SQLException sqlException) {
            throw new RuntimeException("failed to insert coverage information", sqlException);
        }
    }

    @Override
    public void deletePreviousSearch(int periodId) {
        // Delete previous in progress search before this one
        deletePreviousSearch(periodId, 1);
    }

    /**
     * Delete all coverage information related to the results of a search defined by an offset into the past.
     *
     * An offset of 0 finds the current IN_PROGRESS search event and deletes all coverage information associated
     * with that event.
     *
     * @param periodId given coverage period (contractId, month, year) to delete search info for
     * @param offset offset into the past 0 is last search done successfully, 1 is search before that, etc.
     */
    private void deletePreviousSearch(int periodId, int offset) {
        CoveragePeriod period = findCoveragePeriod(periodId);
        Optional<CoverageSearchEvent> searchEvent = coverageSearchEventRepo
                .findSearchEventWithOffset(periodId, JobStatus.IN_PROGRESS.name(), offset);

        // Only delete previous search if a previous search exists.
        if (searchEvent.isPresent()) {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("DELETE FROM coverage cov WHERE " +
                         " cov.bene_coverage_period_id = ? AND cov.bene_coverage_search_event_id = ?")) {
                statement.setLong(1, period.getId());
                statement.setLong(2, searchEvent.get().getId());
                statement.execute();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    // todo: add in appropriate location either the completeCoverageSearch method or within the EOB Search on conclusion
    //      of the current search. This needs to run after the completion of every search
    @Override
    public List<CoverageSummary> pageCoverage(int pageNumber, int pageSize, List<Integer> coveragePeriodIds) {

        CoveragePeriod coveragePeriod = coveragePeriodRepo.getOne(coveragePeriodIds.get(0));

        Map<String, List<CoverageMembership>> coverageMembership = findCoverageMemberships(pageNumber, pageSize, coveragePeriodIds);

        return coverageMembership.entrySet().stream()
                .map(obj -> summarizeCoverageMembership(coveragePeriod.getContract(), obj))
                .collect(toList());
    }

    @Override
    public List<CoverageSummary> pageCoverage(int pageNumber, int pageSize, Integer... coveragePeriodIds) {
        return pageCoverage(pageNumber, pageSize, List.of(coveragePeriodIds));
    }

    /**
     * Find page of beneficiaries then look up all of their coverage information for a range of identified coverage
     * periods.
     */
    private Map<String, List<CoverageMembership>> findCoverageMemberships(int pageNumber, int pageSize, List<Integer> coveragePeriodIds) {
        List<CoveragePeriod> coveragePeriods = coveragePeriodRepo.findAllById(coveragePeriodIds);

        if (coveragePeriods.size() != coveragePeriodIds.size()) {
            throw new IllegalArgumentException("at least one coverage period id not found in database");
        }

        // Look up a page of beneficiaries sorted by id
        List<String> beneficiaries = findActiveBeneficiaryIds(pageNumber, pageSize, coveragePeriodIds);

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
                    .collect(groupingBy(CoverageMembership::getBeneficiaryId));

            coverageMemberships.putAll(coverages);
        }

        return coverageMemberships;
    }

    @Override
    public List<String> findActiveBeneficiaryIds(int pageNumber, int pageSize, List<Integer> coveragePeriods) {

        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("ids", coveragePeriods)
                .addValue("offset", pageNumber * pageSize)
                .addValue("limit", pageSize);

        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);

        return template.query(SELECT_BENEFICIARIES_BATCH, parameters,
                (results, rowNum) -> results.getString(1));
    }

    private List<CoverageMembership> findCoverageInformation(List<Integer> coveragePeriodIds, List<String> beneficiaryIds) {

        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("coveragePeriods", coveragePeriodIds)
                .addValue("beneficiaryIds", beneficiaryIds);

        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);

        return template.query(SELECT_COVERAGE_INFORMATION, parameters,
                (rs, rowNum) -> new CoverageMembership(
                        rs.getString(1),
                        rs.getInt(2),
                        rs.getInt(3)
                ));
    }

    /**
     * Summarize the coverage of one beneficiary for
     */
    private CoverageSummary summarizeCoverageMembership(Contract contract,
                                                        Map.Entry<String, List<CoverageMembership>> membershipInfo) {

        String beneficiaryId = membershipInfo.getKey();
        List<CoverageMembership> membershipMonths = membershipInfo.getValue();

        if (membershipMonths.size() == 1) {
            LocalDate start = fromRawResults(membershipMonths.get(0));
            DateRange range = asDateRange(start, start);
            return new CoverageSummary(beneficiaryId, contract, Collections.singletonList(range));
        }

        List<DateRange> dateRanges = new ArrayList<>();

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

        return new CoverageSummary(beneficiaryId, contract, dateRanges);

    }

    /**
     * Convert raw array results into object
     */
    private LocalDate fromRawResults(CoverageMembership result) {
        return LocalDate.of(result.getYear(), result.getMonth(), 1);
    }

    private DateRange asDateRange(LocalDate localStartDate, LocalDate localEndDate)  {
        return FilterOutByDate.getDateRange(localStartDate.getMonthValue(), localStartDate.getYear(),
                localEndDate.getMonthValue(), localEndDate.getYear());
    }

    // todo: create diff and log on completion of every search. This information may be logged to both
    //      kinesis and sql as part of a subsequent issue.
    @Override
    public CoverageSearchDiff searchDiff(int periodId) {

        CoveragePeriod period = findCoveragePeriod(periodId);

        if (period.getStatus() != JobStatus.IN_PROGRESS) {
            throw new InvalidJobStateTransition("Cannot diff a currently running search against previous search because results may be added");
        }

        Optional<CoverageSearchEvent> previousSearch = coverageSearchEventRepo.findSearchEventWithOffset(periodId, JobStatus.IN_PROGRESS.name(), 1);
        Optional<CoverageSearchEvent> currentSearch = coverageSearchEventRepo.findSearchEventWithOffset(periodId, JobStatus.IN_PROGRESS.name(), 0);

        int previousCount = 0;
        if (previousSearch.isPresent()) {
            previousCount = coverageRepo.countByCoverageSearchEvent(previousSearch.get());
        }

        CoverageSearchEvent current = currentSearch.orElseThrow(() -> new RuntimeException("could not find latest in progress search event"));
        int currentCount = coverageRepo.countByCoverageSearchEvent(current);

        int unchanged = 0;
        if (previousCount > 0) {
            unchanged = coverageRepo.countIntersection(previousSearch.get().getId(), current.getId());
        }

        return new CoverageSearchDiff(period, previousCount, currentCount, unchanged);
    }

    @Override
    public List<CoveragePeriod> findNeverSearched() {
        return coveragePeriodRepo.findAllByStatusIsNull();
    }

    @Override
    public List<CoveragePeriod> coverageNotUpdatedSince(int month, int year, OffsetDateTime lastSuccessful) {
        List<CoveragePeriod> allCoveragePeriodsForMonth = coveragePeriodRepo.findAllByMonthAndYear(month, year);

        return allCoveragePeriodsForMonth.stream()
                .filter(period -> period.getStatus() != JobStatus.SUBMITTED)
                .filter(period -> {
                    Optional<CoverageSearchEvent>  search = coverageSearchEventRepo.findSearchEventWithOffset(period.getId(), JobStatus.SUCCESSFUL.name(), 0);

                    if (search.isPresent()) {
                        OffsetDateTime created = search.get().getCreated();
                        return !created.isAfter(lastSuccessful);
                    }

                    // Never been searched and we need to do the search now
                    return true;
                }).collect(toList());

    }

    @Override
    public CoverageSearchEvent submitCoverageSearch(int periodId, String description) {

        CoveragePeriod period = findCoveragePeriod(periodId);
        JobStatus jobStatus = period.getStatus();

        if (jobStatus == JobStatus.IN_PROGRESS || jobStatus == JobStatus.SUBMITTED) {
            throw new InvalidJobStateTransition("cannot change an " + JobStatus.IN_PROGRESS
                    + " contract mapping job to " + JobStatus.SUBMITTED);
        }
        return updateStatus(period, description, JobStatus.SUBMITTED);
    }

    @Override
    public CoverageSearchEvent startCoverageSearch(int periodId, String description) {

        CoveragePeriod period = findCoveragePeriod(periodId);
        JobStatus jobStatus = period.getStatus();

        if (jobStatus != JobStatus.SUBMITTED) {
            throw new InvalidJobStateTransition("cannot change from " + period.getStatus()
                    + " to " + JobStatus.IN_PROGRESS);
        }

        return updateStatus(period, description, JobStatus.IN_PROGRESS);
    }

    @Override
    public CoverageSearchEvent cancelCoverageSearch(int periodId, String description) {

        CoveragePeriod period = findCoveragePeriod(periodId);
        JobStatus jobStatus = period.getStatus();

        if (jobStatus != JobStatus.SUBMITTED) {
            throw new InvalidJobStateTransition("cannot change from " + jobStatus
                    + " to " + JobStatus.CANCELLED);
        }

        return updateStatus(period, description, JobStatus.CANCELLED);
    }

    @Override
    public CoverageSearchEvent failCoverageSearch(int periodId, String description) {

        CoveragePeriod period = findCoveragePeriod(periodId);
        JobStatus jobStatus = period.getStatus();

        if (jobStatus != JobStatus.IN_PROGRESS) {
            throw new InvalidJobStateTransition("cannot change from " + jobStatus
                    + " to " + JobStatus.FAILED);
        }

        // Delete all results from current search that is failing
        deletePreviousSearch(periodId, 0);

        return updateStatus(period, description, JobStatus.FAILED);
    }

    @Override
    public CoverageSearchEvent completeCoverageSearch(int periodId, String description) {

        CoveragePeriod period = findCoveragePeriod(periodId);
        JobStatus jobStatus = period.getStatus();

        if (jobStatus != JobStatus.IN_PROGRESS) {
            throw new InvalidJobStateTransition("cannot change from " + jobStatus
                    + " to " + JobStatus.SUCCESSFUL);
        }

        deletePreviousSearch(periodId);

        return updateStatus(period, description, JobStatus.SUCCESSFUL);
    }

    // todo: decide whether to call vacuumCoverage internally in the service on completeCoverageSearch
    //      and/or deletePerviousSearch, or wait until all search jobs are done.
    //      This especially applies to bulk deletes which may invalidate the pg_visibility map in Postgres
    //      thus making all SELECT queries hit disk instead of memory.
    @Override
    public void vacuumCoverage() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("VACUUM coverage")) {
            statement.execute();
        } catch (SQLException exception) {
            throw new RuntimeException("Could not vacuum coverage table", exception);
        }
    }

    private static void checkMonthAndYear(int month, int year) {
        if (month < 1 || month > 12) {
            final String errMsg = "invalid value for month. Month must be between 1 and 12";
            log.error("{} - invalid month :[{}]", errMsg, month);
            throw new IllegalArgumentException(errMsg);
        }

        OffsetDateTime time = OffsetDateTime.now(ZoneOffset.UTC);
        int currentYear = time.getYear();

        if (year < AB2D_EPOCH || year > currentYear) {
            final String errMsg = "invalid value for year. Year must be between " + AB2D_EPOCH + " and " + currentYear;
            log.error("{} - invalid year :[{}]", errMsg, year);
            throw new IllegalArgumentException(errMsg);
        }

        OffsetDateTime proposed = OffsetDateTime.of(LocalDate.of(year, month, 1), LocalTime.of(0, 0, 0), ZoneOffset.UTC);
        if (proposed.isAfter(time)) {
            throw new IllegalArgumentException("Pulling coverage period from future. Current time " + time + " is before proposed time " + proposed);
        }
    }

    private CoverageSearchEvent updateStatus(CoveragePeriod period, String description, JobStatus status) {
        CoverageSearchEvent currentStatus = getLastEvent(period);

        logStatusChange(period, description, status);

        JobStatus oldStatus = currentStatus != null ? currentStatus.getNewStatus() : null;

        CoverageSearchEvent newStatus = new CoverageSearchEvent();
        newStatus.setCoveragePeriod(period);
        newStatus.setOldStatus(oldStatus);
        newStatus.setNewStatus(status);
        newStatus.setDescription(description);

        period.setStatus(status);

        coveragePeriodRepo.saveAndFlush(period);

        return coverageSearchEventRepo.saveAndFlush(newStatus);
    }

    private void logStatusChange(CoveragePeriod period, String description, JobStatus jobStatus) {
        log.info("Updating job state for search {}-{}-{} from {} to {} due to {}", period.getContract().getId(),
                period.getMonth(), period.getYear(), period.getStatus(), jobStatus, description);
    }

    /**
     * Eagerly load a {@link CoveragePeriod} by id.
     * @param periodId coverage period id to load
     * @return coverage period if found
     * @throws ResourceNotFoundException if coverage period is not found
     */
    private CoveragePeriod findCoveragePeriod(int periodId) {
        return coveragePeriodRepo.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("coverage period with id not found"));
    }

    private CoverageSearchEvent findCoverageSearchEvent(long periodId) {
        return coverageSearchEventRepo.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("coverage period with id not found"));
    }
}
