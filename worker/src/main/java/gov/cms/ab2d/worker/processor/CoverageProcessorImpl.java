package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.CoverageMapping;
import gov.cms.ab2d.common.model.CoveragePeriod;
import gov.cms.ab2d.common.service.CoverageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PreDestroy;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class CoverageProcessorImpl implements CoverageProcessor {

    private static final long INSERTION_FIXED_DELAY = 30000;

    private final CoverageService coverageService;
    private final BFDClient bfdClient;
    private final ThreadPoolTaskExecutor executor;

    // Number of months into the past to go looking to update
    private final int pastMonthsToUpdate;

    // Number of days without a search before another search is required
    private final int staleDays;

    // Maximum attempts to complete mapping before failure
    private final int maxAttempts;

    private final List<CoverageMappingCallable> inProgressMappings = new ArrayList<>();

    // Queue for results of jobs that have already completed
    private final BlockingQueue<CoverageMapping> coverageInsertionQueue = new LinkedBlockingQueue<>();

    private AtomicBoolean inShutdown = new AtomicBoolean(false);

    public CoverageProcessorImpl(CoverageService coverageService,
                                 BFDClient bfdClient,
                                 @Qualifier("patientCoverageThreadPool") ThreadPoolTaskExecutor executor,
                                 @Value("${coverage.update.months.past}") int pastMonthsToUpdate,
                                 @Value("${coverage.update.stale.days}") int staleDays,
                                 @Value("${coverage.update.max.attempts}") int maxAttempts) {
        this.coverageService = coverageService;
        this.bfdClient = bfdClient;
        this.executor = executor;
        this.pastMonthsToUpdate = pastMonthsToUpdate;
        this.staleDays = staleDays;
        this.maxAttempts = maxAttempts;
    }

    /*
     * 1. Query for all stale coverage periods
     * 2. Insert into new table (has to be created with schema file) which is a list of todo items
     * 3. Delete coverageMappingDeque completely and pull from this list of todo items. Update the mappingLoop to
     *    handle shutdown correctly with this in mind. Marked as submitted
     * 4. Determine whether use of submitted is correct and modify accordingly (see queryCoverageMapping)
     */
    @Override
    public void queueStaleCoveragePeriods() {
        // Use a linked hash set to order by discovery

        Set<CoveragePeriod> stalePeriods = new LinkedHashSet<>(coverageService.findNeverSearched());

        int monthsInPast = 0;
        OffsetDateTime dateTime = OffsetDateTime.now(ZoneId.of("America/New_York"));
        do {
            // Get past month and year
            OffsetDateTime pastMonthTime = dateTime.minusMonths(monthsInPast);
            int month = pastMonthTime.getDayOfMonth();
            int year = pastMonthTime.getDayOfYear();

            // Look for coverage periods that have not been updated
            OffsetDateTime lastUpdatedAfter = dateTime.minusDays(staleDays * (monthsInPast + 1));

            stalePeriods.addAll(coverageService.coverageNotUpdatedSince(month, year, lastUpdatedAfter));
            monthsInPast++;
        } while (monthsInPast < pastMonthsToUpdate);

        for (CoveragePeriod period : stalePeriods) {
            queueCoveragePeriod(period, false);
        }
    }

    @Override
    public void queueCoveragePeriod(CoveragePeriod period, boolean prioritize) {
        this.queueCoveragePeriod(period, 0, prioritize);
    }

    @Override
    public void queueCoveragePeriod(Collection<CoveragePeriod> periods) {
        for (CoveragePeriod period : periods) {
            queueCoveragePeriod(period, false);
        }
    }

    /**
     *
     * @param period period to add for the first time
     * @param prioritize whether to place at front of queue or back
     */
    @Override
    public void queueCoveragePeriod(CoveragePeriod period, int attempts, boolean prioritize) {

        if (!inShutdown.get()) {

            String eventLog = "manually queued coverage period search at " + (prioritize ? "front of queue" : "back of queue");
            log.info(eventLog);
            if (prioritize) {
                coverageService.prioritizeSearch(period.getId(), attempts, eventLog);
            } else {
                coverageService.submitSearch(period.getId(), attempts, eventLog);
            }
        }
    }

    public boolean startJob(CoverageMapping mapping) {
        synchronized (inProgressMappings) {

            if (!inShutdown.get()) {

                log.debug("starting search for {} during {}-{}", mapping.getContract().getContractNumber(),
                        mapping.getPeriod().getMonth(), mapping.getPeriod().getYear());

                CoverageMappingCallable callable = new CoverageMappingCallable(mapping, bfdClient);
                executor.submit(callable);
                inProgressMappings.add(callable);

                return true;
            }
        }

        return false;
    }

    public void mappingLoop() {

        if (coverageInsertionQueue.size() < executor.getMaxPoolSize()
                && executor.getActiveCount() != executor.getMaxPoolSize()) {

            Optional<CoverageMapping> maybeSearch = coverageService.startSearch("starting a job");
            if (maybeSearch.isPresent()) {

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
    }

    private void queueMapping(CoverageMapping mapping, boolean b) {
        queueCoveragePeriod(mapping.getPeriod(), mapping.getCoverageSearch().getAttempts(), b);
    }

    @Scheduled(fixedDelay = 10000)
    public void monitorMapping() {

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
        } else if (mapping.getCoverageSearch().getAttempts() > maxAttempts){

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

    @Scheduled(fixedDelay = INSERTION_FIXED_DELAY)
    public void insertionLoop() {

        Collection<CoverageMapping> results = new ArrayList<>();
        coverageInsertionQueue.drainTo(results);

        for (CoverageMapping result : results) {
            if (!inShutdown.get()) {
                int periodId = result.getPeriodId();
                long eventId = result.getCoverageSearchEvent().getId();

                log.debug("inserting coverage mapping for contract {} during {}-{}",
                        result.getContract().getContractNumber(), result.getPeriod().getMonth(),
                        result.getPeriod().getYear());

                coverageService.insertCoverage(periodId, eventId, result.getBeneficiaryIds());

                coverageService.completeSearch(periodId, "successfully inserted all data for in progress search");
            } else {

                log.debug("shutting down before inserting for contract {} during {}-{}, will re-attempt",
                        result.getContract().getContractNumber(), result.getPeriod().getMonth(),
                        result.getPeriod().getYear());

                coverageService.failSearch(result.getPeriodId(),
                        "shutting down coverage processor before beneficiary data can be inserted into database");
                queueMapping(result, true);
            }
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
