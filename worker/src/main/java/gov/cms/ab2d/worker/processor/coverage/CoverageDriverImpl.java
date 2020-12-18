package gov.cms.ab2d.worker.processor.coverage;

import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.CoverageSearchRepository;
import gov.cms.ab2d.common.service.ContractService;
import gov.cms.ab2d.common.service.CoverageService;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.util.DateUtil;
import gov.cms.ab2d.worker.config.CoverageUpdateConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static gov.cms.ab2d.common.util.DateUtil.AB2D_EPOCH;
import static java.util.stream.Collectors.toList;

@Slf4j
@Service
public class CoverageDriverImpl implements CoverageDriver {

    private static final long SIXTY_SECONDS_IN_MILLIS = 60000;
    private static final long MINUTE = 1;
    private static final long TEN_MINUTES = 1;

    private final CoverageSearchRepository coverageSearchRepository;
    private final ContractService contractService;
    private final CoverageService coverageService;
    private final CoverageProcessor coverageProcessor;
    private final CoverageUpdateConfig config;
    private final CoverageLockWrapper coverageLockWrapper;
    private final PropertiesService propertiesService;

    public CoverageDriverImpl(CoverageSearchRepository coverageSearchRepository,
                              ContractService contractService, CoverageService coverageService,
                              PropertiesService propertiesService, CoverageProcessor coverageProcessor,
                              CoverageUpdateConfig coverageUpdateConfig, CoverageLockWrapper coverageLockWrapper) {
        this.coverageSearchRepository = coverageSearchRepository;
        this.contractService = contractService;
        this.coverageService = coverageService;
        this.coverageProcessor = coverageProcessor;
        this.config = coverageUpdateConfig;
        this.coverageLockWrapper = coverageLockWrapper;
        this.propertiesService = propertiesService;
    }

    /**
     * Find all work that needs to be done including new coverage periods, jobs that have been running too long,
     * and coverage information that is too old.
     */
    @Override
    public void queueStaleCoveragePeriods() throws InterruptedException {

        Lock lock = coverageLockWrapper.getCoverageLock();
        boolean locked = false;

        try {

            Set<CoveragePeriod> outOfDateInfo = getCoveragePeriods();

            log.info("queueing all stale coverage periods");

            // Job runs once a day so we need to grab this lock
            locked = lock.tryLock(TEN_MINUTES, TimeUnit.MINUTES);

            if (locked) {
                for (CoveragePeriod period : outOfDateInfo) {
                    coverageProcessor.queueCoveragePeriod(period, false);
                }

                log.info("queued all stale coverage periods");
            } else {
                throw new CoverageDriverException("could not retrieve lock to update stale coverage periods");
            }

        } catch (InterruptedException interruptedException) {
            log.error("locking interrupted so stale coverage periods could not be updated");
            throw interruptedException;
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    private Set<CoveragePeriod> getCoveragePeriods() {
        log.info("attempting to find all stale coverage periods");

        // Use a linked hash set to order by discovery
        // Add all new coverage periods that have never been mapped/searched
        Set<CoveragePeriod> outOfDateInfo = new LinkedHashSet<>(coverageService.coveragePeriodNeverSearchedSuccessfully());

        // Find all stuck coverage searches and cancel them
        outOfDateInfo.addAll(findAndCancelStuckCoverageJobs());

        // For all months into the past find old coverage searches
        outOfDateInfo.addAll(findStaleCoverageInformation());

        log.info("queueing all stale coverage periods");

        for (CoveragePeriod period : outOfDateInfo) {
            coverageProcessor.queueCoveragePeriod(period, false);
        }

        log.info("queued all stale coverage periods");
        return outOfDateInfo;
    }

    /**
     * Discover any nonexistent coverage periods and add them to the list of coverage periods
     */
    @Override
    public void discoverCoveragePeriods() throws CoverageDriverException, InterruptedException {

        Lock lock = coverageLockWrapper.getCoverageLock();
        boolean locked = false;

        try {

            // We run this job once a day so we really need to grab this lock
            locked = lock.tryLock(TEN_MINUTES, TimeUnit.MINUTES);

            if (locked) {
                log.info("discovering all coverage periods that should exist");

                // Iterate through all attested contracts and look for new
                // coverage periods for each contract
                List<Contract> attestedContracts = contractService.getAllAttestedContracts();
                for (Contract contract : attestedContracts) {
                    discoverCoveragePeriods(contract);
                }

                log.info("discovered all coverage periods now exiting");
            } else {
                throw new CoverageDriverException("could not retrieve lock to discover new coverage periods");
            }

        } catch (InterruptedException interruptedException) {
            log.error("locking interrupted so stale coverage periods could not be updated");
            throw interruptedException;
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    private void discoverCoveragePeriods(Contract contract) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime attestationTime = contract.getESTAttestationTime();

        // Force first coverage period to be after
        // January 1st 2020 which is the first moment we report data for
        if (attestationTime.isBefore(AB2D_EPOCH)) {
            log.debug("contract attested before ab2d epoch setting to epoch");
            attestationTime = AB2D_EPOCH;
        }

        int coveragePeriodsForContracts = 0;
        while (attestationTime.isBefore(now)) {
            coverageService.getCreateIfAbsentCoveragePeriod(contract, attestationTime.getMonthValue(), attestationTime.getYear());
            coveragePeriodsForContracts += 1;

            attestationTime = attestationTime.plusMonths(1);
        }

        log.info("discovered {} coverage periods for contract {}", coveragePeriodsForContracts,
                contract.getContractName());
    }

    private Set<CoveragePeriod> findAndCancelStuckCoverageJobs() {

        log.info("attempting to find all stuck coverage searches and then cancel those stuck coverage searches");

        Set<CoveragePeriod> stuckJobs = new LinkedHashSet<>(
                coverageService.coveragePeriodStuckJobs(OffsetDateTime.now(ZoneOffset.UTC).minusHours(config.getStuckHours())));

        for (CoveragePeriod period : stuckJobs) {
            coverageService.failSearch(period.getId(), "coverage period current job has been stuck for at least "
                    + config.getStuckHours() + " hours and is now considered failed.");
        }

        return stuckJobs;
    }

    private Set<CoveragePeriod> findStaleCoverageInformation() {

        log.info("attempting to find all coverage information that is out of date and reduce down to coverage periods");

        Set<CoveragePeriod> stalePeriods = new LinkedHashSet<>();
        long monthsInPast = 0;
        OffsetDateTime dateTime = OffsetDateTime.now(DateUtil.AB2D_ZONE);

        do {
            // Get past month and year
            OffsetDateTime pastMonthTime = dateTime.minusMonths(monthsInPast);
            int month = pastMonthTime.getMonthValue();
            int year = pastMonthTime.getYear();

            // Look for coverage periods that have not been updated
            long daysInPast = config.getStaleDays() * (monthsInPast + 1);
            OffsetDateTime lastUpdatedAfter = dateTime.minusDays(daysInPast);

            stalePeriods.addAll(coverageService.coveragePeriodNotUpdatedSince(month, year, lastUpdatedAfter));
            monthsInPast++;
        } while (monthsInPast < config.getPastMonthsToUpdate());

        return stalePeriods;
    }

    /**
     * Queues coverage mapping jobs to run on this machine. Coverage mapping jobs are split
     * between workers
     */
    @Scheduled(fixedDelay = SIXTY_SECONDS_IN_MILLIS, initialDelayString = "${coverage.update.initial.delay}")
    public void loadMappingJob() {

        if (propertiesService.isInMaintenanceMode()) {
            log.debug("waiting to execute queued coverage searches because api is in maintenance mode");
            return;
        }

        if (coverageProcessor.isProcessorBusy()) {
            log.debug("not starting any new coverage mapping jobs because service is full.");
            return;
        }

        Optional<CoverageSearch> search = getNextSearch();
        if (search.isEmpty()) {
            return;
        }

        Optional<CoverageMapping> maybeSearch = coverageService.startSearch(search.get(), "starting a job");
        if (maybeSearch.isEmpty()) {
            return;
        }

        CoverageMapping mapping = maybeSearch.get();

        log.debug("found a search in queue for contract {} during {}-{}, attempting to search",
                mapping.getContract().getContractNumber(), mapping.getPeriod().getMonth(),
                mapping.getPeriod().getYear());

        if (!coverageProcessor.startJob(mapping)) {
            coverageService.cancelSearch(mapping.getPeriodId(), "failed to start job");
            coverageProcessor.queueMapping(mapping, false);
        }
    }

    /**
     * This is the most important part of the class. It retrieves the next search in the table
     * assuming that another thread or application is not currently pulling anything from the table.
     * If there are no jobs to pull or the table is locked, it returns an empty optional.
     *
     * @return the next search or else an empty Optional if there are none or if the table is locked
     */
    public Optional<CoverageSearch> getNextSearch() {
        Lock lock = coverageLockWrapper.getCoverageLock();

        if (!lock.tryLock()) {
            return Optional.empty();
        }

        try {

            // First find if a submitted eob job is waiting on a current search
            // and pick those searches first
            Optional<CoverageSearch> searchOpt = coverageSearchRepository.findHighestPrioritySearch();

            // If no high priority search has been found
            // instead pick the first submitted search
            if (searchOpt.isEmpty()) {
                searchOpt = coverageSearchRepository.findFirstByOrderByCreatedAsc();
            }

            // If no search found just return empty
            if (searchOpt.isEmpty()) {
                return searchOpt;
            }

            CoverageSearch search = searchOpt.get();
            coverageSearchRepository.delete(search);
            coverageSearchRepository.flush();

            search.setId(null);
            return Optional.of(search);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isCoverageAvailable(Job job) throws InterruptedException {

        Lock coverageLock = coverageLockWrapper.getCoverageLock();

        // Track whether locked or not to prevent an illegal monitor exception
        boolean locked = false;

        try {
            locked = coverageLock.tryLock(MINUTE, TimeUnit.MINUTES);

            if (!locked) {
                log.warn("Could not retrieve lock after timeout of {} minute(s)." +
                        " Cannot confirm coverage metadata is available", MINUTE);
                return false;
            }

            // Check whether a coverage period is missing for this contract.
            // If so then create those coverage periods.
            discoverCoveragePeriods(job.getContract());

            /*
             * If any relevant coverage period has never been pulled from BFD successfully then automatically fail the
             * search
             */
            List<CoveragePeriod> neverSearched = coverageService.coveragePeriodNeverSearchedSuccessfully().stream()
                    .filter(period -> Objects.equals(job.getContract(), period.getContract())).collect(toList());
            neverSearched = filterBySince(job, neverSearched);
            if (!neverSearched.isEmpty()) {
                // Add all never searched coverage periods to the queue for processing
                neverSearched.forEach(period -> coverageProcessor.queueCoveragePeriod(period, false));
                return false;
            }

            /*
             * If coverage periods are submitted, in progress or null then ignore for the moment.
             */
            List<CoveragePeriod> periods = coverageService.findAssociatedCoveragePeriods(job.getContract());
            periods = filterBySince(job, periods);

            if (periods.isEmpty()) {
                log.error("There are no existing coverage periods for this job so no metadata exists");
                throw new CoverageDriverException("There are no existing coverage periods for this job so no ");
            }

            return periods.stream().map(CoveragePeriod::getStatus).noneMatch(status -> status == null ||
                    status == JobStatus.IN_PROGRESS || status == JobStatus.SUBMITTED);
        } catch (InterruptedException interruptedException) {
            log.error("Interrupted attempting to retrieve lock. Cannot confirm coverage metadata is available");
            throw interruptedException;
        } finally {
            if (locked) {
                coverageLock.unlock();
            }
        }
    }

    private List<CoveragePeriod> filterBySince(Job job, List<CoveragePeriod> periods) {
        if (job.getSince() != null) {
            LocalDate sinceExactDay = job.getSince().toLocalDate();
            LocalDate sinceMonth = LocalDate.of(sinceExactDay.getYear(), sinceExactDay.getMonth(), 1);

            periods = periods.stream().filter(period -> {
                LocalDate periodMonth = LocalDate.of(period.getYear(), period.getMonth(), 1);
                return !periodMonth.isBefore(sinceMonth);
            }).collect(toList());
        }
        return periods;
    }
}
