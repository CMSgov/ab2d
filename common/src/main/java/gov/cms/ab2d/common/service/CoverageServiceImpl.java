package gov.cms.ab2d.common.service;

import com.newrelic.api.agent.Trace;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.CoverageMapping;
import gov.cms.ab2d.common.model.CoveragePagingRequest;
import gov.cms.ab2d.common.model.CoveragePagingResult;
import gov.cms.ab2d.common.model.CoveragePeriod;
import gov.cms.ab2d.common.model.CoverageSearch;
import gov.cms.ab2d.common.model.CoverageSearchDiff;
import gov.cms.ab2d.common.model.CoverageSearchEvent;
import gov.cms.ab2d.common.model.Identifiers;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.repository.*;      // NOPMD
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.time.*;
import java.util.*;

import static gov.cms.ab2d.common.util.DateUtil.AB2D_EPOCH_YEAR;
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
@AllArgsConstructor
@Slf4j
@Service
@Transactional
@SuppressWarnings("PMD.UnusedImports")
public class CoverageServiceImpl implements CoverageService {


    private final CoveragePeriodRepository coveragePeriodRepo;

    private final CoverageSearchEventRepository coverageSearchEventRepo;

    private final CoverageSearchRepository coverageSearchRepo;

    private final CoverageServiceRepository coverageServiceRepo;

    private final CoverageDeltaRepository coverageDeltaRepository;

    @Override
    public CoveragePeriod getCoveragePeriod(Contract contract, int month, int year) {
        checkMonthAndYear(month, year);

        Optional<CoveragePeriod> period = coveragePeriodRepo.findByContractIdAndMonthAndYear(contract.getId(), month, year);
        return period.orElseThrow(() ->
                new EntityNotFoundException("could not find coverage period matching contract, month, and year"));
    }

    @Override
    public List<CoveragePeriod> getCoveragePeriods(int month, int year) {
        checkMonthAndYear(month, year);

        return coveragePeriodRepo.findAllByMonthAndYear(month, year);
    }

    @Override
    public CoveragePeriod getCreateIfAbsentCoveragePeriod(Contract contract, int month, int year) {
        checkMonthAndYear(month, year);

        Optional<CoveragePeriod> existing = coveragePeriodRepo.findByContractIdAndMonthAndYear(contract.getId(), month, year);

        if (existing.isPresent()) {
            return existing.get();
        }

        CoveragePeriod period = new CoveragePeriod();
        period.setContract(contract);
        period.setMonth(month);
        period.setYear(year);

        return coveragePeriodRepo.save(period);
    }

    @Override
    public List<CoveragePeriod> findAssociatedCoveragePeriods(Contract contract) {
        return coveragePeriodRepo.findAllByContractId(contract.getId());
    }

    @Override
    public boolean isCoveragePeriodInProgress(int periodId) {
        CoveragePeriod period = findCoveragePeriod(periodId);
        JobStatus jobStatus = period.getStatus();
        return jobStatus == JobStatus.IN_PROGRESS;
    }

    @Override
    public int countBeneficiariesByCoveragePeriod(List<CoveragePeriod> coveragePeriods) {
        List<Integer> ids = coveragePeriods.stream().map(CoveragePeriod::getId).collect(toList());
        return coverageServiceRepo.countBeneficiariesByPeriods(ids);
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
    public void deletePreviousSearch(int periodId) {
        // Delete previous in progress search before this one
        // but check that coverage period is valid
        CoveragePeriod period = findCoveragePeriod(periodId);
        coverageServiceRepo.deletePreviousSearch(period, 1);
    }

    @Override
    @Trace
    public CoveragePagingResult pageCoverage(CoveragePagingRequest pagingRequest) {

        List<Integer> coveragePeriodIds = pagingRequest.getCoveragePeriodIds();
        if (coveragePeriodIds.isEmpty()) {
            log.error("cannot page coverage if no coverage period ids provided");
            throw new IllegalArgumentException("must provide more than one period id");
        }

        CoveragePeriod coveragePeriod = findCoveragePeriod(coveragePeriodIds.get(0));
        return coverageServiceRepo.pageCoverage(coveragePeriod.getContract(), pagingRequest);
    }

    @Override
    @Trace(metricName = "SearchDiff", dispatcher = true)
    public CoverageSearchDiff searchDiff(int periodId) {

        CoveragePeriod period = findCoveragePeriod(periodId);

        if (period.getStatus() != JobStatus.IN_PROGRESS) {
            throw new InvalidJobStateTransition("Cannot diff a currently running search against previous search because results may be added");
        }

        Optional<CoverageSearchEvent> previousSearch = coverageSearchEventRepo.findSearchEventWithOffset(periodId, JobStatus.IN_PROGRESS.name(), 1);
        Optional<CoverageSearchEvent> currentSearch = coverageSearchEventRepo.findSearchEventWithOffset(periodId, JobStatus.IN_PROGRESS.name(), 0);
        CoverageSearchEvent current = currentSearch.orElseThrow(() -> new RuntimeException("could not find latest in progress search event"));

        int previousCount = 0;
        if (previousSearch.isPresent()) {
            previousCount = coverageServiceRepo.countBySearchEvent(previousSearch.get());
        }

        int currentCount = coverageServiceRepo.countBySearchEvent(current);

        int unchanged = 0;
        if (previousCount > 0) {
            log.info("Calculating the deltas for the search period {}-{}-{}", period.getContract().getContractNumber(),
                    period.getMonth(), period.getYear());
            coverageDeltaRepository.trackDeltas(previousSearch.get(), current);
            unchanged = coverageServiceRepo.countIntersection(previousSearch.get(), current);
        }

        return new CoverageSearchDiff(period, previousCount, currentCount, unchanged);
    }

    @Override
    public List<CoveragePeriod> coveragePeriodNeverSearchedSuccessfully() {

        log.info("attempting to find all never successfully searched coverage periods");

        List<CoveragePeriod> neverSuccessful = coveragePeriodRepo.findAllByLastSuccessfulJobIsNull();

        return neverSuccessful.stream().filter(period ->
                period.getStatus() != JobStatus.SUBMITTED && period.getStatus() != JobStatus.IN_PROGRESS)
                .collect(toList());
    }

    @Override
    public List<CoveragePeriod> coveragePeriodNotUpdatedSince(int month, int year, OffsetDateTime lastSuccessful) {
        return coveragePeriodRepo.findAllByMonthAndYearAndLastSuccessfulJobLessThanEqual(month, year, lastSuccessful);
    }

    @Override
    public List<CoveragePeriod> coveragePeriodStuckJobs(OffsetDateTime startedBefore) {

        log.info("attempting to find all coverage searches that have been in progress for too long");

        List<CoverageSearchEvent> events = coverageSearchEventRepo
                .findStuckAtStatus(JobStatus.IN_PROGRESS.name(), startedBefore);

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
        JobStatus jobStatus = period.getStatus();

        if (jobStatus == JobStatus.IN_PROGRESS || jobStatus == JobStatus.SUBMITTED) {
            return Optional.empty();
        }

        // Add to queue of jobs to do
        CoverageSearch search = new CoverageSearch();
        search.setPeriod(period);
        search.setAttempts(attempts);
        coverageSearchRepo.saveAndFlush(search);

        return Optional.of(updateStatus(period, description, JobStatus.SUBMITTED));
    }

    @Override
    public CoverageSearchEvent resubmitSearch(int periodId, int attempts, String failedDescription,
                                              String restartDescription, boolean prioritize) {
        CoveragePeriod period = findCoveragePeriod(periodId);

        updateStatus(period, failedDescription, JobStatus.FAILED, false);

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

        return updateStatus(period, restartDescription, JobStatus.SUBMITTED);
    }

    @Override
    public Optional<CoverageSearchEvent> prioritizeSearch(int periodId, String description) {
        return prioritizeSearch(periodId, 0, description);
    }

    @Override
    public Optional<CoverageSearchEvent> prioritizeSearch(int periodId, int attempts, String description) {

        CoveragePeriod period = findCoveragePeriod(periodId);
        JobStatus jobStatus = period.getStatus();

        if (jobStatus == JobStatus.IN_PROGRESS || jobStatus == JobStatus.SUBMITTED) {
            return Optional.empty();
        }

        // Add to queue of jobs to do
        CoverageSearch search = new CoverageSearch();
        search.setPeriod(period);
        search.setAttempts(attempts);
        search.setCreated(OffsetDateTime.of(2000, 1, 1,
                0, 0, 0, 0, ZoneOffset.UTC));
        coverageSearchRepo.saveAndFlush(search);

        return Optional.of(updateStatus(period, description, JobStatus.SUBMITTED));
    }

    @Override
    public Optional<CoverageMapping> startSearch(CoverageSearch submittedSearch, String description) {

        if (submittedSearch == null) {
            return Optional.empty();
        }

        CoveragePeriod period = submittedSearch.getPeriod();

        CoverageSearchEvent coverageSearchEvent = updateStatus(period, description, JobStatus.IN_PROGRESS);

        return Optional.of(new CoverageMapping(coverageSearchEvent, submittedSearch));
    }

    @Override
    public CoverageSearchEvent cancelSearch(int periodId, String description) {

        CoveragePeriod period = findCoveragePeriod(periodId);
        JobStatus jobStatus = period.getStatus();

        if (jobStatus != JobStatus.SUBMITTED) {
            throw new InvalidJobStateTransition("cannot change from " + jobStatus
                    + " to " + JobStatus.CANCELLED);
        }

        coverageSearchRepo.deleteCoverageSearchByPeriod(period);

        return updateStatus(period, description, JobStatus.CANCELLED);
    }

    @Override
    public CoverageSearchEvent failSearch(int periodId, String description) {

        CoveragePeriod period = findCoveragePeriod(periodId);
        JobStatus jobStatus = period.getStatus();

        if (jobStatus != JobStatus.IN_PROGRESS) {
            throw new InvalidJobStateTransition("cannot change from " + jobStatus
                    + " to " + JobStatus.FAILED);
        }

        // Delete all results from current search that is failing
        coverageServiceRepo.deletePreviousSearch(period, 0);

        return updateStatus(period, description, JobStatus.FAILED);
    }

    @Override
    public CoverageSearchEvent completeSearch(int periodId, String description) {

        CoveragePeriod period = findCoveragePeriod(periodId);
        JobStatus jobStatus = period.getStatus();

        if (jobStatus != JobStatus.IN_PROGRESS) {
            throw new InvalidJobStateTransition("cannot change from " + jobStatus
                    + " to " + JobStatus.SUCCESSFUL);
        }

        // todo: log to kinesis as well
        Contract contract = period.getContract();
        CoverageSearchDiff diff = searchDiff(periodId);
        log.info("{}-{}-{} difference between previous metadata and current metadata\n {}",
                contract.getContractNumber(), period.getYear(), period.getMonth(), diff);

        deletePreviousSearch(periodId);

        return updateStatus(period, description, JobStatus.SUCCESSFUL);
    }

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

    private CoverageSearchEvent updateStatus(CoveragePeriod period, String description, JobStatus status) {
        return updateStatus(period, description, status, true);
    }

    private CoverageSearchEvent updateStatus(CoveragePeriod period, String description, JobStatus status, boolean updateCoveragePeriod) {
        CoverageSearchEvent currentStatus = getLastEvent(period);

        logStatusChange(period, description, status);

        JobStatus oldStatus = currentStatus != null ? currentStatus.getNewStatus() : null;

        CoverageSearchEvent newStatus = new CoverageSearchEvent();
        newStatus.setCoveragePeriod(period);
        newStatus.setOldStatus(oldStatus);
        newStatus.setNewStatus(status);
        newStatus.setDescription(description);


        if (status == JobStatus.SUCCESSFUL) {
            period.setLastSuccessfulJob(OffsetDateTime.now());
        }

        if (updateCoveragePeriod) {
            period.setStatus(status);
            coveragePeriodRepo.saveAndFlush(period);
        }

        return coverageSearchEventRepo.saveAndFlush(newStatus);
    }

    private void logStatusChange(CoveragePeriod period, String description, JobStatus jobStatus) {
        log.info("Updating job state for search {}-{}-{} from {} to {} due to {}", period.getContract().getContractNumber(),
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
