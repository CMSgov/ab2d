package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.aggregator.AggregatorCallable;
import gov.cms.ab2d.aggregator.FileOutputType;
import gov.cms.ab2d.aggregator.FileUtils;
import gov.cms.ab2d.aggregator.JobHelper;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobOutput;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.util.Constants;
import gov.cms.ab2d.coverage.model.CoveragePagingRequest;
import gov.cms.ab2d.coverage.model.CoveragePagingResult;
import gov.cms.ab2d.coverage.model.CoverageSummary;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.ErrorEvent;
import gov.cms.ab2d.fhir.FhirVersion;
import gov.cms.ab2d.worker.config.RoundRobinBlockingQueue;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriver;
import gov.cms.ab2d.worker.service.JobChannelService;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.stream.Collectors;

import static gov.cms.ab2d.aggregator.FileOutputType.DATA;
import static gov.cms.ab2d.aggregator.FileOutputType.ERROR;
import static gov.cms.ab2d.common.model.JobStatus.CANCELLED;

import static gov.cms.ab2d.common.util.Constants.CONTRACT_LOG;
import static gov.cms.ab2d.common.util.EventUtils.getOrganization;
import static gov.cms.ab2d.fhir.BundleUtils.EOB;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

@Slf4j
@Service
@SuppressWarnings({"PMD.TooManyStaticImports", "java:S2142"}) //java:S2142: "InterruptedException" should not be ignored
public class ContractProcessorImpl implements ContractProcessor {
    private static final int SLEEP_DURATION = 250;

    @Value("${job.file.rollover.ndjson:200}")
    private int ndjsonRollOver;

    @Value("${eob.job.patient.queue.max.size}")
    private int eobJobPatientQueueMaxSize;

    @Value("${eob.job.patient.queue.page.size}")
    private int eobJobPatientQueuePageSize;

    private final ContractRepository contractRepository;

    @Value("${eob.job.patient.number.per.thread}")
    private int numberPatientRequestsPerThread;

    @Value("${efs.mount}")
    private String efsMount;

    @Value("${aggregator.directory.finished}")
    private String finishedDir;

    @Value("${aggregator.directory.streaming}")
    private String streamingDir;

    @Value("${aggregator.multiplier}")
    private int multiplier;

    private final JobRepository jobRepository;
    private final CoverageDriver coverageDriver;
    private final PatientClaimsProcessor patientClaimsProcessor;
    private final LogManager eventLogger;
    private final RoundRobinBlockingQueue<PatientClaimsRequest> eobClaimRequestsQueue;
    private final JobChannelService jobChannelService;
    private final JobProgressService jobProgressService;
    private final ThreadPoolTaskExecutor aggregatorThreadPool;

    @SuppressWarnings("checkstyle:ParameterNumber") // TODO - refactor to eliminate the ridiculous number of args
    public ContractProcessorImpl(ContractRepository contractRepository,
                                 JobRepository jobRepository,
                                 CoverageDriver coverageDriver,
                                 PatientClaimsProcessor patientClaimsProcessor,
                                 LogManager eventLogger,
                                 RoundRobinBlockingQueue<PatientClaimsRequest> eobClaimRequestsQueue,
                                 JobChannelService jobChannelService,
                                 JobProgressService jobProgressService,
                                 @Qualifier("aggregatorThreadPool") ThreadPoolTaskExecutor aggregatorThreadPool) {
        this.contractRepository = contractRepository;
        this.jobRepository = jobRepository;
        this.coverageDriver = coverageDriver;
        this.patientClaimsProcessor = patientClaimsProcessor;
        this.eventLogger = eventLogger;
        this.eobClaimRequestsQueue = eobClaimRequestsQueue;
        this.jobChannelService = jobChannelService;
        this.jobProgressService = jobProgressService;
        this.aggregatorThreadPool = aggregatorThreadPool;
    }

    /**
     * Process the contract - execute an entire {@link Job} from start to finish. Under the hood beneficiaries are
     * loaded from our database, queued for a thread pool to process, and the results processed and written out.
     * <p>
     * Under the hood all of this is done by paging through beneficiaries. We queue {@link #eobJobPatientQueuePageSize}
     * beneficiaries at a time and then check to see what requests have finished before attempting
     * to queue more beneficiaries.
     * <p>
     * Steps
     * - Calculate number of beneficiaries expected to process for the job
     * - Open a stream to the file system to write out results
     * - Page through enrollment queueing requests for each patient, and processing results
     * - Handle remaining requests waiting to be finished
     * - Report all files generated by running the job
     * - Cleanup stream
     * <p>
     * Periodically run at various points in this method
     * - Update the progress tracker with number of beneficiaries and eobs processed
     * - Are too many failures occurring in requests and do we need to shut down?
     * - Has the job been cancelled externally?
     * - Are too many requests waiting to run and do we have to wait for some requests to be processed?
     * - Log progress to database or console
     *
     * @return - the job output records containing the file information
     */
    public List<JobOutput> process(Path outputDirPath, Job job) {
        var contractNumber = job.getContractNumber();
        log.info("Beginning to process contract {}", keyValue(CONTRACT_LOG, contractNumber));

        //noinspection OptionalGetWithoutIsPresent
        Contract contract = contractRepository.findContractByContractNumber(contractNumber).get();
        int numBenes = coverageDriver.numberOfBeneficiariesToProcess(job, contract);
        jobChannelService.sendUpdate(job.getJobUuid(), JobMeasure.PATIENTS_EXPECTED, numBenes);
        log.info("Contract [{}] has [{}] Patients", contractNumber, numBenes);

        // Create the aggregator
        AggregatorCallable aggregator = new AggregatorCallable(efsMount, job.getJobUuid(), contractNumber,
                ndjsonRollOver, streamingDir, finishedDir, multiplier);

        List<JobOutput> jobOutputs = new ArrayList<>();
        try {
            // Let the aggregator create all the necessary directories
            JobHelper.workerSetUpJobDirectories(job.getJobUuid(), efsMount, streamingDir, finishedDir);
            // Create the aggregator thread
            Future<Integer> aggregatorFuture = aggregatorThreadPool.submit(aggregator);

            ContractData contractData = new ContractData(contract, job);

            contractData.addAggregatorHandle(aggregatorFuture);
            // Iterate through pages of beneficiary data
            loadEobRequests(contractData);

            // Wait for remaining work to finish before cleaning up after the job
            // This should be at most eobJobPatientQueueMaxSize requests
            processRemainingRequests(contractData);

            log.info("Finished writing {} EOBs for contract {}",
                    jobProgressService.getStatus(job.getJobUuid()).getEobsProcessedCount(), contractNumber);

            // Mark the job as finished for the aggregator (all file data has been written out)
            JobHelper.workerFinishJob(efsMount + "/" + job.getJobUuid() + "/" + streamingDir);

            // Wait for the aggregator to finish
            while (!aggregatorFuture.isDone()) {
                Thread.sleep(1000);
            }

            // Retrieve all the job output info
            jobOutputs.addAll(getOutputs(job.getJobUuid(), DATA));
            jobOutputs.addAll(getOutputs(job.getJobUuid(), ERROR));
            System.out.println("Number of outputs: " + jobOutputs.size());

        } catch (InterruptedException | IOException ex) {
            log.error("interrupted while processing job for contract");
        }

        return jobOutputs;
    }

    List<JobOutput> getOutputs(String jobId, FileOutputType type) {
        List<JobOutput> jobOutputs = new ArrayList<>();
        List<StreamOutput> dataOutputs = FileUtils.listFiles(efsMount + "/" + jobId, type).stream()
                .map(file -> OutputHelper.createStreamOutput(file, type))
                .collect(Collectors.toList());
        dataOutputs.stream().map(output -> createJobOutput(output, type)).forEach(jobOutputs::add);
        return jobOutputs;
    }

    /**
     * Load beneficiaries and create an EOB request for each patient. Patients are loaded a page at a time. The page size is
     * configurable. At the end of loading all requests, the number of requests loaded is compared to the expected
     * number and the job immediately failed if not equal.
     * <p>
     * Steps:
     * - load a page of beneficiaries from the database
     * - create a request per patient and queue each request
     * - update the progress tracker with the number of patients added
     * - check if the job has been cancelled
     * - process any requests to BFD that are complete
     *
     * @param contractData job requests record and object for storing in motion requests
     * @throws InterruptedException if job is shut down during a busy wait for space in the queue
     */
    private void loadEobRequests(ContractData contractData) throws InterruptedException {
        String jobUuid = contractData.getJob().getJobUuid();
        Contract contract = contractData.getContract();

        // Handle first page of beneficiaries and then enter loop
        CoveragePagingResult current = coverageDriver.pageCoverage(new CoveragePagingRequest(eobJobPatientQueuePageSize,
                null, contract, contractData.getJob().getCreatedAt()));
        loadRequestBatch(contractData, current, numberPatientRequestsPerThread);
        jobChannelService.sendUpdate(jobUuid, JobMeasure.PATIENT_REQUEST_QUEUED, current.size());

        // Do not replace with for each, continue is meant to force patients to wait to be queued
        //noinspection WhileLoopReplaceableByForEach
        while (current.getNextRequest().isPresent()) {

            if (eobClaimRequestsQueue.size(jobUuid) > eobJobPatientQueueMaxSize) {
                // Wait for queue to empty out some before adding more
                //noinspection BusyWait
                Thread.sleep(1000);
                continue;
            }

            // Queue a batch of patients
            current = coverageDriver.pageCoverage(current.getNextRequest().get());
            loadRequestBatch(contractData, current, numberPatientRequestsPerThread);
            jobChannelService.sendUpdate(jobUuid, JobMeasure.PATIENT_REQUEST_QUEUED, current.size());

            processFinishedRequests(contractData);
        }

        // Verify that the number of benes requested matches the number expected from the database and fail
        // immediately if the two do not match
        ProgressTracker progressTracker = jobProgressService.getStatus(jobUuid);
        int totalQueued = progressTracker.getPatientRequestQueuedCount();
        int totalExpected = progressTracker.getPatientsExpected();

        if (totalQueued != totalExpected) {
            throw new ContractProcessingException("expected " + totalExpected +
                    " patients from database but retrieved " + totalQueued);
        }
    }

    /**
     * For each request queue a {@link org.springframework.scheduling.annotation.Async} callable and record
     * that callable as in progress.
     *
     * @param contractData object containing list of in progress requests
     * @param result       the page of beneficiaries that need requests to be created for them
     */
    private void loadRequestBatch(ContractData contractData, CoveragePagingResult result, int searchBatchSize) {
        Queue<CoverageSummary> coverageSummaries = new LinkedList<>(result.getCoverageSummaries());
        int actualBatchSize = searchBatchSize == 0 ? 1 : searchBatchSize;

        Job job = contractData.getJob();
        while (coverageSummaries.size() > 0) {
            List<CoverageSummary> subList = new ArrayList<>(actualBatchSize);
            for (int i = 0; i < actualBatchSize; i++) {
                if (coverageSummaries.size() > 0) {
                    subList.add(coverageSummaries.remove());
                }
            }
            Future<ProgressTrackerUpdate> requestFuture = queuePatientClaimsRequest(subList, contractData);
            contractData.addEobRequestHandle(requestFuture, subList.size());
        }
    }

    /**
     * After all requests are queued wait until all requests in queue are finished
     *
     * @param contractData object containing list of remaining requests
     */
    private void processRemainingRequests(ContractData contractData) {

        while (contractData.remainingRequestHandles()) {
            sleep();

            processFinishedRequests(contractData);
        }
    }

    private void processFinishedRequests(ContractData contractData) {
        String jobUuid = contractData.getJob().getJobUuid();
        Job job = jobRepository.findByJobUuid(jobUuid);

        if (job.hasJobBeenCancelled()) {
            log.warn("Job [{}] has been cancelled. Attempting to stop processing the job shortly ... ",
                    job.getJobUuid());
            cancelFuturesInQueue(contractData);
            final String errMsg = "Job was cancelled while it was being processed";
            log.warn("{}", errMsg);
            throw new JobCancelledException(errMsg);
        }

        // Process finished requests
        processHandles(contractData);
    }

    /**
     * Create a {@link PatientClaimsRequest} and queue the request into a round robin queue used by the eob thread pool.
     * <p>
     * The queue maintains multiple distinct queues, one for each job running, and offers guarantees that jobs
     * are served equally.
     * <p>
     * On using new-relic tokens with async calls
     * See https://docs.newrelic.com/docs/agents/java-agent/async-instrumentation/java-agent-api-asynchronous-applications
     *
     * @param patient - the patient to process
     * @return a pointer to the queued request which will complete or be cancelled at some point.
     */
    private Future<ProgressTrackerUpdate> queuePatientClaimsRequest(List<CoverageSummary> patient, ContractData contractData) {
        final Token token = NewRelic.getAgent().getTransaction().getToken();

        Job job = contractData.getJob();
        // Using a ThreadLocal to communicate contract number to RoundRobinBlockingQueue
        // could be viewed as a hack by many; but on the other hand it saves us from writing
        // tons of extra code.
        var jobUuid = job.getJobUuid();
        RoundRobinBlockingQueue.CATEGORY_HOLDER.set(jobUuid);
        try {
            var patientClaimsRequest = new PatientClaimsRequest(patient,
                    contractData.getContract().getAttestedOn(),
                    job.getSince(),
                    getOrganization(job),
                    jobUuid,
                    job.getContractNumber(),
                    contractData.getContract().getContractType(),
                    token,
                    job.getFhirVersion(),
                    efsMount);
            return patientClaimsProcessor.process(patientClaimsRequest);

        } finally {
            RoundRobinBlockingQueue.CATEGORY_HOLDER.remove();
        }
    }

    /**
     * Cancel threads
     *
     * @param contractData - all of the handles associated with a job (searching & aggregating)
     */
    private void cancelFuturesInQueue(ContractData contractData) {
        List<Future<ProgressTrackerUpdate>> eobRequestHandles = contractData.getEobRequestHandles();

        // cancel any futures that have not started processing and are waiting in the queue.
        eobRequestHandles.parallelStream().forEach(future -> future.cancel(false));

        //At this point, there may be a few futures that are already in progress.
        //But all the futures that are not yet in progress would be cancelled.

        // Cancel the aggregator
        contractData.getAggregatorHandle().cancel(false);
    }

    /**
     * For each future, check to see if it's done. If it is, remove it from the list of future handles
     * and increment the number processed
     *
     * @param contractData set of contract data
     */
    private void processHandles(ContractData contractData) {
        var iterator = contractData.getEobRequestHandles().iterator();

        while (iterator.hasNext()) {
            var future = iterator.next();
            if (future.isDone()) {
                // If the request completed successfully there will be results to process
                ProgressTrackerUpdate update = processFuture(future, contractData);
                // Update progress after each written out file
                updateJobProgress(contractData, update);
                iterator.remove();
            }
        }

        // Check whether failures have reached over the threshold where we need to fail the job
        checkErrorThreshold(contractData);
    }

    /**
     * process the future that is marked as done.
     * On doing a get(), if an exception is thrown, analyze it to decide whether to stop the batch or not.
     *
     * @param future - a specific future
     */
    @Trace
    private ProgressTrackerUpdate processFuture(Future<ProgressTrackerUpdate> future, ContractData data) {
        int numBenes = 0;
        try {
            numBenes = data.getNumberBenes(future);
            return future.get();
        } catch (CancellationException e) {
            // This could happen in the rare event that a job was cancelled mid-process.
            // due to which the futures in the queue (that were not yet in progress) were cancelled.
            // Nothing to be done here
            log.warn("CancellationException while calling Future.get() - Job may have been cancelled");
            return new ProgressTrackerUpdate();
        } catch (InterruptedException | ExecutionException | RuntimeException e) {
            ProgressTrackerUpdate update = new ProgressTrackerUpdate();
            update.incPatientProcessCount(numBenes);
            update.incPatientFailureCount(numBenes);
            final Throwable rootCause = ExceptionUtils.getRootCause(e);
            log.error("exception while processing patient {}", rootCause.getMessage(), rootCause);
            return update;
        }
    }

    private void updateJobProgress(ContractData contractData, ProgressTrackerUpdate updateTracker) {
        String jobUuid = contractData.getJob().getJobUuid();
        jobChannelService.sendUpdate(jobUuid, JobMeasure.PATIENT_REQUESTS_PROCESSED,
                updateTracker.getPatientRequestProcessedCount());
        jobChannelService.sendUpdate(jobUuid, JobMeasure.PATIENT_REQUESTS_ERRORED,
                updateTracker.getPatientFailureCount());

        jobChannelService.sendUpdate(jobUuid, JobMeasure.PATIENTS_WITH_EOBS,
                updateTracker.getPatientWithEobCount());

        jobChannelService.sendUpdate(jobUuid, JobMeasure.EOBS_FETCHED,
                updateTracker.getEobsFetchedCount());
        jobChannelService.sendUpdate(jobUuid, JobMeasure.EOBS_WRITTEN,
                updateTracker.getEobsProcessedCount());
    }

    private void checkErrorThreshold(ContractData contractData) {
        ProgressTracker progressTracker = jobProgressService.getStatus(contractData.getJob().getJobUuid());

        if (progressTracker.isErrorThresholdExceeded()) {
            cancelFuturesInQueue(contractData);
            contractData.getAggregatorHandle().cancel(true);
            String description = progressTracker.getPatientFailureCount() + " out of " + progressTracker.getTotalCount() + " records failed. Stopping job";
            eventLogger.log(new ErrorEvent(null, progressTracker.getJobUuid(),
                    ErrorEvent.ErrorType.TOO_MANY_SEARCH_ERRORS, description));
            log.error("{} out of {} records failed. Stopping job", progressTracker.getPatientFailureCount(), progressTracker.getTotalCount());
            throw new RuntimeException("Too many patient records in the job had failures");
        }
    }

    /**
     * From a file, return the JobOutput object
     *
     * @param streamOutput - the output file from the job
     * @param type         - file output type
     * @return - the job output object
     */
    @Trace(dispatcher = true)
    @SuppressFBWarnings
    private JobOutput createJobOutput(StreamOutput streamOutput, FileOutputType type) {
        JobOutput jobOutput = new JobOutput();
        jobOutput.setFilePath(streamOutput.getFilePath());
        jobOutput.setFhirResourceType(EOB);
        jobOutput.setError(type == ERROR);
        jobOutput.setChecksum(streamOutput.getChecksum());
        jobOutput.setFileLength(streamOutput.getFileLength());
        return jobOutput;
    }

    /**
     * Tell the thread to sleep
     */
    private void sleep() {
        try {
            Thread.sleep(SLEEP_DURATION);
        } catch (InterruptedException e) {
            log.warn("interrupted exception in thread.sleep(). Ignoring");
        }
    }
}
