package gov.cms.ab2d.worker.processor.coverage;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.coverage.model.CoverageMapping;
import gov.cms.ab2d.coverage.model.CoveragePeriod;
import gov.cms.ab2d.coverage.model.CoverageSearch;
import gov.cms.ab2d.coverage.service.CoverageService;
import gov.cms.ab2d.eventlogger.Ab2dEnvironment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;


import static gov.cms.ab2d.fhir.FhirVersion.STU3;

/**
 * Implements the nuts and bolts of updating enrollment by pulling from BFD.
 *
 * This class is driven by spring annotations which let several methods in this class run repeatedly every
 * x seconds.
 *
 * The following methods are run using the {@link Scheduled} annotation
 *
 *      - {@link #monitorMappingJobs()} which monitors all currently running coverage searches against BFD for completion
 *      - {@link #insertJobResults()} which waits for coverage searches against BFD to finish and then attempts
 *          to insert the results for each search one at a time.
 *
 * This class consists of several concurrently running tasks that share resources. These resources are protected
 * by synchronizing specific objects within the class.
 *
 * {@link #inProgressMappings} is synchronized when starting and monitoring jobs
 * {@link #coverageInsertionQueue} is synchronized when attempting to insert results into the database and also adding
 * elements to that queue
 * {@link #inShutdown} is Atomic and is used to attempt a clean shutdown marking searches as failed and restarted
 *
 * Main responsibilities and where they are implemented
 *      - Queue individual coverage periods for search
 *          - {@link #queueCoveragePeriod(CoveragePeriod, boolean)}, {@link #queueCoveragePeriod(CoveragePeriod, int, boolean)},
 *      - Start an enrollment search against BFD {@link #startJob(CoverageMapping)}
 *      - Monitor an enrollment search currently running and
 *          handle completion, failure, or shutdown {@link #monitorMappingJobs()}
 *      - Insert results from a successful enrollment search into the database and cleanly handle shutdown or failure
 *
 * Methods for changing the status of a search.
 *      - {@link gov.cms.ab2d.coverage.service.CoverageService#submitSearch(int, String)}
 *      - {@link gov.cms.ab2d.coverage.service.CoverageService#resubmitSearch(int, int, String, String, boolean)}
 *      - {@link CoverageService#startSearch(CoverageSearch, String)}
 *      - {@link gov.cms.ab2d.coverage.service.CoverageService#completeSearch(int, String)}
 *      - {@link gov.cms.ab2d.coverage.service.CoverageService#failSearch(int, String)}
 */
@Slf4j
@Service
public class CoverageProcessorImpl implements CoverageProcessor {

    private static final long ONE_SECOND = 1000;
    private static final long SIXTY_SECONDS = 60000;

    private final CoverageService coverageService;
    private final BFDClient bfdClient;
    private final ThreadPoolTaskExecutor executor;
    private final int maxAttempts;

    private Ab2dEnvironment ab2dEnvironment;

    private final List<CoverageMappingCallable> inProgressMappings = new ArrayList<>();

    // Queue for results of jobs that have already completed
    private final BlockingQueue<CoverageMapping> coverageInsertionQueue = new LinkedBlockingQueue<>();

    private final AtomicBoolean inShutdown = new AtomicBoolean(false);

    /**
     * Coverage processor needs an interface to the database, client for BFD, and thread pool to concurrently execute
     * searches.
     *
     * @param coverageService interface with the database for querying, saving, and inserting searches
     * @param bfdClient REST client for specific calls to BFD
     * @param executor thread pool to execute enrollment updates within
     * @param maxAttempts max number of retries to make for updating enrollment for a specific month before failing outright
     */
    public CoverageProcessorImpl(CoverageService coverageService, BFDClient bfdClient,
                                 @Qualifier("patientCoverageThreadPool") ThreadPoolTaskExecutor executor,
                                 @Value("${coverage.update.max.attempts}") int maxAttempts,
                                 Ab2dEnvironment ab2dEnvironment) {
        this.coverageService = coverageService;
        this.bfdClient = bfdClient;
        this.executor = executor;
        this.maxAttempts = maxAttempts;
        this.ab2dEnvironment = ab2dEnvironment;
    }

    @Override
    public void queueCoveragePeriod(CoveragePeriod period, boolean prioritize) {
        queueCoveragePeriod(period, 0, prioritize);
    }

    /**
     * Queue an already attempted (and cancelled or failed) coverage search for another attempt
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

    /**
     * Attempt to start a coverage search of BFD
     * @param mapping a mapping job
     *
     */
    @Override
    public boolean startJob(CoverageMapping mapping) {

        synchronized (inProgressMappings) {

            if (inShutdown.get()) {
                log.warn("cannot start job because service has been shutdown");
                return false;
            }

            log.info("starting search for {} during {}-{}", mapping.getContractNumber(),
                    mapping.getPeriod().getMonth(), mapping.getPeriod().getYear());

            // Currently, we are using the STU3 version to get patient mappings
            CoverageMappingCallable callable = new CoverageMappingCallable(STU3, mapping, bfdClient, ab2dEnvironment);
            executor.submit(callable);
            inProgressMappings.add(callable);

            return true;
        }
    }

    /**
     * Check to see if processor is currently running too many searches and needs
     * to wait before starting more searches.
     *
     * Attempts to prevent running out of RAM and CPU by having too many searches running.
     *
     * Attempts to avoid these conditions:
     *      - too many jobs actively pulling enrollment from BFD, may DOS BFD
     *      - too many job results sitting in memory which may cause OOM issues
     */
    @Override
    public boolean isProcessorBusy() {

        boolean busy = coverageInsertionQueue.size() >= executor.getCorePoolSize() ||
                executor.getActiveCount() >= executor.getCorePoolSize();

        // Useful log if we run into concurrency issues
        if (busy) {
            log.info("Currently executing {}. Currently inserting {}", executor.getActiveCount(), coverageInsertionQueue.size());
        }

        return busy;
    }

    /**
     * Queue an already attempted coverage search that failed
     * @param mapping a coverage mapping to be performed
     * @param prioritize true if coverage mapping needs to be run first before other periods
     */
    @Override
    public void queueMapping(CoverageMapping mapping, boolean prioritize) {
        queueCoveragePeriod(mapping.getPeriod(), mapping.getCoverageSearch().getAttempts(), prioritize);
    }

    /**
     * Attempt to add a coverage search to the queue again with a record of the attempts already made on that search.
     *
     * @param mapping search that was attempted
     * @param failedDescription reason for failure of previous attempt
     * @param restartDescription report why search is being restarted instead of outright failed
     * @param prioritize whether search needs to happen before other searches
     */
    private void resubmitMapping(CoverageMapping mapping, String failedDescription, String restartDescription, boolean prioritize) {

        if (inShutdown.get()) {
            return;
        }

        log.info("resubmitted coverage period search at front of queue");

        coverageService.resubmitSearch(mapping.getPeriodId(), mapping.getCoverageSearch().getAttempts(),
                failedDescription, restartDescription, prioritize);
    }

    /**
     * Check in progress coverage searches on the current machine and when one of those searches finishes
     * evaluate the result whether successful or failed.
     *
     * Only monitors jobs running in the current application, not jobs running on other machines.
     *
     * This monitoring step only runs if the list of mappings being run can be synchronized and the processor
     * has not been shut down.
     *
     */
    @Scheduled(cron = "${coverage.update.monitoring.interval}")
    public void monitorMappingJobs() {

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

    /**
     * Evaluate the results of a mapping into three buckets:
     *
     *      - Coverage successfully pulled from BFD. Coverage is then queued (in memory) to be written to the database
     *      - Coverage unsuccessfully pulled from BFD and all retries used. Coverage search is marked as failed without a re-attempt
     *      - Coverage unsuccessfully pulled but there are still retries available. Coverage search is resubmitted for another attempt
     *
     * @param mapping results of {@link CoverageMappingCallable}
     */
    public void evaluateJob(CoverageMapping mapping) {
        if (mapping.isSuccessful()) {
            log.info("finished a search for contract {} during {}-{}", mapping.getContractNumber(),
                    mapping.getPeriod().getMonth(), mapping.getPeriod().getYear());

            coverageInsertionQueue.add(mapping);
        } else if (mapping.getCoverageSearch().getAttempts() > maxAttempts) {

            log.error("could not complete coverage mapping job due to multiple failed attempts and will not re-attempt");

            coverageService.failSearch(mapping.getPeriodId(),
                    "could not complete coverage mapping due to error will not re-attempt");
        } else {

            log.warn("could not complete coverage mapping job but will re-attempt");

            resubmitMapping(mapping, "could not complete coverage mapping due to error",
                    "could not complete coverage mapping job but will re-attempt", true);
        }
    }

    /**
     * Insert results of one coverage search at a time into the database unless the processor has shut down.
     *
     * The {@link #attemptCoverageInsertion(CoverageMapping)} method will handle failure to insertion by resubmitting
     * quietly.
     *
     * Only inserts results of coverage mapping jobs run on the current application, not jobs running on other machines
     */
    @Scheduled(fixedDelay = ONE_SECOND, initialDelayString = "${coverage.update.initial.delay}")
    public void insertJobResults() {

        try {
            CoverageMapping result = coverageInsertionQueue.poll(SIXTY_SECONDS, TimeUnit.MILLISECONDS);

            if (result == null) {
                return;
            }

            String contractNumber = result.getContractNumber();
            int month = result.getPeriod().getMonth();
            int year = result.getPeriod().getYear();

            log.info("attempting to insert coverage for {}-{}-{}", contractNumber, month, year);

            if (!inShutdown.get()) {
                log.info("inserting coverage mapping for contract {} during {}-{}",
                        contractNumber, month, year);

                attemptCoverageInsertion(result);
            } else {

                String message = String.format("shutting down before inserting results for" +
                        " contract %s during %d-%d, will re-attempt", contractNumber, month, year);
                log.info(message);

                resubmitMapping(result, message,
                "shutting down coverage processor before beneficiary data can be inserted into database",
                        true);
            }
        } catch (InterruptedException ie) { //NOSONAR
            log.info("polling for data to insert failed due to interruption {}", ie.getMessage());
        }
    }

    /**
     * Attempt to insert all metadata retrieved in a coverage search handling failure quietly.
     *
     * If the database is unavailable or a query times out fail the coverage search quietly, otherwise mark
     * the coverage search as complete.
     *
     * Insertions have failed in the past so each stage of inserting data is logged out for monitoring and postmortems
     * if necessary.
     *
     * @param result all metadata retrieved by a search
     */
    private void attemptCoverageInsertion(CoverageMapping result) {

        int periodId = result.getPeriodId();
        long eventId = result.getCoverageSearchEvent().getId();

        try {
            String contractNumber = result.getContractNumber();
            int month = result.getPeriod().getMonth();
            int year = result.getPeriod().getYear();

            coverageService.insertCoverage(eventId, result.getBeneficiaryIds());

            log.info("finished inserting coverage for {}-{}-{}", contractNumber, month, year);

            coverageService.completeSearch(periodId, "successfully inserted all data for in progress search");

            log.info("marked search as completed {}-{}-{}", contractNumber, month, year);
        } catch (Exception exception) {
            log.error("inserting the coverage data failed for {}-{}-{}", result.getContractNumber(),
                    result.getPeriod().getMonth(), result.getPeriod().getYear());
            log.error("inserting the coverage data failed {}", exception.getMessage());
            coverageService.failSearch(result.getPeriodId(),
                    "inserting coverage information failed with reason: " +
                    exception.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {

        log.warn("shutting down coverage processor");

        // Notify all threads that we are shutting down and that work should not progress through pipeline
        synchronized (inProgressMappings) {
            inShutdown.set(true);
        }

        Collection<CoverageMapping> inserting = new ArrayList<>();

        coverageInsertionQueue.drainTo(inserting);
        coverageInsertionQueue.clear();

        for (CoverageMapping insertedMapping : inserting) {
            String message = String.format("shutting down before inserting for contract %s during %d-%d, will re-attempt",
                    insertedMapping.getContractNumber(), insertedMapping.getPeriod().getMonth(),
                    insertedMapping.getPeriod().getYear());
            log.info(message);

            resubmitMapping(insertedMapping, message, message, false);
        }

        // Force shutdown all running bfd queries

        synchronized (inProgressMappings) {
            executor.setWaitForTasksToCompleteOnShutdown(false);
            executor.shutdown();

            inProgressMappings.forEach(callable -> {
                CoverageMapping mapping = callable.getCoverageMapping();
                String message = String.format("shutting down in progress search for contract %s during %d-%d",
                        mapping.getContractNumber(), mapping.getPeriod().getMonth(),
                        mapping.getPeriod().getYear());
                log.info(message);

                resubmitMapping(mapping, message, message, false);
            });
        }
    }
}
