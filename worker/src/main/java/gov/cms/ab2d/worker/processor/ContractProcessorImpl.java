package gov.cms.ab2d.worker.processor;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.CoverageSummary;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobOutput;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.util.Constants;
import gov.cms.ab2d.common.util.fhir.FhirUtils;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.ErrorEvent;
import gov.cms.ab2d.fhir.EobUtils;
import gov.cms.ab2d.fhir.FhirVersion;
import gov.cms.ab2d.worker.config.RoundRobinBlockingQueue;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static gov.cms.ab2d.common.model.JobStatus.CANCELLED;
import static gov.cms.ab2d.common.util.Constants.CONTRACT_LOG;
import static gov.cms.ab2d.common.util.EventUtils.getOrganization;
import static gov.cms.ab2d.fhir.BundleUtils.EOB;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

@Slf4j
@Service
@SuppressWarnings("PMD.TooManyStaticImports")
public class ContractProcessorImpl implements ContractProcessor {
    private static final int SLEEP_DURATION = 250;
    static final String ID_EXT = "http://hl7.org/fhir/StructureDefinition/elementdefinition-identifier";

    @Value("${job.file.rollover.ndjson:200}")
    private long ndjsonRollOver;

    @Value("${cancellation.check.frequency:10}")
    private int cancellationCheckFrequency;

    @Value("${file.try.lock.timeout}")
    private int tryLockTimeout;

    @Value("${eob.job.patient.queue.max.size}")
    private int eobJobPatientQueueMaxSize;

    private final JobRepository jobRepository;
    private final PatientClaimsProcessor patientClaimsProcessor;
    private final LogManager eventLogger;
    private final RoundRobinBlockingQueue<PatientClaimsRequest> eobClaimRequestsQueue;
    private final JobChannelService jobChannelService;
    private final JobProgressService jobProgressService;

    public ContractProcessorImpl(JobRepository jobRepository,
                                 PatientClaimsProcessor patientClaimsProcessor,
                                 LogManager eventLogger,
                                 RoundRobinBlockingQueue<PatientClaimsRequest> eobClaimRequestsQueue,
                                 JobChannelService jobChannelService,
                                 JobProgressService jobProgressService) {
        this.jobRepository = jobRepository;
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
     * @param patients - the map of all the patients we are working on.
     * @return - the job output records containing the file information
     */
    public List<JobOutput> process(Path outputDirPath, Job job, Map<Long, CoverageSummary> patients) {
        assert job.getContract() != null;
        var contractNumber = job.getContract().getContractNumber();
        log.info("Beginning to process contract {}", keyValue(CONTRACT_LOG, contractNumber));

        log.info("Contract [{}] has [{}] Patients", contractNumber, patients.size());

        List<JobOutput> jobOutputs = new ArrayList<>();
        try (StreamHelper helper = new TextStreamHelperImpl(outputDirPath, contractNumber, getRollOverThreshold(), tryLockTimeout,
                eventLogger, job)) {

            ContractData contractData = new ContractData(job, patients, helper);
            loadRequests(job, patients, contractData);

            // Wait for remaining work to finish before cleaning up after the job
            // This should be at most eobJobPatientQueueMaxSize requests
            while (contractData.remainingRequestHandles()) {
                sleep();
                processHandles(contractData);

                if (hasJobBeenCancelled(job.getJobUuid())) {
                    cancelFuturesInQueue(contractData.getEobRequestHandles());
                    final String errMsg = "Job was cancelled while it was being processed";
                    log.warn("{}", errMsg);
                    throw new JobCancelledException(errMsg);
                }
            }

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

    private void loadRequests(Job job, Map<Long, CoverageSummary> patients,
                              ContractData contractData) throws InterruptedException {
        String jobUuid = job.getJobUuid();
        int numQueued = 0;

        Iterator<Map.Entry<Long, CoverageSummary>> patientEntries = patients.entrySet().iterator();
        //noinspection WhileLoopReplaceableByForEach
        while (patientEntries.hasNext()) {

            if (eobClaimRequestsQueue.size(job.getJobUuid()) > eobJobPatientQueueMaxSize) {
                // Wait for queue to empty out some before adding more
                //noinspection BusyWait
                Thread.sleep(1000);
                continue;
            }

            // Queue a patient
            CoverageSummary patient = patientEntries.next().getValue();
            assert job.getContract() != null;

            contractData.addEobRequestHandle(processPatient(contractData.getFhirVersion(), patient, job));

            // Periodically check if cancelled
            if (++numQueued % cancellationCheckFrequency == 0) {

                // Update progress periodically as well
                this.jobChannelService.sendUpdate(jobUuid, JobMeasure.PATIENT_REQUEST_QUEUED, numQueued);

                numQueued = 0;

                if (hasJobBeenCancelled(jobUuid)) {
                    log.warn("Job [{}] has been cancelled. Attempting to stop processing the job shortly ... ",
                            jobUuid);
                    cancelFuturesInQueue(contractData.getEobRequestHandles());
                    break;
                }
                processHandles(contractData);
            }
        }

        this.jobChannelService.sendUpdate(jobUuid, JobMeasure.PATIENT_REQUEST_QUEUED, numQueued);
    }

    /**
     * Create a token from newRelic for the transaction.
     * <p>
     * On using new-relic tokens with async calls
     * See https://docs.newrelic.com/docs/agents/java-agent/async-instrumentation/java-agent-api-asynchronous-applications
     *
     * @param version - the FHIR version to search
     * @param patient - the patient to process
     * @param job - all things about the job including the contract data information
     * @return a Future<EobSearchResult>
     */
    private Future<EobSearchResult> processPatient(FhirVersion version, CoverageSummary patient, Job job) {
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
                    version);
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
                EobSearchResult result = processFuture(contractData, updateTracker, future);

                if (result == null) {
                    log.debug("ignoring empty results because pulling eobs failed");
                } else if (result.getEobs() == null) {
                    log.error("result returned but the eob list is null which should not be possible");
                } else if (!result.getEobs().isEmpty()) {
                    FhirUtils.addMbiIdsToEobs(result.getEobs(), contractData.getPatients(), contractData.getFhirVersion());
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
     * @param contractData - standard contract data
     * @param future       - a specific future
     */
    @Trace
    private EobSearchResult processFuture(ContractData contractData, ProgressTrackerUpdate update, Future<EobSearchResult> future) {
        try {
            EobSearchResult result = future.get();
            if (result != null) {
                result.setEobs(result.getEobs().stream().filter(c -> validPatientInContract(c, contractData.getPatients())).collect(Collectors.toList()));
            }
            return result;
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

    /**
     * returns true if the patient is a valid member of a contract, false otherwise. If either value is empty,
     * it returns false
     *
     * @param benefit  - The benefit to check
     * @param patients - the patient map containing the patient id & patient object
     * @return true if this patient is a member of the correct contract
     */
    boolean validPatientInContract(IBaseResource benefit, Map<Long, CoverageSummary> patients) {
        if (benefit == null || patients == null) {
            log.debug("Passed an invalid benefit or an invalid list of patients");
            return false;
        }
        Long patientId = getPatientIdFromEOB(benefit);
        if (patientId == null || patients.get(patientId) == null) {
            log.error(patientId + " returned in EOB, but not a member of a contract");
            return false;
        }
        return true;
    }

    public Long getPatientIdFromEOB(IBaseResource eob) {
        return EobUtils.getPatientId(eob);
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
