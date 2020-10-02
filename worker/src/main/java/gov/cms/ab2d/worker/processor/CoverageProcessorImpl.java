package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.CoverageMapping;
import gov.cms.ab2d.common.model.CoveragePeriod;
import gov.cms.ab2d.common.model.CoverageSearchEvent;
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
        this.queueCoveragePeriod(period, prioritize);
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

            Optional<CoverageSearchEvent> lastEvent = coverageService.findLastEvent(period.getId());

            String eventLog = "manually queued coverage period search at " + (prioritize ? "front of queue" : "back of queue");
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
                if (!startJob(mapping)) {
                    coverageService.cancelSearch(mapping.getPeriodId(), "failed to start job");
                }
            }
        }
    }

    @Scheduled(fixedDelay = 10000)
    public void monitorMapping() {

        synchronized (inProgressMappings) {
            if (!inShutdown.get()) {
                Iterator<CoverageMappingCallable> mappingCallableIterator = inProgressMappings.iterator();

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
            coverageInsertionQueue.add(mapping);
        } else if (mapping.getCoverageSearch().getAttempts() > maxAttempts){

            log.error("could not complete coverage mapping job due to multiple failed attempts");

            coverageService.failSearch(mapping.getPeriodId(),
                    "could not complete coverage mapping due to error");
        } else {
            coverageService.failSearch(mapping.getPeriodId(),
                    "could not complete coverage mapping due to error");
            queueCoveragePeriod(mapping.getPeriod(), mapping.getCoverageSearch().getAttempts(), false);
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

                coverageService.insertCoverage(periodId, eventId, result.getBeneficiaryIds());

                coverageService.completeSearch(periodId, "successfully inserted all data for in progress search");
            } else {
                coverageService.failSearch(result.getPeriodId(),
                        "shutting down coverage processor before beneficiary data can be inserted into database");
            }
        }

    }

    @PreDestroy
    public void shutdown() {

        // Notify all threads that we are shutting down and that work should not progress through pipeline
        synchronized (inProgressMappings) {
            inShutdown.set(true);
        }

        Collection<CoverageMapping> inserting = new ArrayList<>();

        coverageInsertionQueue.drainTo(inserting);
        coverageInsertionQueue.clear();

        for (CoverageMapping insertedMapping : inserting) {
            coverageService.cancelSearch(insertedMapping.getPeriodId(),
                    "shutting down coverage processor before results could be inserted into database");
        }

        // Force shutdown all running bfd queries

        synchronized (inProgressMappings) {
            executor.setWaitForTasksToCompleteOnShutdown(false);
            executor.shutdown();

            inProgressMappings.forEach(callable -> {
                CoverageMapping mapping = callable.getCoverageMapping();
                coverageService.failSearch(mapping.getPeriodId(),
                        "shutting down coverage processor before bfd query completed");
            });
        }
    }
}
