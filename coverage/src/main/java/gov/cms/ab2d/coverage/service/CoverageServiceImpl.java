package gov.cms.ab2d.coverage.service;

import com.newrelic.api.agent.Trace;
import gov.cms.ab2d.coverage.model.ContractForCoverageDTO;
import gov.cms.ab2d.coverage.model.CoverageCount;
import gov.cms.ab2d.coverage.model.CoverageJobStatus;
import gov.cms.ab2d.coverage.model.CoverageMapping;
import gov.cms.ab2d.coverage.model.CoveragePagingRequest;
import gov.cms.ab2d.coverage.model.CoveragePagingResult;
import gov.cms.ab2d.coverage.model.CoveragePeriod;
import gov.cms.ab2d.coverage.model.CoverageSearch;
import gov.cms.ab2d.coverage.model.CoverageSearchDiff;
import gov.cms.ab2d.coverage.model.CoverageSearchEvent;
import gov.cms.ab2d.coverage.model.Identifiers;
import gov.cms.ab2d.coverage.repository.CoverageDeltaRepository;
import gov.cms.ab2d.coverage.repository.CoveragePeriodRepository;
import gov.cms.ab2d.coverage.repository.CoverageSearchEventRepository;
import gov.cms.ab2d.coverage.repository.CoverageSearchRepository;
import gov.cms.ab2d.coverage.repository.CoverageServiceRepository;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import static gov.cms.ab2d.common.util.DateUtil.AB2D_EPOCH_YEAR;
import static gov.cms.ab2d.eventclient.config.Ab2dEnvironment.PUBLIC_LIST;
import static gov.cms.ab2d.eventclient.events.SlackEvents.COVERAGE_DELETE_FAILED;
import static gov.cms.ab2d.eventclient.events.SlackEvents.COVERAGE_UPDATE_FAILED;
import static java.util.stream.Collectors.toList;

/**
 * Interact with Coverage, CoveragePeriod, and CoverageSearchEvent entities/tables including
 * bulk insertions, reads, and deletes. Wraps more complex behavior implemented in {@link CoverageServiceRepository}
 *
 * Business requirements implemented in the code here:
 *
 *      - Enrollment and hence EOBs must be after AB2D epoch {@link #checkMonthAndYear(int, int)}
 *      - Only one copy of the enrollment should exist in the database
 *          {@link #completeSearch(int, String)}, {@link #failSearch(int, String)} via
 *          {@link CoverageServiceRepository#deleteCurrentSearch(CoveragePeriod)},
 *          {@link CoverageServiceRepository#deletePreviousSearches(CoveragePeriod, int)}
 *      - Coverage searches should transition states logically SUBMITTED -> IN_PROGRESS for example
 *      - Only one coverage search should be running for any given {@link CoveragePeriod}
 *      - Changes to enrollment should be tracked between updates
 *
 * Each status change method has side effects beyond change a {@link CoveragePeriod}. For example, {@link #completeSearch(int, String)}
 * marks a search as successful and deletes all previous enrollment related to a search. Sometimes these methods are
 * more intensive than appears on the surface.
 *
 * Warning: {@link org.springframework.data.jpa.repository.JpaRepository#getOne(Object)}
 * loads lazily which causes issues when an object is returned by the {@link CoverageServiceImpl}
 * to another context which may not have a {@link Transactional} context.
 *
 * To prevent issues getOne is not used in this service at all.
 */
@AllArgsConstructor
@Slf4j
@Service
@Transactional
public class CoverageServiceImpl implements CoverageService {

    private final CoveragePeriodRepository coveragePeriodRepo;

    private final CoverageSearchEventRepository coverageSearchEventRepo;

    private final CoverageSearchRepository coverageSearchRepo;

    private final CoverageServiceRepository coverageServiceRepo;

    private final CoverageDeltaRepository coverageDeltaRepository;

    private final SQSEventClient eventLogger;

    private static final String JOB_TRANSITION_ERROR_PRESTRING = "cannot change from ";

    @Override
    public CoveragePeriod getCoveragePeriod(ContractForCoverageDTO contract, int month, int year) {
        checkMonthAndYear(month, year);

        Optional<CoveragePeriod> period = coveragePeriodRepo.findByContractNumberAndMonthAndYear(contract.getContractNumber(), month, year);
        return period.orElseThrow(() ->
                new EntityNotFoundException("could not find coverage period matching contract, month, and year"));
    }

    @Override
    public List<CoveragePeriod> getCoveragePeriods(int month, int year) {
        checkMonthAndYear(month, year);

        return coveragePeriodRepo.findAllByMonthAndYear(month, year);
    }

    @Override
    public CoveragePeriod getCreateIfAbsentCoveragePeriod(ContractForCoverageDTO contract, int month, int year) {
        checkMonthAndYear(month, year);

        Optional<CoveragePeriod> existing = coveragePeriodRepo.findByContractNumberAndMonthAndYear(contract.getContractNumber(), month, year);

        if (existing.isPresent()) {
            return existing.get();
        }

        CoveragePeriod period = new CoveragePeriod();
        period.setContractNumber(contract.getContractNumber());
        period.setMonth(month);
        period.setYear(year);

        return coveragePeriodRepo.save(period);
    }

    @Override
    public List<CoveragePeriod> findAssociatedCoveragePeriods(String contractNumber) {
        return coveragePeriodRepo.findAllByContractNumber(contractNumber);
    }

    @Override
    public boolean isCoveragePeriodInProgress(int periodId) {
        CoveragePeriod period = findCoveragePeriod(periodId);
        CoverageJobStatus coverageJobStatus = period.getStatus();
        return coverageJobStatus == CoverageJobStatus.IN_PROGRESS;
    }

    @Override
    public int countBeneficiariesByCoveragePeriod(List<CoveragePeriod> coveragePeriods) {
        List<Integer> ids = coveragePeriods.stream().map(CoveragePeriod::getId).collect(toList());
        return coverageServiceRepo.countBeneficiariesByPeriods(ids, coveragePeriods.get(0).getContractNumber());
    }

    @Override
    public List<CoverageCount> countBeneficiariesForContracts(List<ContractForCoverageDTO> contracts) {
        int partitionSize = 5;
        List<List<ContractForCoverageDTO>> contractPartitions = new ArrayList<>();

        // Split queries into smaller pieces so queries don't time out
        for (int idx = 0; idx < contracts.size(); idx += partitionSize) {
            contractPartitions.add(contracts.subList(idx, Math.min(idx + partitionSize, contracts.size())));
        }

        return contractPartitions.stream().map(coverageServiceRepo::countByContractCoverage)
                .flatMap(List::stream).collect(toList());
    }

    @Override
    public boolean canEOBSearchBeStarted(int periodId) {
        return !isCoveragePeriodInProgress(periodId);
    }

    @Override
    public CoverageJobStatus getSearchStatus(int periodId) {
        CoveragePeriod coverageSearch = findCoveragePeriod(periodId);
        return coverageSearch.getStatus();
    }

    @Override
    public Optional<CoverageSearchEvent> findMostRecentEvent(int periodId) {
        CoveragePeriod period = findCoveragePeriod(periodId);
        return findMostRecentEvent(period);
    }

    private Optional<CoverageSearchEvent> findMostRecentEvent(CoveragePeriod period) {
        return coverageSearchEventRepo.findFirstByCoveragePeriodOrderByCreatedDesc(period);
    }

    private CoverageSearchEvent getLastEvent(CoveragePeriod period) {
        return findMostRecentEvent(period).orElse(null);
    }

    @Override
    @Trace(metricName = "InsertingCoverage", dispatcher = true)
    public CoverageSearchEvent insertCoverage(long searchEventId, Set<Identifiers> beneficiaryIds) {

        // Make sure that coverage period and searchEvent actually exist in the database before inserting
        CoverageSearchEvent searchEvent = findCoverageSearchEvent(searchEventId);
        coverageServiceRepo.insertBatches(searchEvent, beneficiaryIds);

        log.info("Vacuuming coverage table now");

        // Update indices after every batch of insertions to make sure speed of database is preserved
        coverageServiceRepo.vacuumCoverage();

        log.info("Vacuuming coverage finished now");

        return searchEvent;
    }

    @Override
    @Trace
    public CoveragePagingResult pageCoverage(CoveragePagingRequest pagingRequest) {
        return coverageServiceRepo.pageCoverage(pagingRequest);
    }

    // todo: consider removing now that the CoverageDeltaRepository functionality exists
    //  We can write alarms using that delta table if we need to.
    @Override
    @Trace(metricName = "SearchDiff", dispatcher = true)
    public CoverageSearchDiff searchDiff(int periodId) {

        CoveragePeriod period = findCoveragePeriod(periodId);

        if (period.getStatus() != CoverageJobStatus.IN_PROGRESS) {
            throw new InvalidJobStateTransition("Cannot diff a currently running search against previous search because results may be added");
        }

        Optional<CoverageSearchEvent> previousSearch = findEventWithSuccessfulOffset(periodId, 1);
        Optional<CoverageSearchEvent> currentSearch = findEventWithSuccessfulOffset(periodId, 0);
        CoverageSearchEvent current = currentSearch.orElseThrow(() -> new RuntimeException("could not find latest in progress search event"));

        int previousCount = 0;
        if (previousSearch.isPresent()) {
            previousCount = coverageServiceRepo.countBySearchEvent(previousSearch.get());
        }

        int currentCount = coverageServiceRepo.countBySearchEvent(current);

        int unchanged = 0;
        if (previousCount > 0) {
            log.info("Calculating the deltas for the search period {}-{}-{}", period.getContractNumber(),
                    period.getMonth(), period.getYear());
            coverageDeltaRepository.trackDeltas(previousSearch.get(), current);
            unchanged = coverageServiceRepo.countIntersection(previousSearch.get(), current);
        }

        return new CoverageSearchDiff(period, previousCount, currentCount, unchanged);
    }

    /**
     * Find data saved from in progress search
     * @param periodId coverage period id corresponding to contract, year, and month
     * @param successfulOffset number of successful searches in the past to look at [0,n)
     * @return an in progress search event corresponding to the offset if found
     */
    public Optional<CoverageSearchEvent> findEventWithSuccessfulOffset(int periodId, int successfulOffset) {
        List<CoverageSearchEvent> events = coverageSearchEventRepo.findByPeriodDesc(periodId, 100);

        int successfulSearches = 0;
        for (CoverageSearchEvent event : events) {
            if (event.getNewStatus() == CoverageJobStatus.SUCCESSFUL) {
                successfulSearches += 1;
            } else if (event.getNewStatus() == CoverageJobStatus.IN_PROGRESS && successfulSearches == successfulOffset) {
                return Optional.of(event);
            }
        }

        return Optional.empty();
    }

    @Override
    public List<CoveragePeriod> coveragePeriodNeverSearchedSuccessfully() {

        log.debug("attempting to find all never successfully searched coverage periods");

        List<CoveragePeriod> neverSuccessful = coveragePeriodRepo.findAllByLastSuccessfulJobIsNull();

        return neverSuccessful.stream().filter(period ->
                period.getStatus() != CoverageJobStatus.SUBMITTED && period.getStatus() != CoverageJobStatus.IN_PROGRESS)
                .collect(toList());
    }

    @Override
    public List<CoveragePeriod> coveragePeriodNotUpdatedSince(int month, int year, OffsetDateTime lastSuccessful) {

        log.debug("Looking for coverage periods in {}/{} not updated since {}", month, year, lastSuccessful);

        return coveragePeriodRepo.findAllByMonthAndYearAndLastSuccessfulJobLessThanEqual(month, year, lastSuccessful);
    }

    @Override
    public List<CoveragePeriod> coveragePeriodStuckJobs(OffsetDateTime startedBefore) {

        log.debug("attempting to find all coverage searches that have been in progress for too long");

        List<CoverageSearchEvent> events = coverageSearchEventRepo
                .findStuckAtStatus(CoverageJobStatus.IN_PROGRESS.name(), startedBefore);

        return events.stream().map(CoverageSearchEvent::getCoveragePeriod)
                .collect(toList());
    }

    @Override
    public Optional<CoverageSearchEvent> submitSearch(int periodId, String description) {
        return submitSearch(periodId, 0, description);
    }

    @Override
    public Optional<CoverageSearchEvent> submitSearch(int periodId, int attempts, String description) {

        CoveragePeriod period = findCoveragePeriod(periodId);
        CoverageJobStatus coverageJobStatus = period.getStatus();

        if (coverageJobStatus == CoverageJobStatus.IN_PROGRESS || coverageJobStatus == CoverageJobStatus.SUBMITTED) {
            return Optional.empty();
        }

        // Add to queue of jobs to do
        CoverageSearch search = new CoverageSearch();
        search.setPeriod(period);
        search.setAttempts(attempts);
        coverageSearchRepo.saveAndFlush(search);

        return Optional.of(updateStatus(period, description, CoverageJobStatus.SUBMITTED));
    }

    @Override
    public CoverageSearchEvent resubmitSearch(int periodId, int attempts, String failedDescription,
                                              String restartDescription, boolean prioritize) {
        CoveragePeriod period = findCoveragePeriod(periodId);

        updateStatus(period, failedDescription, CoverageJobStatus.FAILED, false);

        // Add to queue of jobs to do
        CoverageSearch search = new CoverageSearch();
        search.setPeriod(period);
        search.setAttempts(attempts);

        // Force to front of the queue if necessary
        if (prioritize) {
            search.setCreated(OffsetDateTime.of(2000, 1, 1,
                    0, 0, 0, 0, ZoneOffset.UTC));
        }

        coverageSearchRepo.saveAndFlush(search);

        return updateStatus(period, restartDescription, CoverageJobStatus.SUBMITTED);
    }

    @Override
    public Optional<CoverageSearchEvent> prioritizeSearch(int periodId, String description) {
        return prioritizeSearch(periodId, 0, description);
    }

    @Override
    public Optional<CoverageSearchEvent> prioritizeSearch(int periodId, int attempts, String description) {

        CoveragePeriod period = findCoveragePeriod(periodId);
        CoverageJobStatus coverageJobStatus = period.getStatus();

        if (coverageJobStatus == CoverageJobStatus.IN_PROGRESS || coverageJobStatus == CoverageJobStatus.SUBMITTED) {
            return Optional.empty();
        }

        // Add to queue of jobs to do
        CoverageSearch search = new CoverageSearch();
        search.setPeriod(period);
        search.setAttempts(attempts);
        search.setCreated(OffsetDateTime.of(2000, 1, 1,
                0, 0, 0, 0, ZoneOffset.UTC));
        coverageSearchRepo.saveAndFlush(search);

        return Optional.of(updateStatus(period, description, CoverageJobStatus.SUBMITTED));
    }

    @Override
    public Optional<CoverageMapping> startSearch(CoverageSearch submittedSearch, String description) {

        if (submittedSearch == null) {
            return Optional.empty();
        }

        CoveragePeriod period = submittedSearch.getPeriod();

        CoverageSearchEvent coverageSearchEvent = updateStatus(period, description, CoverageJobStatus.IN_PROGRESS);

        return Optional.of(new CoverageMapping(coverageSearchEvent, submittedSearch));
    }

    @Override
    public CoverageSearchEvent cancelSearch(int periodId, String description) {

        CoveragePeriod period = findCoveragePeriod(periodId);
        CoverageJobStatus coverageJobStatus = period.getStatus();

        if (coverageJobStatus != CoverageJobStatus.SUBMITTED) {
            throw new InvalidJobStateTransition(JOB_TRANSITION_ERROR_PRESTRING + coverageJobStatus
                    + " to " + CoverageJobStatus.CANCELLED);
        }

        coverageSearchRepo.deleteCoverageSearchByPeriod(period);

        return updateStatus(period, description, CoverageJobStatus.CANCELLED);
    }

    @Override
    public CoverageSearchEvent failSearch(int periodId, String description) {

        CoveragePeriod period = findCoveragePeriod(periodId);
        CoverageJobStatus coverageJobStatus = period.getStatus();

        if (coverageJobStatus != CoverageJobStatus.IN_PROGRESS) {
            throw new InvalidJobStateTransition(JOB_TRANSITION_ERROR_PRESTRING + coverageJobStatus
                    + " to " + CoverageJobStatus.FAILED);
        }

        try {
            // Delete all results from current search that is failing
            coverageServiceRepo.deleteCurrentSearch(period);
        } catch (Exception exception) {
            String issue = String.format(COVERAGE_DELETE_FAILED + " Failed to delete coverage for a failed search for %s-%d-%d. " +
                            "There could be duplicate enrollment data in the db",
                    period.getContractNumber(), period.getYear(), period.getMonth());
            eventLogger.alert(issue, PUBLIC_LIST);
            throw exception;
        }

        // Before finishing up alert AB2D team that there could be a problem with enrollment
        String issue = String.format(COVERAGE_UPDATE_FAILED + " Failed to update coverage for %s-%d-%d." +
                " There could be out of date enrollment data in the db",
                period.getContractNumber(), period.getYear(), period.getMonth());
        eventLogger.alert(issue, PUBLIC_LIST);

        return updateStatus(period, description, CoverageJobStatus.FAILED);
    }

    @Override
    public CoverageSearchEvent completeSearch(int periodId, String description) {

        CoveragePeriod period = findCoveragePeriod(periodId);
        CoverageJobStatus coverageJobStatus = period.getStatus();

        if (coverageJobStatus != CoverageJobStatus.IN_PROGRESS) {
            throw new InvalidJobStateTransition(JOB_TRANSITION_ERROR_PRESTRING + coverageJobStatus
                    + " to " + CoverageJobStatus.SUCCESSFUL);
        }

        // todo: log to kinesis as well
        String contractNumber = period.getContractNumber();

        /*
         * Log the difference between any earlier enrollment we have for the given coverage period
         * and the update just performed.
         *
         * The intersection should be more than 95%.
         */
        CoverageSearchDiff diff = searchDiff(periodId);
        log.info("{}-{}-{} difference between previous metadata and current metadata\n {}",
                contractNumber, period.getYear(), period.getMonth(), diff);

        // Execute after diff
        // this deletion will remove as much past information as it can find
        try {
            coverageServiceRepo.deletePreviousSearches(period, 1);
        } catch (Exception exception) {
            String issue = String.format(COVERAGE_DELETE_FAILED + " Failed to delete old coverage for newly completed %s-%d-%d." +
                            " There could be duplicate enrollment data in the db",
                    period.getContractNumber(), period.getYear(), period.getMonth());
            eventLogger.alert(issue, PUBLIC_LIST);
            throw exception;
        }

        return updateStatus(period, description, CoverageJobStatus.SUCCESSFUL);
    }

    /**
     * Verify that the proposed year and month for a coverage period is not before AB2D began serving claims
     * or in the future.
     *
     * @throws IllegalArgumentException when month and year are not within bounds
     */
    private static void checkMonthAndYear(int month, int year) {
        if (month < 1 || month > 12) {
            final String errMsg = "invalid value for month. Month must be between 1 and 12";
            log.error("{} - invalid month :[{}]", errMsg, month);
            throw new IllegalArgumentException(errMsg);
        }

        // todo: change to EST offset since all deployments are in EST
        OffsetDateTime time = OffsetDateTime.now(ZoneOffset.UTC);
        int currentYear = time.getYear();

        if (year < AB2D_EPOCH_YEAR || year > currentYear) {
            final String errMsg = "invalid value for year. Year must be between " + AB2D_EPOCH_YEAR + " and " + currentYear;
            log.error("{} - invalid year :[{}]", errMsg, year);
            throw new IllegalArgumentException(errMsg);
        }

        OffsetDateTime proposed = OffsetDateTime.of(LocalDate.of(year, month, 1), LocalTime.of(0, 0, 0), ZoneOffset.UTC);
        if (proposed.isAfter(time)) {
            throw new IllegalArgumentException("Pulling coverage period from future. Current time " + time + " is before proposed time " + proposed);
        }
    }

    private CoverageSearchEvent updateStatus(CoveragePeriod period, String description, CoverageJobStatus status) {
        return updateStatus(period, description, status, true);
    }

    /**
     * Update the status of a {@link CoveragePeriod} in the search workflow.
     * @param period the coverage period to update
     * @param description a description of the update
     * @param status the new status for the coverage period and coverage search
     * @param updateCoveragePeriod whether the coverage period needs to be updated as well
     * @return the update coverage search event
     */
    private CoverageSearchEvent updateStatus(CoveragePeriod period, String description, CoverageJobStatus status, boolean updateCoveragePeriod) {
        CoverageSearchEvent currentStatus = getLastEvent(period);

        logStatusChange(period, description, status);

        CoverageJobStatus oldStatus = currentStatus != null ? currentStatus.getNewStatus() : null;

        CoverageSearchEvent newStatus = new CoverageSearchEvent();
        newStatus.setCoveragePeriod(period);
        newStatus.setOldStatus(oldStatus);
        newStatus.setNewStatus(status);
        newStatus.setDescription(description);


        if (status == CoverageJobStatus.SUCCESSFUL) {
            period.setLastSuccessfulJob(OffsetDateTime.now());
        }

        // For successful, failed, or submitted jobs also update the coverage periods status in the DB
        if (updateCoveragePeriod) {
            period.setStatus(status);
            coveragePeriodRepo.saveAndFlush(period);
        }

        return coverageSearchEventRepo.saveAndFlush(newStatus);
    }

    private void logStatusChange(CoveragePeriod period, String description, CoverageJobStatus coverageJobStatus) {
        log.info("Updating job state for search {}-{}-{} from {} to {} due to {}", period.getContractNumber(),
                period.getMonth(), period.getYear(), period.getStatus(), coverageJobStatus, description);
    }

    /**
     * Eagerly load a {@link CoveragePeriod} by id.
     * @param periodId coverage period id to load
     * @return coverage period if found
     * @throws CoveragePeriodNotFoundException if coverage period is not found
     */
    private CoveragePeriod findCoveragePeriod(int periodId) {
        return coveragePeriodRepo.findById(periodId)
                .orElseThrow(() -> new CoveragePeriodNotFoundException("coverage period with id not found"));
    }

    private CoverageSearchEvent findCoverageSearchEvent(long periodId) {
        return coverageSearchEventRepo.findById(periodId)
                .orElseThrow(() -> new CoveragePeriodNotFoundException("coverage period with id not found"));
    }
}
