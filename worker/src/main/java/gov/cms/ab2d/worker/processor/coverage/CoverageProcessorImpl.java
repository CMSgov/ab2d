package gov.cms.ab2d.worker.processor.coverage;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.CoverageMapping;
import gov.cms.ab2d.common.model.CoveragePeriod;
import gov.cms.ab2d.common.model.CoverageSearch;
import gov.cms.ab2d.common.service.ContractService;
import gov.cms.ab2d.common.service.CoverageService;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.common.util.DateUtil;
import gov.cms.ab2d.worker.config.CoverageMappingConfig;
import gov.cms.ab2d.worker.processor.domainmodel.ContractSearchLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static gov.cms.ab2d.common.util.DateUtil.getAB2DEpoch;

@Slf4j
@Service
public class CoverageProcessorImpl implements CoverageProcessor {

    private static final long ONE_SECOND = 1000;
    private static final long SIXTY_SECONDS = 60000;

    private final ContractService contractService;
    private final CoverageService coverageService;
    private final BFDClient bfdClient;
    private final ThreadPoolTaskExecutor executor;
    private final ContractSearchLock searchLock;
    private final PropertiesService propertiesService;

    private final CoverageMappingConfig config;

    private final List<CoverageMappingCallable> inProgressMappings = new ArrayList<>();

    // Queue for results of jobs that have already completed
    private final BlockingQueue<CoverageMapping> coverageInsertionQueue = new LinkedBlockingQueue<>();

    private final AtomicBoolean inShutdown = new AtomicBoolean(false);

    public CoverageProcessorImpl(ContractService contractService, CoverageService coverageService,
                                 PropertiesService propertiesService, BFDClient bfdClient,
                                 @Qualifier("patientCoverageThreadPool") ThreadPoolTaskExecutor executor,
                                 CoverageMappingConfig coverageMappingConfig, ContractSearchLock searchLock) {
        this.contractService = contractService;
        this.coverageService = coverageService;
        this.propertiesService = propertiesService;
        this.bfdClient = bfdClient;
        this.executor = executor;
        this.config = coverageMappingConfig;
        this.searchLock = searchLock;
    }

    /**
     * Find all work that needs to be done including new coverage periods, jobs that have been running too long,
     * and coverage information that is too old.
     *
     * todo annotate as quartz job
     */
    @Override
    public void queueStaleCoveragePeriods() {

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
            queueCoveragePeriod(period, false);
        }

        log.info("queued all stale coverage periods");
    }

    /**
     * Discover any nonexistent coverage periods and add them to the list of coverage periods
     */
    @Override
    public void discoverCoveragePeriods() {

        log.info("discovering all coverage periods that should exist");

        List<Contract> attestedContracts = contractService.getAllAttestedContracts();

        ZonedDateTime now = ZonedDateTime.now();

        ZonedDateTime epoch = getAB2DEpoch();

        for (Contract contract : attestedContracts) {
            ZonedDateTime attestationTime = contract.getESTAttestationTime();

            // Force first coverage period to be after
            // January 1st 2020 which is the first moment we report data for
            if (attestationTime.isBefore(epoch)) {
                log.debug("contract attested before ab2d epoch setting to epoch");
                attestationTime = getAB2DEpoch();
            }

            int coveragePeriodsForContracts = 0;
            while (attestationTime.isBefore(now)) {
                coverageService.getOrCreateCoveragePeriod(contract, attestationTime.getMonthValue(), attestationTime.getYear());
                coveragePeriodsForContracts += 1;

                attestationTime = attestationTime.plusMonths(1);
            }

            log.info("discovered {} coverage periods for contract {}", coveragePeriodsForContracts,
                    contract.getContractName());
        }

        log.info("discovered all coverage periods now exiting");
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
        int monthsInPast = 0;
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

    @Override
    public void queueCoveragePeriod(CoveragePeriod period, boolean prioritize) {
        queueCoveragePeriod(period, 0, prioritize);
    }

    /**
     *
     * @param period period to add for the first time
     * @param prioritize whether to place at front of queue or back
     */
    @Override
    public void queueCoveragePeriod(CoveragePeriod period, int attempts, boolean prioritize) {

        if (inShutdown.get()) {
            return;
        }

        String eventLog = "manually queued coverage period search at " + (prioritize ? "front of queue" : "back of queue");
        log.info(eventLog);
        if (prioritize) {
            coverageService.prioritizeSearch(period.getId(), attempts, eventLog);
        } else {
            coverageService.submitSearch(period.getId(), attempts, eventLog);
        }
    }

    private boolean startJob(CoverageMapping mapping) {

        synchronized (inProgressMappings) {

            if (inShutdown.get()) {
                log.warn("cannot start job because service has been shutdown");
                return false;
            }

            log.debug("starting search for {} during {}-{}", mapping.getContract().getContractNumber(),
                    mapping.getPeriod().getMonth(), mapping.getPeriod().getYear());

            CoverageMappingCallable callable = new CoverageMappingCallable(mapping, bfdClient, false);
            executor.submit(callable);
            inProgressMappings.add(callable);

            return true;
        }
    }

    @Scheduled(fixedDelay = SIXTY_SECONDS, initialDelayString = "${coverage.update.initial.delay}")
    public void loadMappingJob() {

        if (propertiesService.isInMaintenanceMode()) {
            log.debug("waiting to execute queued coverage searches because api is in maintenance mode");
        }

        if (isProcessorBusy()) {
            log.debug("not starting any new coverage mapping jobs because service is full. " +
                    "Currently executing {}. Currently inserting {}", executor.getActiveCount(), coverageInsertionQueue.size());
            return;
        }

        while (!isProcessorBusy()) {
            Optional<CoverageSearch> search = searchLock.getNextSearch();
            if (search.isEmpty()) {
                break;
            }

            Optional<CoverageMapping> maybeSearch = coverageService.startSearch(search.get(), "starting a job");
            if (maybeSearch.isEmpty()) {
                break;
            }

            CoverageMapping mapping = maybeSearch.get();

            log.debug("found a search in queue for contract {} during {}-{}, attempting to search",
                    mapping.getContract().getContractNumber(), mapping.getPeriod().getMonth(),
                    mapping.getPeriod().getYear());

            if (!startJob(mapping)) {
                coverageService.cancelSearch(mapping.getPeriodId(), "failed to start job");
                queueMapping(mapping, false);
            }
        }
    }

    private boolean isProcessorBusy() {
        return coverageInsertionQueue.size() >= executor.getMaxPoolSize() ||
                executor.getActiveCount() >= executor.getMaxPoolSize();
    }

    private void queueMapping(CoverageMapping mapping, boolean b) {
        queueCoveragePeriod(mapping.getPeriod(), mapping.getCoverageSearch().getAttempts(), b);
    }

    @Scheduled(fixedDelay = SIXTY_SECONDS, initialDelayString = "${coverage.update.initial.delay}")
    public void monitorMappingJobs() {

        synchronized (inProgressMappings) {
            if (!inShutdown.get()) {
                Iterator<CoverageMappingCallable> mappingCallableIterator = inProgressMappings.iterator();

                log.debug("Checking running jobs for changes in state");

                while (mappingCallableIterator.hasNext()) {

                    CoverageMappingCallable mappingCallable = mappingCallableIterator.next();

                    if (mappingCallable.isCompleted()) {

                        evaluateJob(mappingCallable.getCoverageMapping());

                        mappingCallableIterator.remove();
                    }
                }
            }
        }
    }

    public void evaluateJob(CoverageMapping mapping) {
        if (mapping.isSuccessful()) {
            log.debug("finished a search for contract {} during {}-{}", mapping.getContract().getContractNumber(),
                    mapping.getPeriod().getMonth(), mapping.getPeriod().getYear());

            coverageInsertionQueue.add(mapping);
        } else if (mapping.getCoverageSearch().getAttempts() > config.getMaxAttempts()) {

            log.error("could not complete coverage mapping job due to multiple failed attempts and will not re-attempt");

            coverageService.failSearch(mapping.getPeriodId(),
                    "could not complete coverage mapping due to error will not re-attempt");
        } else {

            log.warn("could not complete coverage mapping job but will re-attempt");

            coverageService.failSearch(mapping.getPeriodId(),
                    "could not complete coverage mapping due to error");
            queueMapping(mapping, false);
        }
    }

    @Scheduled(fixedDelay = ONE_SECOND, initialDelayString = "${coverage.update.initial.delay}")
    public void insertJobResults() {

        try {
            CoverageMapping result = coverageInsertionQueue.poll(SIXTY_SECONDS, TimeUnit.MILLISECONDS);

            if (result == null) {
                return;
            }

            int periodId = result.getPeriodId();
            long eventId = result.getCoverageSearchEvent().getId();
            String contractNumber = result.getContract().getContractNumber();
            int month = result.getPeriod().getMonth();
            int year = result.getPeriod().getYear();

            if (!inShutdown.get()) {

                log.debug("inserting coverage mapping for contract {} during {}-{}",
                        contractNumber, month, year);

                coverageService.insertCoverage(eventId, result.getBeneficiaryIds());

                coverageService.completeSearch(periodId, "successfully inserted all data for in progress search");
            } else {

                log.debug("shutting down before inserting results for contract {} during {}-{}, will re-attempt",
                        contractNumber, month, year);

                coverageService.failSearch(result.getPeriodId(),
                        "shutting down coverage processor before beneficiary data can be inserted into database");
                queueMapping(result, true);
            }
        } catch (InterruptedException ie) {
            log.debug("polling for data to insert failed due to interruption", ie);
        }
    }

    @PreDestroy
    public void shutdown() {

        log.debug("shutting down coverage processor");

        // Notify all threads that we are shutting down and that work should not progress through pipeline
        synchronized (inProgressMappings) {
            inShutdown.set(true);
        }

        Collection<CoverageMapping> inserting = new ArrayList<>();

        coverageInsertionQueue.drainTo(inserting);
        coverageInsertionQueue.clear();

        for (CoverageMapping insertedMapping : inserting) {
            log.debug("shutting down before inserting for contract {} during {}-{}, will re-attempt",
                    insertedMapping.getContract().getContractNumber(), insertedMapping.getPeriod().getMonth(),
                    insertedMapping.getPeriod().getYear());

            coverageService.failSearch(insertedMapping.getPeriodId(),
                    "shutting down coverage processor before results could be inserted into database");
            queueMapping(insertedMapping, false);
        }

        // Force shutdown all running bfd queries

        synchronized (inProgressMappings) {
            executor.setWaitForTasksToCompleteOnShutdown(false);
            executor.shutdown();

            inProgressMappings.forEach(callable -> {
                CoverageMapping mapping = callable.getCoverageMapping();
                log.debug("shutting down in progress search for contract {} during {}-{}",
                        mapping.getContract().getContractNumber(), mapping.getPeriod().getMonth(),
                        mapping.getPeriod().getYear());

                coverageService.failSearch(mapping.getPeriodId(),
                        "shutting down coverage processor before bfd query completed");
                queueMapping(mapping, false);
            });
        }
    }
}
