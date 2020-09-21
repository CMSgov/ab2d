package gov.cms.ab2d.common.service;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.CoveragePeriod;
import gov.cms.ab2d.common.model.CoverageSearchDiff;
import gov.cms.ab2d.common.model.CoverageSearchEvent;
import gov.cms.ab2d.common.model.CoverageSummary;
import gov.cms.ab2d.common.model.DateRange;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.repository.CoveragePeriodRepository;
import gov.cms.ab2d.common.repository.CoverageRepository;
import gov.cms.ab2d.common.repository.CoverageSearchEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

    private static final int BATCH_INSERT_SIZE = 1000;
    private static final int BATCH_SELECT_SIZE = 200;

    private static final String INSERT = "INSERT INTO coverage " +
            "(bene_coverage_period_id, bene_coverage_search_event_id, beneficiary_id) VALUES(?,?,?)";

    @Autowired
    private CoverageRepository coverageRepo;

    @Autowired
    private CoveragePeriodRepository coveragePeriodRepo;

    @Autowired
    private CoverageSearchEventRepository coverageSearchEventRepo;

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
    public CoverageSearchEvent insertCoverage(int periodId, long searchEventId, List<String> beneficiaryIds) {

        // Make sure that coverage period and searchEvent actually exist in the database before inserting
        findCoveragePeriod(periodId);
        CoverageSearchEvent searchEvent = findCoverageSearchEvent(searchEventId);

        int written = 0;
        while (written < beneficiaryIds.size()) {

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(INSERT)) {

                int stopCondition = written + BATCH_INSERT_SIZE > beneficiaryIds.size() ? beneficiaryIds.size() : written + BATCH_INSERT_SIZE;

                for (int index = written; index < stopCondition; index++) {
                    String beneficiaryId = beneficiaryIds.get(index);
                    statement.setInt(1, periodId);
                    statement.setLong(2, searchEventId);
                    statement.setString(3, beneficiaryId);

                    statement.addBatch();
                }

                statement.executeBatch();

            } catch (SQLException sqlException) {
                sqlException.printStackTrace();
                throw new RuntimeException("failed sql exception");
            }

            written += BATCH_INSERT_SIZE;
        }

        return searchEvent;
    }

    @Override
    public void deletePreviousSearch(int periodId) {
        CoveragePeriod period = findCoveragePeriod(periodId);
        Optional<CoverageSearchEvent> searchEvent = coverageSearchEventRepo.findSearchWithOffset(periodId, 1);

        searchEvent.ifPresent(coverageSearchEvent ->
                coverageRepo.removeAllByCoveragePeriodAndCoverageSearchEvent(period, coverageSearchEvent));
    }

    // todo: add in appropriate location either the completeCoverageSearch method or within the EOB Search on conclusion
    //      of the current search. This needs to run after the completion of every search
    @Override
    public List<CoverageSummary> pageCoverage(int pageNumber, int pageSize, List<Integer> coveragePeriodIds) {

        CoveragePeriod coveragePeriod = coveragePeriodRepo.getOne(coveragePeriodIds.get(0));

        Map<String, List<Object[]>> coverageMembership = findCoverageMemberships(pageNumber, pageSize, coveragePeriodIds);

        return coverageMembership.entrySet().stream()
                .map(obj -> summarizeCoverageMembership(coveragePeriod.getContract(), obj))
                .collect(toList());
    }

    @Override
    public List<CoverageSummary> pageCoverage(int pageNumber, int pageSize, Integer... coveragePeriodIds) {
        return pageCoverage(pageNumber, pageSize, List.of(coveragePeriodIds));
    }

    private Map<String, List<Object[]>> findCoverageMemberships(int pageNumber, int pageSize, List<Integer> coveragePeriodIds) {
        List<CoveragePeriod> coveragePeriods = coveragePeriodRepo.findAllById(coveragePeriodIds);

        Page<String> beneficiaryPage = coverageRepo.findActiveBeneficiaryIds(coveragePeriods, PageRequest.of(pageNumber, pageSize));
        List<String> beneficiaries = beneficiaryPage.getContent();

        Map<String, List<Object[]>> coverageMemberships = new HashMap<>(beneficiaries.size() * coveragePeriodIds.size());
        int startIndex = 0;
        while (startIndex < beneficiaries.size()) {
            int endIndex = Math.min(startIndex + BATCH_SELECT_SIZE, beneficiaries.size());
            List<Object[]> rawResults = coverageRepo.findCoverageInformation(coveragePeriodIds, beneficiaries.subList(startIndex, endIndex));
            Map<String, List<Object[]>> coverages = rawResults.stream()
                    .collect(groupingBy(raw -> (String) raw[0]));
            coverageMemberships.putAll(coverages);

            startIndex = endIndex;
        }

        return coverageMemberships;
    }

    private CoverageSummary summarizeCoverageMembership(Contract contract, Map.Entry<String, List<Object[]>> membershipInfo) {

        String beneficiaryId = membershipInfo.getKey();
        List<Object[]> membershipMonths = membershipInfo.getValue();

        if (membershipMonths.size() == 1) {
            LocalDate start = fromRawResults(membershipMonths.get(0));
            DateRange range = new DateRange(start, start.plusMonths(1));
            return new CoverageSummary(beneficiaryId, contract, Collections.singletonList(range));
        } else {

            List<DateRange> dateRanges = new ArrayList<>();

            // Remove is dangerous but more efficient than subList or skip
            LocalDate begin = fromRawResults(membershipMonths.remove(0));
            LocalDate last = begin;
            for (Object[] membership : membershipMonths) {
                LocalDate next = fromRawResults(membership);

                if (!next.isEqual(last)) {
                    dateRanges.add(new DateRange(begin, last.plusMonths(1)));
                    begin = next;
                    last = next;
                // Extend the date range by one month
                } else {
                    last = next;
                }
            }

            if (begin.equals(last)) {
                dateRanges.add(new DateRange(begin, begin.plusMonths(1)));
            } else {
                dateRanges.add(new DateRange(begin, last.plusMonths(1)));
            }

            return new CoverageSummary(beneficiaryId, contract, dateRanges);
        }
    }

    private LocalDate fromRawResults(Object[] result) {
        int year = (int) result[1];
        int month = (int) result[2];
        return LocalDate.of(year, month, 1);
    }

    // todo: create diff and log on completion of every search. This information may be logged to both
    //      kinesis and sql as part of a subsequent issue.
    @Override
    public CoverageSearchDiff searchDiff(int periodId) {

        CoveragePeriod period = findCoveragePeriod(periodId);

        if (period.getStatus() != JobStatus.IN_PROGRESS) {
            throw new InvalidJobStateTransition("Cannot diff a currently running search against previous search because results may be added");
        }

        Optional<CoverageSearchEvent> previousSearch = coverageSearchEventRepo.findSearchWithOffset(periodId, 1);
        Optional<CoverageSearchEvent> currentSearch = coverageSearchEventRepo.findSearchWithOffset(periodId, 0);

        int previousCount = 0;
        if (previousSearch.isPresent()) {
            previousCount = coverageRepo.countByCoverageSearchEvent(previousSearch.get());
        }

        CoverageSearchEvent current = currentSearch.get();
        int currentCount = coverageRepo.countByCoverageSearchEvent(current);

        int unchanged = 0;
        if (previousCount > 0) {
            unchanged = coverageRepo.countIntersection(previousSearch.get().getId(), currentSearch.get().getId());
        }

        return new CoverageSearchDiff(period, previousCount, currentCount, unchanged);
    }

    @Override
    public CoverageSearchEvent submitCoverageSearch(int periodId, String description) {

        CoveragePeriod period = findCoveragePeriod(periodId);
        JobStatus jobStatus = period.getStatus();

        if (jobStatus == JobStatus.IN_PROGRESS) {
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

        return updateStatus(period, description, JobStatus.SUCCESSFUL);
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
