package gov.cms.ab2d.worker.processor;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.util.Constants;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.ErrorEvent;
import gov.cms.ab2d.fhir.FhirVersion;
import gov.cms.ab2d.worker.config.RoundRobinBlockingQueue;
import gov.cms.ab2d.worker.processor.coverage.CoverageDriver;
import gov.cms.ab2d.worker.service.JobChannelService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
    private long ndjsonRollOver;

    @Value("${file.try.lock.timeout}")
    private int tryLockTimeout;

    @Value("${eob.job.patient.queue.max.size}")
    private int eobJobPatientQueueMaxSize;

    @Value("${eob.job.patient.queue.page.size}")
    private int eobJobPatientQueuePageSize;

    private final JobRepository jobRepository;
    private final CoverageDriver coverageDriver;
    private final PatientClaimsProcessor patientClaimsProcessor;
    private final LogManager eventLogger;
    private final RoundRobinBlockingQueue<PatientClaimsRequest> eobClaimRequestsQueue;
    private final JobChannelService jobChannelService;
    private final JobProgressService jobProgressService;

    public ContractProcessorImpl(JobRepository jobRepository,
                                 CoverageDriver coverageDriver,
                                 PatientClaimsProcessor patientClaimsProcessor,
                                 LogManager eventLogger,
                                 RoundRobinBlockingQueue<PatientClaimsRequest> eobClaimRequestsQueue,
                                 JobChannelService jobChannelService,
                                 JobProgressService jobProgressService) {
        this.jobRepository = jobRepository;
        this.coverageDriver = coverageDriver;
        this.patientClaimsProcessor = patientClaimsProcessor;
        this.eventLogger = eventLogger;
        this.eobClaimRequestsQueue = eobClaimRequestsQueue;
        this.jobChannelService = jobChannelService;
        this.jobProgressService = jobProgressService;
    }

    /**
     * Process the contract - retrieve all the patients for the contract and create a thread in the
     * patientProcessorThreadPool to handle searching for EOBs for each patient. Periodically check to
     * see if the job is cancelled and cancel the threads if necessary, otherwise, wait until all threads
     * have processed.
     *
     * @return - the job output records containing the file information
     */
    public List<JobOutput> process(Path outputDirPath, Job job) {
        assert job.getContract() != null;
        var contractNumber = job.getContract().getContractNumber();
        log.info("Beginning to process contract {}", keyValue(CONTRACT_LOG, contractNumber));

        int numBenes = coverageDriver.numberOfBeneficiariesToProcess(job);
        jobChannelService.sendUpdate(job.getJobUuid(), JobMeasure.PATIENTS_EXPECTED, numBenes);
        log.info("Contract [{}] has [{}] Patients", contractNumber, numBenes);

        List<JobOutput> jobOutputs = new ArrayList<>();
        try (StreamHelper helper = new TextStreamHelperImpl(outputDirPath, contractNumber, getRollOverThreshold(), tryLockTimeout,
                eventLogger, job)) {

            ContractData contractData = new ContractData(job, helper);
            loadEobRequests(contractData);

            // Wait for remaining work to finish before cleaning up after the job
            // This should be at most eobJobPatientQueueMaxSize requests
            processRemainingRequests(contractData);

            log.info("Finished writing {} EOBs for contract {}",
                    jobProgressService.getStatus(job.getJobUuid()).getEobsProcessedCount(), contractNumber);


            // Close the last file and report it as a job output
            helper.closeLastStream();

            List<StreamOutput> dataOutputs = helper.getDataOutputs();
            dataOutputs.stream().map(output -> createJobOutput(output, false)).forEach(jobOutputs::add);

            List<StreamOutput> errorOutputs = helper.getErrorOutputs();
            errorOutputs.stream().map(output -> createJobOutput(output, true)).forEach(jobOutputs::add);

        } catch (IOException ex) {
            log.error("Unable to open output file");
        } catch (InterruptedException ex) {
            log.error("interrupted while processing job for contract");
        }

        return jobOutputs;
    }

    private void loadEobRequests(ContractData contractData) throws InterruptedException {
        String jobUuid = contractData.getJob().getJobUuid();
        Contract contract = contractData.getJob().getContract();

        // Handle first page of beneficiaries and then enter loop
        CoveragePagingResult current = coverageDriver.pageCoverage(new CoveragePagingRequest(eobJobPatientQueuePageSize,
                null, contract, contractData.getJob().getCreatedAt()));
        loadRequestBatch(contractData, current);
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
            loadRequestBatch(contractData, current);
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

    private void loadRequestBatch(ContractData contractData, CoveragePagingResult result) {

        for (CoverageSummary summary : result.getCoverageSummaries()) {
            Future<EobSearchResult> requestFuture = processPatient(summary, contractData.getJob());

            contractData.addEobRequestHandle(requestFuture);
        }
    }

    private void processRemainingRequests(ContractData contractData) {

        while (contractData.remainingRequestHandles()) {
            sleep();

            processFinishedRequests(contractData);
        }
    }

    private void processFinishedRequests(ContractData contractData) {
        String jobUuid = contractData.getJob().getJobUuid();

        if (hasJobBeenCancelled(jobUuid)) {
            log.warn("Job [{}] has been cancelled. Attempting to stop processing the job shortly ... ",
                    jobUuid);
            cancelFuturesInQueue(contractData.getEobRequestHandles());
            final String errMsg = "Job was cancelled while it was being processed";
            log.warn("{}", errMsg);
            throw new JobCancelledException(errMsg);
        }

        // Process finished requests
        processHandles(contractData);
    }

    /**
     * Create a token from newRelic for the transaction.
     * <p>
     * On using new-relic tokens with async calls
     * See https://docs.newrelic.com/docs/agents/java-agent/async-instrumentation/java-agent-api-asynchronous-applications
     *
     * @param patient - the patient to process
     * @param job - all things about the job including the contract data information
     * @return a Future<EobSearchResult>
     */
    private Future<EobSearchResult> processPatient(CoverageSummary patient, Job job) {
        final Token token = NewRelic.getAgent().getTransaction().getToken();

        Contract contract = job.getContract();
        assert contract != null;

        // Using a ThreadLocal to communicate contract number to RoundRobinBlockingQueue
        // could be viewed as a hack by many; but on the other hand it saves us from writing
        // tons of extra code.
        var jobUuid = job.getJobUuid();
        RoundRobinBlockingQueue.CATEGORY_HOLDER.set(jobUuid);
        try {
            var patientClaimsRequest = new PatientClaimsRequest(patient,
                    contract.getAttestedOn(),
                    job.getSince(),
                    getOrganization(job),
                    jobUuid,
                    contract.getContractNumber(),
                    token,
                    job.getFhirVersion());
            return patientClaimsProcessor.process(patientClaimsRequest);

        } finally {
            RoundRobinBlockingQueue.CATEGORY_HOLDER.remove();
        }
    }

    /**
     * Return the number of bytes when to rollover given the number of megabytes
     *
     * @return the number of bytes
     */
    private long getRollOverThreshold() {
        return ndjsonRollOver * Constants.ONE_MEGA_BYTE;
    }

    /**
     * Cancel threads
     *
     * @param eobRequestHandles - all of the handles associated with a job
     */
    private void cancelFuturesInQueue(List<Future<EobSearchResult>> eobRequestHandles) {

        // cancel any futures that have not started processing and are waiting in the queue.
        eobRequestHandles.parallelStream().forEach(future -> future.cancel(false));

        //At this point, there may be a few futures that are already in progress.
        //But all the futures that are not yet in progress would be cancelled.
    }

    /**
     * A Job could run for a long time, perhaps hours. An in process job can be cancelled. Here
     * we search for the job and determine if the status has been changed to cancel. This is checked
     * periodically while processing the job.
     *
     * @param jobUuid - the job id
     * @return true if the job is cancelled
     */
    private boolean hasJobBeenCancelled(String jobUuid) {
        final JobStatus jobStatus = jobRepository.findJobStatus(jobUuid);
        return CANCELLED == jobStatus;
    }

    /**
     * For each future, check to see if it's done. If it is, remove it from the list of future handles
     * and increment the number processed
     *
     * @param contractData set of contract data
     */
    private void processHandles(ContractData contractData) {
        var iterator = contractData.getEobRequestHandles().iterator();

        ProgressTrackerUpdate updateTracker = new ProgressTrackerUpdate();

        while (iterator.hasNext()) {
            var future = iterator.next();
            if (future.isDone()) {
                updateTracker.incPatientProcessCount();

                // If the request completed successfully there will be results to process
                EobSearchResult result = processFuture(updateTracker, future);

                if (result == null) {
                    log.debug("ignoring empty results because pulling eobs failed");
                } else if (result.getEobs() == null) {
                    log.error("result returned but the eob list is null which should not be possible");
                } else if (!result.getEobs().isEmpty()) {
                    writeOutResource(contractData, updateTracker, result.getEobs());
                }

                iterator.remove();
            }
        }

        // Update progress after going through the loop
        updateJobProgress(contractData, updateTracker);

        // Check whether failures have reached over the threshold where we need to fail the job
        checkErrorThreshold(contractData);
    }

    /**
     * process the future that is marked as done.
     * On doing a get(), if an exception is thrown, analyze it to decide whether to stop the batch or not.
     *
     * @param future       - a specific future
     */
    @Trace
    private EobSearchResult processFuture(ProgressTrackerUpdate update, Future<EobSearchResult> future) {
        try {
            return future.get();
        } catch (CancellationException e) {
            // This could happen in the rare event that a job was cancelled mid-process.
            // due to which the futures in the queue (that were not yet in progress) were cancelled.
            // Nothing to be done here
            log.warn("CancellationException while calling Future.get() - Job may have been cancelled");
        } catch (InterruptedException | ExecutionException | RuntimeException e) {
            update.incPatientFailureCount();
            final Throwable rootCause = ExceptionUtils.getRootCause(e);
            log.error("exception while processing patient {}", rootCause.getMessage(), rootCause);
        }

        return null;
    }

    @Trace(metricName = "EOBWriteToFile", dispatcher = true)
    private void writeOutResource(ContractData contractData, ProgressTrackerUpdate updateTracker, List<IBaseResource> eobs) {
        var jsonParser = contractData.getFhirVersion().getJsonParser().setPrettyPrint(false);

        String payload = "";
        try {

            updateTracker.addEobFetchedCount(eobs.size());

            int eobsWritten = 0;
            int eobsError = 0;
            for (IBaseResource resource : eobs) {
                try {
                    payload = jsonParser.encodeResourceToString(resource) + System.lineSeparator();
                    contractData.getStreamHelper()
                            .addData(payload.getBytes(StandardCharsets.UTF_8));
                    eobsWritten++;
                } catch (Exception e) {
                    log.warn("Encountered exception while processing job resources: {}", e.getClass());
                    writeExceptionToContractErrorFile(contractData, payload, e);
                    eobsError++;
                }
            }

            updateTracker.addEobProcessedCount(eobsWritten);

            // Log that the patient failed but do not log how many eobs failed. Each eob will be written to a file
            if (eobsError != 0) {
                updateTracker.incPatientFailureCount();
            }
        } catch (Exception e) {
            try {
                writeExceptionToContractErrorFile(contractData, payload, e);
            } catch (IOException e1) {
                //should not happen - original exception will be thrown
                log.error("error during exception handling to write error record");
            }

            throw new RuntimeException(e.getMessage(), e);
        }
    }

    void writeExceptionToContractErrorFile(ContractData contractData, String data, Exception e) throws IOException {
        var errMsg = ExceptionUtils.getRootCauseMessage(e);
        FhirVersion fhirVersion = contractData.getFhirVersion();
        IBaseResource operationOutcome = fhirVersion.getErrorOutcome(errMsg);

        var jsonParser = fhirVersion.getJsonParser().setPrettyPrint(false);
        var payload = jsonParser.encodeResourceToString(operationOutcome) + System.lineSeparator();

        var byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(payload.getBytes(StandardCharsets.UTF_8));
        contractData.getStreamHelper().addError(data);
    }

    private void updateJobProgress(ContractData contractData, ProgressTrackerUpdate updateTracker) {
        String jobUuid = contractData.getJob().getJobUuid();
        jobChannelService.sendUpdate(jobUuid, JobMeasure.PATIENT_REQUESTS_PROCESSED,
                updateTracker.getPatientRequestProcessedCount());
        jobChannelService.sendUpdate(jobUuid, JobMeasure.PATIENT_REQUESTS_ERRORED,
                updateTracker.getPatientFailureCount());

        jobChannelService.sendUpdate(jobUuid, JobMeasure.EOBS_FETCHED,
                updateTracker.getEobsFetchedCount());
        jobChannelService.sendUpdate(jobUuid, JobMeasure.EOBS_WRITTEN,
                updateTracker.getEobsProcessedCount());
    }

    private void checkErrorThreshold(ContractData contractData) {
        ProgressTracker progressTracker = jobProgressService.getStatus(contractData.getJob().getJobUuid());

        if (progressTracker.isErrorThresholdExceeded()) {
            cancelFuturesInQueue(contractData.getEobRequestHandles());
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
     * @param isError      - if there was an error
     * @return - the job output object
     */
    @Trace(dispatcher = true)
    @SuppressFBWarnings
    private JobOutput createJobOutput(StreamOutput streamOutput, boolean isError) {
        JobOutput jobOutput = new JobOutput();
        jobOutput.setFilePath(streamOutput.getFilePath());
        jobOutput.setFhirResourceType(EOB);
        jobOutput.setError(isError);
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
