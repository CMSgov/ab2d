package gov.cms.ab2d.worker.processor.coverage;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.CoverageMapping;
import gov.cms.ab2d.common.model.CoveragePeriod;
import gov.cms.ab2d.common.service.CoverageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static gov.cms.ab2d.fhir.FhirVersion.STU3;

@Slf4j
@Service
public class CoverageProcessorImpl implements CoverageProcessor {

    private static final long ONE_SECOND = 1000;
    private static final long SIXTY_SECONDS = 60000;

    private final CoverageService coverageService;
    private final BFDClient bfdClient;
    private final ThreadPoolTaskExecutor executor;
    private final int maxAttempts;
    private final boolean skipBillablePeriodCheck;

    private final List<CoverageMappingCallable> inProgressMappings = new ArrayList<>();

    // Queue for results of jobs that have already completed
    private final BlockingQueue<CoverageMapping> coverageInsertionQueue = new LinkedBlockingQueue<>();

    private final AtomicBoolean inShutdown = new AtomicBoolean(false);

    public CoverageProcessorImpl(CoverageService coverageService, BFDClient bfdClient,
                                 @Qualifier("patientCoverageThreadPool") ThreadPoolTaskExecutor executor,
                                 @Value("${coverage.update.max.attempts}") int maxAttempts,
                                 @Value("${claims.skipBillablePeriodCheck}") boolean skipBillablePeriodCheck) {
        this.coverageService = coverageService;
        this.bfdClient = bfdClient;
        this.executor = executor;
        this.maxAttempts = maxAttempts;
        this.skipBillablePeriodCheck = skipBillablePeriodCheck;
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

    public boolean startJob(CoverageMapping mapping) {

        synchronized (inProgressMappings) {

            if (inShutdown.get()) {
                log.warn("cannot start job because service has been shutdown");
                return false;
            }

            log.info("starting search for {} during {}-{}", mapping.getContract().getContractNumber(),
                    mapping.getPeriod().getMonth(), mapping.getPeriod().getYear());

            // Currently, we are using the STU3 version to get patient mappings
            CoverageMappingCallable callable = new CoverageMappingCallable(STU3, mapping, bfdClient, skipBillablePeriodCheck);
            executor.submit(callable);
            inProgressMappings.add(callable);

            return true;
        }
    }

    public boolean isProcessorBusy() {

        boolean busy = coverageInsertionQueue.size() >= executor.getCorePoolSize() ||
                executor.getActiveCount() >= executor.getCorePoolSize();

        // Useful log if we run into concurrency issues
        if (busy) {
            log.info("Currently executing {}. Currently inserting {}", executor.getActiveCount(), coverageInsertionQueue.size());
        }

        return busy;
    }

    public void queueMapping(CoverageMapping mapping, boolean prioritize) {
        queueCoveragePeriod(mapping.getPeriod(), mapping.getCoverageSearch().getAttempts(), prioritize);
    }

    private void resubmitMapping(CoverageMapping mapping, String failedDescription, String restartDescription, boolean prioritize) {

        if (inShutdown.get()) {
            return;
        }

        log.info("resubmitted coverage period search at front of queue");

        coverageService.resubmitSearch(mapping.getPeriodId(), mapping.getCoverageSearch().getAttempts(),
                failedDescription, restartDescription, prioritize);
    }

    /**
     * Only monitors jobs running in the current application, not jobs running on other machines
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

    public void evaluateJob(CoverageMapping mapping) {
        if (mapping.isSuccessful()) {
            log.info("finished a search for contract {} during {}-{}", mapping.getContract().getContractNumber(),
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
     * Only inserts results of coverage mapping jobs run on the current application, not jobs running on other machines
     */
    @Scheduled(fixedDelay = ONE_SECOND, initialDelayString = "${coverage.update.initial.delay}")
    public void insertJobResults() {

        try {
            CoverageMapping result = coverageInsertionQueue.poll(SIXTY_SECONDS, TimeUnit.MILLISECONDS);

            if (result == null) {
                return;
            }

            String contractNumber = result.getContract().getContractNumber();
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
        } catch (InterruptedException ie) {
            log.info("polling for data to insert failed due to interruption {}", ie.getMessage());
        }
    }

    /**
     * Attempt to insert all metadata retrieved in a search
     * @param result all metadata retrieved by a search
     */
    private void attemptCoverageInsertion(CoverageMapping result) {

        int periodId = result.getPeriodId();
        long eventId = result.getCoverageSearchEvent().getId();

        try {
            String contractNumber = result.getContract().getContractNumber();
            int month = result.getPeriod().getMonth();
            int year = result.getPeriod().getYear();

            coverageService.insertCoverage(eventId, result.getBeneficiaryIds());

            log.info("finished inserting coverage for {}-{}-{}", contractNumber, month, year);

            coverageService.completeSearch(periodId, "successfully inserted all data for in progress search");

            log.info("marked search as completed {}-{}-{}", contractNumber, month, year);
        } catch (Exception exception) {
            log.error("inserting the coverage data failed for {}-{}-{}", result.getContract().getContractNumber(),
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
                    insertedMapping.getContract().getContractNumber(), insertedMapping.getPeriod().getMonth(),
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
                        mapping.getContract().getContractNumber(), mapping.getPeriod().getMonth(),
                        mapping.getPeriod().getYear());
                log.info(message);

                resubmitMapping(mapping, message, message, false);
            });
        }
    }
}
