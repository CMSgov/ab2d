package gov.cms.ab2d.worker.processor.coverage;

import com.newrelic.api.agent.Trace;
import gov.cms.ab2d.common.properties.PropertiesService;
import gov.cms.ab2d.common.service.PdpClientService;
import gov.cms.ab2d.common.util.DateUtil;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.contracts.model.ContractDTO;
import gov.cms.ab2d.coverage.model.*;
import gov.cms.ab2d.coverage.repository.CoverageSearchRepository;
import gov.cms.ab2d.coverage.service.CoverageService;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.worker.config.ContractToContractCoverageMapping;
import gov.cms.ab2d.worker.processor.coverage.check.*;
import gov.cms.ab2d.worker.service.coveragesnapshot.CoverageSnapshotService;
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
import static gov.cms.ab2d.common.util.PropertyConstants.*;
import static gov.cms.ab2d.worker.processor.coverage.CoverageUtils.getAttestationTime;
import static gov.cms.ab2d.worker.processor.coverage.CoverageUtils.getEndDateTime;
import static java.util.stream.Collectors.groupingBy;

/**
 * Handle high level actions related to updating and querying enrollment for contracts.
 * <p>
 * The {@link #loadMappingJob()} runs periodically to check whether searches have been submitted. If a search
 * is found, and all preconditions are met for starting the search, this method will attempt to start a single
 * search.
 * <p>
 * This method is the main driver for the {@link CoverageProcessor} and uses database locks to guarantee that two
 * workers do not run the same coverage search.
 * <p>
 * This class is concurrency aware and handles the existence of other worker nodes potentially attempting to queue
 * searches.
 */
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
    private final ContractToContractCoverageMapping mapping;
    private final CoverageSnapshotService coverageSnapshotService;


    //CHECKSTYLE.OFF
    public CoverageDriverImpl(CoverageSearchRepository coverageSearchRepository,
                              PdpClientService pdpClientService,
                              CoverageService coverageService,
                              PropertiesService propertiesService,
                              CoverageProcessor coverageProcessor,
                              CoverageLockWrapper coverageLockWrapper,
                              ContractToContractCoverageMapping mapping,
                              CoverageSnapshotService coverageSnapshotService) {
        this.coverageSearchRepository = coverageSearchRepository;
        this.pdpClientService = pdpClientService;
        this.coverageService = coverageService;
        this.coverageProcessor = coverageProcessor;
        this.coverageLockWrapper = coverageLockWrapper;
        this.propertiesService = propertiesService;
        this.mapping = mapping;
        this.coverageSnapshotService = coverageSnapshotService;
    }
    //CHECKSTYLE.ON


    /**
     * Retrieve configuration for the coverage search from the database.
     * <p>
     * The following parameters are configurable:
     * - How far into the past to update coverage for
     * - Force a coverage override
     * - Fail a search if it has been running too long
     *
     * @return the current meaningful coverage update configuration
     */
    private CoverageUpdateConfig retrieveConfig() {
        String updateMonths = propertiesService.getProperty(COVERAGE_SEARCH_UPDATE_MONTHS, "3");
        String stuckHours = propertiesService.getProperty(COVERAGE_SEARCH_STUCK_HOURS, "24");
        String override = propertiesService.getProperty(COVERAGE_SEARCH_OVERRIDE, "false");

        return new CoverageUpdateConfig(Integer.parseInt(updateMonths), Integer.parseInt(stuckHours), Boolean.parseBoolean(override));
    }

    /**
     * Find all work that needs to be done including new coverage periods, jobs that have been running too long,
     * and coverage information that is too old.
     *
     * @throws InterruptedException                                           if is interrupted by a shutdown
     * @throws gov.cms.ab2d.worker.processor.coverage.CoverageDriverException on failure to acquire lock programmatically
     */
    @Override
    public void queueStaleCoveragePeriods() throws InterruptedException {

        Lock lock = coverageLockWrapper.getCoverageLock();
        boolean locked = false;

        try {
            Set<CoveragePeriod> outOfDateInfo = getCoveragePeriods();

            log.debug("queueing all stale coverage periods");

            /*
             * Guarantee that no other worker node is also trying to start or update queued searches
             */
            locked = lock.tryLock(TEN_MINUTES, TimeUnit.MINUTES);

            if (locked) {

                for (CoveragePeriod period : outOfDateInfo) {
                    log.info("Attempting to add {}-{}-{} to queue", period.getContractNumber(),
                            period.getYear(), period.getMonth());
                }
                //commented out, needs to be moved elsewhere due to do timeout
                //Set<String> contracts = outOfDateInfo.stream().map(CoveragePeriod::getContractNumber).collect(Collectors.toSet());
                //coverageSnapshotService.sendCoverageCounts(AB2DServices.AB2D, contracts);
                for (CoveragePeriod period : outOfDateInfo) {
                    coverageProcessor.queueCoveragePeriod(period, false);
                }

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

    /**
     * Get all coverage periods that need an update.
     * <p>
     * Steps
     * - Find all coverage periods that haven't ever been searched before
     * - Find all stuck coverage updates and
     * - Find all coverage periods that still need to be updated
     *
     * @return list of coverage periods that need to be updated
     */
    private Set<CoveragePeriod> getCoveragePeriods() {
        log.debug("attempting to find all stale coverage periods");

        // Use a linked hash set to order by discovery
        // Add all new coverage periods that have never been mapped/searched
        Set<CoveragePeriod> outOfDateInfo = new LinkedHashSet<>(coverageService.coveragePeriodNeverSearchedSuccessfully());

        // Find all stuck coverage searches and cancel them
        outOfDateInfo.addAll(findAndCancelStuckCoverageJobs());

        // For all months into the past find old coverage searches
        outOfDateInfo.addAll(findStaleCoverageInformation());

        return outOfDateInfo;
    }

    @Override
    public void discoverCoveragePeriods() throws CoverageDriverException, InterruptedException {

        Lock lock = coverageLockWrapper.getCoverageLock();
        boolean locked = false;

        try {

            // We run this job once a day, so we really need to grab this lock
            locked = lock.tryLock(TEN_MINUTES, TimeUnit.MINUTES);

            if (locked) {
                log.debug("discovering all coverage periods that should exist");

                // Iterate through all attested contracts and look for new
                // coverage periods for each contract
                List<Contract> enabledContracts = pdpClientService.getAllEnabledContracts();

                for (Contract contract : enabledContracts) {
                    discoverCoveragePeriods(mapping.map(contract));
                }

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

    private void discoverCoveragePeriods(ContractForCoverageDTO contract) {
        // Assume current time is EST since all AWS deployments are in EST
        ZonedDateTime now = getEndDateTime();
        ZonedDateTime attestationTime = getAttestationTime(contract);

        int coveragePeriodsForContracts = 0;
        while (attestationTime.isBefore(now)) {
            coverageService.getCreateIfAbsentCoveragePeriod(contract, attestationTime.getMonthValue(), attestationTime.getYear());
            coveragePeriodsForContracts += 1;

            attestationTime = attestationTime.plusMonths(1);
        }

        log.debug("discovered {} coverage periods for contract {}", coveragePeriodsForContracts,
                contract.getContractNumber());
    }

    private Set<CoveragePeriod> findAndCancelStuckCoverageJobs() {

        log.debug("attempting to find all stuck coverage searches and then cancel those stuck coverage searches");

        CoverageUpdateConfig config = retrieveConfig();

        Set<CoveragePeriod> stuckJobs = new LinkedHashSet<>(
                coverageService.coveragePeriodStuckJobs(OffsetDateTime.now(ZoneOffset.UTC)
                        .minusHours(config.getStuckHours())));

        for (CoveragePeriod period : stuckJobs) {
            coverageService.failSearch(period.getId(), "coverage period current job has been stuck for at least "
                    + config.getStuckHours() + " hours and is now considered failed.");
        }

        return stuckJobs;
    }

    /**
     * Stale coverage information is defined as coverage data for the past {@link CoverageUpdateConfig#getPastMonthsToUpdate()}
     * that has not been updated during the current week or, in the case of forced updates,
     * update regardless of last updated time.
     * <p>
     * When the coverage override is set all coverage data for the past {@link CoverageUpdateConfig#getPastMonthsToUpdate()}
     * will be returned. Sometimes updates are triggered manually and the cron job which normally find stale coverage information
     * doesn't need to run again.
     * <p>
     * This definition is crafted to match BFD data uploads which occur every weekend. If a job is updated on Monday it
     * does not need to be updated on Tuesday. If a job is updated on Friday it does need to be updated
     * the upcoming Tuesday.
     * <p>
     * Sometimes updates are triggered manually and the cron job which normally find stale coverage information
     * doesn't need to run again.
     *
     * @return set of coverage periods that need to be queued for updates.
     */
    private Set<CoveragePeriod> findStaleCoverageInformation() {

        log.debug("attempting to find all coverage information that is out of date and reduce down to coverage periods");

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

        log.debug("Last Sunday computed as {}", lastSunday);

        /* Check coverage periods up to {@link CoverageUpdateConfig#getPastMonthsToUpdate()} */
        do {
            // Get past month and year
            OffsetDateTime pastMonthTime = dateTime.minusMonths(monthsInPast);
            int month = pastMonthTime.getMonthValue();
            int year = pastMonthTime.getYear();

            /*
             * If override is set, update every qualifying coverage period that is not currently being updated.
             *
             * Otherwise, update only if coverage has not been updated during the current week.
             */
            if (config.isOverride()) {
                // Only add coverage periods that are not running already
                log.info("In override, get all periods");
                List<CoveragePeriod> periods = coverageService.getCoveragePeriods(month, year);
                List<CoveragePeriod> notRunning = periods.stream()
                        .filter(period -> period.getStatus() != CoverageJobStatus.SUBMITTED
                                && period.getStatus() != CoverageJobStatus.IN_PROGRESS)
                        .toList();
                notRunning.forEach(c -> log.info("    Contract: {}, id: {}, last successful {}", c.getContractNumber(), c.getId(), c.getLastSuccessfulJob()));
                stalePeriods.addAll(notRunning);
            } else {
                log.debug("Looking for periods not updated for {}/{} since {}", month, year, lastSunday);
                List<CoveragePeriod> found = coverageService.coveragePeriodNotUpdatedSince(month, year, lastSunday);
                found.forEach(c -> log.debug("    Contract: {}, id: {}, last successful {}", c.getContractNumber(), c.getId(), c.getLastSuccessfulJob()));
                stalePeriods.addAll(found);
            }

            monthsInPast++;
        } while (monthsInPast < config.getPastMonthsToUpdate());

        return stalePeriods;
    }

    /**
     * Queues coverage mapping jobs to run on this machine. Coverage mapping jobs are split
     * between workers so this worker will get a job to start in a thread safe manner.
     * <p>
     * Method checks that worker node is running properly and not too busy before attempting to find a search
     * and start that search.
     * <p>
     * If a coverage job fails to start it is immediately cancelled and queued again silently.
     */
    @Scheduled(cron = "${coverage.update.load.schedule}")
    public void loadMappingJob() {

        if (propertiesService.isToggleOn(MAINTENANCE_MODE, false)) {
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
                mapping.getContractNumber(), mapping.getPeriod()
                        .getMonth(),
                mapping.getPeriod()
                        .getYear());

        /*
         * Start a job, if starting a job fails immediately cancel the job and queue the search again.
         */
        if (!coverageProcessor.startJob(mapping)) {
            coverageService.cancelSearch(mapping.getPeriodId(), "failed to start job");
            coverageProcessor.queueMapping(mapping, false);
        }
    }

    /**
     * This is the most important part of the class. It retrieves the next search in the table
     * assuming that another thread or application is not currently pulling anything from the table.
     * <p>
     * This method locks any modifications to queued searches while it executes.
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

    /**
     * Determine whether database contains all necessary enrollment for a contract and that no updates to that
     * enrollment are currently occurring.
     * <p>
     * Steps
     * - Lock coverage so no other workers can modify coverage while this check is occurring
     * - Create any {@link CoveragePeriod}s that are currently missing
     * - Check the following to determine whether a job can run (return false if any are not met)
     * - Look for whether months have failed to update during earlier attempts
     * - Look for coverage periods that have never been successfully searched and queue them
     * - Look for coverage periods currently being updated
     *
     * @param job job to check for coverage
     * @throws CoverageDriverException if enrollment state violates assumed preconditions or database lock cannot be retrieved
     * @throws InterruptedException    if trying to lock the table is interrupted
     */
    @Trace(metricName = "EnrollmentIsAvailable", dispatcher = true)
    @Override
    public boolean isCoverageAvailable(Job job, ContractDTO contract) throws InterruptedException {

        String contractNumber = job.getContractNumber();
        assert contractNumber.equals(contract.getContractNumber());

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
            discoverCoveragePeriods(mapping.map(contract));

            log.debug("queueing never searched coverage metadata periods for {}", contractNumber);
            /*
             * If any relevant coverage period has never been pulled from BFD successfully then automatically fail the
             * search
             */
            List<CoveragePeriod> neverSearched = coverageService.coveragePeriodNeverSearchedSuccessfully()
                    .stream()
                    .filter(period -> Objects.equals(contract.getContractNumber(), period.getContractNumber()))
                    .toList();
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
            List<CoveragePeriod> periods = coverageService.findAssociatedCoveragePeriods(contract.getContractNumber());

            if (periods.isEmpty()) {
                log.error("There are no existing coverage periods for this job so no metadata exists");
                throw new CoverageDriverException("There are no existing coverage periods for this job so no ");
            }

            return periods.stream()
                    .map(CoveragePeriod::getStatus)
                    .noneMatch(status -> status == null ||
                            status == CoverageJobStatus.IN_PROGRESS || status == CoverageJobStatus.SUBMITTED);
        } catch (InterruptedException interruptedException) {
            log.error("Interrupted attempting to retrieve lock. Cannot confirm coverage metadata is available");
            throw interruptedException;
        } finally {
            if (locked) {
                coverageLock.unlock();
            }
        }
    }

    /**
     * Determine number of beneficiaries enrolled in the contract which should be pulled from the database
     * and queried from BFD.
     * <p>
     * Get all coverage periods associated with a contract and then find all unique beneficiaries between
     * those coverage periods.
     *
     * @throws CoverageDriverException job has no contract which should not be possible
     */
    @Trace(metricName = "EnrollmentCount", dispatcher = true)
    @Override
    public int numberOfBeneficiariesToProcess(Job job, ContractDTO contract) {

        ZonedDateTime time;
        //Centene support
        if (job.getContractNumber().equals("S4802") || job.getContractNumber().equals("Z1001") || job.getContractNumber().equals("S3147"))
            time = job.getSince().atZoneSameInstant(AB2D_ZONE).plusMonths(1);
        else time = getEndDateTime();

        if (contract == null) {
            throw new CoverageDriverException("cannot retrieve metadata for job missing contract");
        }

        ZonedDateTime startDateTime = getStartDateTime(contract);

        List<CoveragePeriod> periodsToReport = new ArrayList<>();
        while (startDateTime.isBefore(time)) {
            CoveragePeriod periodToReport =
                    coverageService.getCoveragePeriod(mapping.map(contract), startDateTime.getMonthValue(), startDateTime.getYear());
            periodsToReport.add(periodToReport);
            startDateTime = startDateTime.plusMonths(1);
        }

        log.debug("counting number of beneficiaries for {} coverage periods for job {}",
                periodsToReport.size(), job.getJobUuid());

        return coverageService.countBeneficiariesByCoveragePeriod(periodsToReport);
    }

    /**
     * Pull an initial page of enrollment from the database with the requisites for the next page.
     *
     * @throws CoverageDriverException if coverage period or some other precondition necessary for paging is missing
     */
    @Trace(metricName = "EnrollmentLoadFromDB", dispatcher = true)
    @Override
    public CoveragePagingResult pageCoverage(Job job, ContractDTO contract) {
        ZonedDateTime now = getEndDateTime();

        if (contract == null) {
            throw new CoverageDriverException("cannot retrieve metadata for job missing contract");
        }

        ZonedDateTime startDateTime = getStartDateTime(contract);

        try {
            // Check that all coverage periods necessary are present before beginning to page
            while (startDateTime.isBefore(now)) {
                // Will throw exception if it doesn't exist
                coverageService.getCoveragePeriod(mapping.map(contract), startDateTime.getMonthValue(), startDateTime.getYear());
                startDateTime = startDateTime.plusMonths(1);
            }

            // Make initial request which returns a result and a request starting at the next cursor
            CoveragePagingRequest request = new CoveragePagingRequest(PAGING_SIZE, null, mapping.map(contract), job.getCreatedAt());

            // Make request for coverage metadata
            return coverageService.pageCoverage(request);
        } catch (Exception exception) {
            log.error("coverage period missing or year,month query incorrect, driver should have resolved earlier");
            throw new CoverageDriverException("coverage driver failing preconditions", exception);
        }
    }

    /**
     * Get first date that an EOB job needs enrollment for. Make sure that the period of time enrollment is
     * available for does not violate the attestation time or the AB2D epoch.
     *
     * @throws CoverageDriverException if somehow start time is in the future like the attestation time being
     *                                 in the future
     */
    ZonedDateTime getStartDateTime(ContractDTO contract) {
        // Attestation time should never be null for a job making it to this point
        ZonedDateTime startDateTime = contract.getAttestedOn()
                .atZoneSameInstant(DateUtil.AB2D_ZONE);


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

    void checkCoveragePeriodValidity(Job job, CoveragePeriod period) {
        if (period.getStatus() == CoverageJobStatus.FAILED &&
                period.getModified()
                        .isAfter(job.getCreatedAt())) {
            throw new CoverageDriverException("attempts to pull coverage information failed too many times, " +
                    "cannot pull coverage");
        }
    }

    /**
     * Retrieve enrollment information for {@link CoveragePagingRequest#getPageSize()} number of beneficiaries
     * where all enrollment records for each patient are aggregated into an {@link gov.cms.ab2d.coverage.model.CoverageSummary}
     *
     * @throws CoverageDriverException if coverage period or some other precondition necessary for paging is missing
     */
    @Override
    public CoveragePagingResult pageCoverage(CoveragePagingRequest request) {
        try {
            return coverageService.pageCoverage(request);
        } catch (Exception exception) {
            log.error("coverage period missing or year,month query incorrect, driver should have resolved earlier");
            throw new CoverageDriverException("coverage driver failing preconditions", exception);
        }
    }

    /**
     * Verify that coverage data cached in the database matches expected business requirements.
     * <p>
     * Steps
     * - List of all contracts that are active contracts
     * - Check whether contracts have a coverage period for every month since the contract
     * attested. If not, log issue and filter out because other checks do not apply.
     * {@link CoveragePeriodsPresentCheck}
     * - Get count of beneficiaries for every month for every contract
     * - Check that there are no {@link CoverageNoDuplicatesCheck}
     * - Check that every month for a contract has some enrollment except
     * for the current month {@link CoveragePresentCheck}
     * - Check that the coverage for a contract and month is from the latest
     * successful search {@link CoverageUpToDateCheck}
     * - Check that the coverage month to month has not changed drastically {@link CoverageStableCheck}
     * - If there are any issues report all of those issues and fail
     *
     * @throws CoverageVerificationException if one or more violations of expected business level behavior are found
     */
    @Override
    public void verifyCoverage() {

        List<String> issues = new ArrayList<>();

        // Only filter contracts that matter
        List<ContractDTO> enabledContracts = pdpClientService.getAllEnabledContracts()
                .stream()
                .filter(contract -> !contract.isTestContract())
                .filter(contract -> contractNotBeingUpdated(issues, contract))
                .map(Contract::toDTO)
                .toList();

        // Don't perform other verification checks if coverage for months is outright missing
        List<ContractDTO> filteredContracts = enabledContracts.stream()
                .filter(new CoveragePeriodsPresentCheck(coverageService, null, issues))
                .toList();

        // Query for counts of beneficiaries for each contract
        Map<String, List<CoverageCount>> coverageCounts = coverageService.countBeneficiariesForContracts(filteredContracts.stream()
                        .map(mapping::map)
                        .toList())
                .stream()
                .collect(groupingBy(CoverageCount::getContractNumber));

        // Use counts to perform other checks and count passing contracts
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

    /**
     * Check that a contract is not currently having its enrollment updated. The verification steps are only valid
     * for contracts not currently being updated
     *
     * @param issues   list of already discovered issues to append new issues to
     * @param contract the contract to check
     * @return true if the contract is not being updated
     */
    private boolean contractNotBeingUpdated(List<String> issues, Contract contract) {
        List<CoveragePeriod> periods = coverageService.findAssociatedCoveragePeriods(contract.getContractNumber());

        boolean contractBeingUpdated = periods.stream()
                .anyMatch(period -> period.getStatus() == CoverageJobStatus.IN_PROGRESS || period.getStatus() == CoverageJobStatus.SUBMITTED);

        if (contractBeingUpdated) {
            issues.add("Contract " + contract.getContractNumber() + " is being updated now so coverage verification will be done later");
            return false;
        }

        return true;
    }
}
