package gov.cms.ab2d.worker.processor.coverage;

import com.newrelic.api.agent.Trace;
import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.CoverageSearchRepository;
import gov.cms.ab2d.common.service.CoverageService;
import gov.cms.ab2d.common.service.PdpClientService;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.util.Constants;
import gov.cms.ab2d.worker.processor.coverage.check.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static gov.cms.ab2d.common.util.DateUtil.AB2D_EPOCH;
import static gov.cms.ab2d.common.util.DateUtil.AB2D_ZONE;
import static gov.cms.ab2d.worker.processor.coverage.CoverageUtils.getAttestationTime;
import static gov.cms.ab2d.worker.processor.coverage.CoverageUtils.getEndDateTime;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@SuppressWarnings("PMD.TooManyStaticImports")
@Slf4j
@Service
public class CoverageDriverImpl implements CoverageDriver {

    private static final long MINUTE = 1;
    private static final long TEN_MINUTES = 10;

    // Number of metadata records to pull from database in one go
    private static final int PAGING_SIZE = 10000;

    private final CoverageSearchRepository coverageSearchRepository;
    private final PdpClientService pdpClientService;
    private final CoverageService coverageService;
    private final CoverageProcessor coverageProcessor;
    private final CoverageLockWrapper coverageLockWrapper;
    private final PropertiesService propertiesService;

    public CoverageDriverImpl(CoverageSearchRepository coverageSearchRepository,
                              PdpClientService pdpClientService, CoverageService coverageService,
                              PropertiesService propertiesService, CoverageProcessor coverageProcessor,
                              CoverageLockWrapper coverageLockWrapper) {
        this.coverageSearchRepository = coverageSearchRepository;
        this.pdpClientService = pdpClientService;
        this.coverageService = coverageService;
        this.coverageProcessor = coverageProcessor;
        this.coverageLockWrapper = coverageLockWrapper;
        this.propertiesService = propertiesService;
    }


    /**
     * Retrieve configuration for the coverage search from the database
     * @return the current meaningful coverage update configuration
     */
    private CoverageUpdateConfig retrieveConfig() {
        String updateMonths = propertiesService.getPropertiesByKey(Constants.COVERAGE_SEARCH_UPDATE_MONTHS).getValue();
        String staleDays = propertiesService.getPropertiesByKey(Constants.COVERAGE_SEARCH_STALE_DAYS).getValue();
        String stuckHours = propertiesService.getPropertiesByKey(Constants.COVERAGE_SEARCH_STUCK_HOURS).getValue();
        String override = propertiesService.getPropertiesByKey(Constants.COVERAGE_SEARCH_OVERRIDE).getValue();

        return new CoverageUpdateConfig(Integer.parseInt(updateMonths), Integer.parseInt(staleDays),
                Integer.parseInt(stuckHours), Boolean.parseBoolean(override));
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

            for (CoveragePeriod period : outOfDateInfo) {
                log.info("Attempting to add {}-{}-{} to queue", period.getContract().getContractNumber(),
                        period.getYear(), period.getMonth());
            }

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
                List<Contract> enabledContracts = pdpClientService.getAllEnabledContracts();
                for (Contract contract : enabledContracts) {
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
        // Assume current time is EST since all AWS deployments are in EST
        ZonedDateTime now = getEndDateTime();
        ZonedDateTime attestationTime = getAttestationTime(contract);

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

        CoverageUpdateConfig config = retrieveConfig();

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

        CoverageUpdateConfig config = retrieveConfig();

        Set<CoveragePeriod> stalePeriods = new LinkedHashSet<>();
        long monthsInPast = 0;
        OffsetDateTime dateTime = OffsetDateTime.now(AB2D_ZONE);

        OffsetDateTime lastSunday;
        if (dateTime.getDayOfWeek() == DayOfWeek.SUNDAY) {
            lastSunday = dateTime.truncatedTo(ChronoUnit.DAYS);

        } else {
            lastSunday = dateTime.with(TemporalAdjusters.previous(DayOfWeek.SUNDAY))
                    .truncatedTo(ChronoUnit.DAYS);
        }

        do {
            // Get past month and year
            OffsetDateTime pastMonthTime = dateTime.minusMonths(monthsInPast);
            int month = pastMonthTime.getMonthValue();
            int year = pastMonthTime.getYear();

            if (config.isOverride()) {
                // Only add coverage periods that are not running already
                List<CoveragePeriod> periods = coverageService.getCoveragePeriods(month, year);
                periods.stream().filter(period -> period.getStatus() != JobStatus.SUBMITTED
                        && period.getStatus() != JobStatus.IN_PROGRESS)
                        .forEach(stalePeriods::add);
            } else {
                stalePeriods.addAll(coverageService.coveragePeriodNotUpdatedSince(month, year, lastSunday));
            }

            monthsInPast++;
        } while (monthsInPast < config.getPastMonthsToUpdate());

        return stalePeriods;
    }

    /**
     * Queues coverage mapping jobs to run on this machine. Coverage mapping jobs are split
     * between workers
     */
    @Scheduled(cron = "${coverage.update.load.schedule}")
    public void loadMappingJob() {

        if (propertiesService.isInMaintenanceMode()) {
            log.info("waiting to execute queued coverage searches because api is in maintenance mode");
            return;
        }

        if (coverageProcessor.isProcessorBusy()) {
            log.info("not starting any new coverage mapping jobs because service is full.");
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

        log.info("found a search in queue for contract {} during {}-{}, attempting to search",
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

    @Trace(metricName = "EnrollmentIsAvailable", dispatcher = true)
    @Override
    public boolean isCoverageAvailable(Job job) throws InterruptedException {

        String contractNumber = job.getContract().getContractNumber();

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

            log.info("queueing never searched coverage metadata periods for {}", contractNumber);
            /*
             * If any relevant coverage period has never been pulled from BFD successfully then automatically fail the
             * search
             */
            List<CoveragePeriod> neverSearched = coverageService.coveragePeriodNeverSearchedSuccessfully().stream()
                    .filter(period -> Objects.equals(job.getContract(), period.getContract())).collect(toList());
            if (!neverSearched.isEmpty()) {
                // Check that we've not submitted and failed these jobs
                neverSearched.forEach(period -> checkCoveragePeriodValidity(job, period));

                // Add all never searched coverage periods to the queue for processing
                neverSearched.forEach(period -> coverageProcessor.queueCoveragePeriod(period, false));
                return false;
            }

            log.info("checking whether any coverage metadata is currently being updated for {}", contractNumber);
            /*
             * If coverage periods are submitted, in progress or null then ignore for the moment.
             *
             * There will always be at least one coverage period returned.
             */
            List<CoveragePeriod> periods = coverageService.findAssociatedCoveragePeriods(job.getContract());

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

    @Trace(metricName = "EnrollmentCount", dispatcher = true)
    @Override
    public int numberOfBeneficiariesToProcess(Job job) {

        ZonedDateTime now = getEndDateTime();

        Contract contract = job.getContract();

        if (contract == null) {
            throw new CoverageDriverException("cannot retrieve metadata for job missing contract");
        }

        ZonedDateTime startDateTime = getStartDateTime(job);

        List<CoveragePeriod> periodsToReport = new ArrayList<>();
        while (startDateTime.isBefore(now)) {
            CoveragePeriod periodToReport =
                    coverageService.getCoveragePeriod(contract, startDateTime.getMonthValue(), startDateTime.getYear());
            periodsToReport.add(periodToReport);
            startDateTime = startDateTime.plusMonths(1);
        }

        log.info("counting number of beneficiaries for {} coverage periods for job {}",
                periodsToReport.size(), job.getJobUuid());

        return coverageService.countBeneficiariesByCoveragePeriod(periodsToReport);
    }

    @Trace(metricName = "EnrollmentLoadFromDB", dispatcher = true)
    @Override
    public CoveragePagingResult pageCoverage(Job job) {
        ZonedDateTime now = getEndDateTime();

        Contract contract = job.getContract();

        if (contract == null) {
            throw new CoverageDriverException("cannot retrieve metadata for job missing contract");
        }

        log.info("attempting to build first page of results for job {}", job.getJobUuid());

        ZonedDateTime startDateTime = getStartDateTime(job);

        try {
            // Check that all coverage periods necessary are present before beginning to page
            while (startDateTime.isBefore(now)) {
                // Will throw exception if it doesn't exist
                coverageService.getCoveragePeriod(contract, startDateTime.getMonthValue(), startDateTime.getYear());
                startDateTime = startDateTime.plusMonths(1);
            }

            // Make initial request which returns a result and a request starting at the next cursor
            CoveragePagingRequest request = new CoveragePagingRequest(PAGING_SIZE, null, contract, job.getCreatedAt());

            // Make request for coverage metadata
            return coverageService.pageCoverage(request);
        } catch (Exception exception) {
            log.error("coverage period missing or year,month query incorrect, driver should have resolved earlier");
            throw new CoverageDriverException("coverage driver failing preconditions", exception);
        }
    }

    private ZonedDateTime getStartDateTime(Job job) {
        Contract contract = job.getContract();

        // Attestation time should never be null for a job making it to this point
        ZonedDateTime startDateTime = contract.getESTAttestationTime();

        // Do not allow in any case for someone to pull data before the AB2D API officially supports.
        // Do not remove this without extreme consideration
        if (startDateTime.isBefore(AB2D_EPOCH)) {
            startDateTime = AB2D_EPOCH;
        }

        ZonedDateTime now = ZonedDateTime.now(AB2D_ZONE);

        if (startDateTime.isAfter(now)) {
            throw new CoverageDriverException("contract attestation time is after current time," +
                    " cannot find metadata for coverage periods in the future");
        }

        return ZonedDateTime.of(startDateTime.getYear(), startDateTime.getMonthValue(),
                1, 0, 0, 0, 0, AB2D_ZONE);
    }

    private void checkCoveragePeriodValidity(Job job, CoveragePeriod period) {
        if (period.getStatus() == JobStatus.FAILED &&
                period.getModified().isAfter(job.getCreatedAt())) {
            throw new CoverageDriverException("attempts to pull coverage information failed too many times, " +
                    "cannot pull coverage");
        }
    }

    @Override
    public CoveragePagingResult pageCoverage(CoveragePagingRequest request) {
        return coverageService.pageCoverage(request);
    }

    @Override
    public void verifyCoverage() {

        List<String> issues = new ArrayList<>();

        List<Contract> enabledContracts = pdpClientService.getAllEnabledContracts();

        List<Contract> filteredContracts = enabledContracts.stream()
                .filter(new CoveragePeriodsPresentCheck(coverageService, null, issues))
                .collect(toList());

        Map<String, List<CoverageCount>> coverageCounts = coverageService.countBeneficiariesForContracts(filteredContracts)
                .stream().collect(groupingBy(CoverageCount::getContractNumber));

        long passingContracts = filteredContracts.stream()
                .filter(new CoverageNoDuplicatesCheck(coverageService, coverageCounts, issues))
                .filter(new CoveragePresentCheck(coverageService, coverageCounts, issues))
                .filter(new CoverageUpToDateCheck(coverageService, coverageCounts, issues))
                .filter(new CoverageStableCheck(coverageService, coverageCounts, issues))
                .count();

        String message = String.format("Verified that %d contracts pass all coverage checks out of %d",
                passingContracts, enabledContracts.size());
        log.info(message);

        if (!issues.isEmpty()) {
            throw new CoverageVerificationException(message, issues);
        }
    }
}
