package gov.cms.ab2d.worker.processor.eob;

import ca.uhn.fhir.context.FhirContext;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.util.Constants;
import gov.cms.ab2d.common.util.FHIRUtil;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.ErrorEvent;
import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneficiaries;
import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneficiaries.PatientDTO;
import gov.cms.ab2d.worker.config.RoundRobinBlockingQueue;
import gov.cms.ab2d.worker.processor.StreamHelper;
import gov.cms.ab2d.worker.processor.TextStreamHelperImpl;
import gov.cms.ab2d.worker.processor.coverage.CoverageMappingCallable;
import gov.cms.ab2d.worker.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Identifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static gov.cms.ab2d.common.model.JobStatus.CANCELLED;
import static gov.cms.ab2d.common.util.Constants.CONTRACT_LOG;
import static gov.cms.ab2d.common.util.Constants.EOB;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.TooManyStaticImports")
public class ContractProcessorImpl implements ContractProcessor {
    private static final int SLEEP_DURATION = 250;
    static final String ID_EXT = "http://hl7.org/fhir/StructureDefinition/elementdefinition-identifier";

    @Value("${job.file.rollover.ndjson:200}")
    private long ndjsonRollOver;

    @Value("${cancellation.check.frequency:10}")
    private int cancellationCheckFrequency;

    @Value("${report.progress.db.frequency:100}")
    private int reportProgressDbFrequency;

    @Value("${report.progress.log.frequency:100}")
    private int reportProgressLogFrequency;

    @Value("${file.try.lock.timeout}")
    private int tryLockTimeout;

    private final FileService fileService;
    private final JobRepository jobRepository;
    private final PatientClaimsProcessor patientClaimsProcessor;
    private final LogManager eventLogger;
    private final FhirContext fhirContext;

    /**
     * Process the contract - retrieve all the patients for the contract and create a thread in the
     * patientProcessorThreadPool to handle searching for EOBs for each patient. Periodically check to
     * see if the job is cancelled and cancel the threads if necessary, otherwise, wait until all threads
     * have processed.
     *
     * @param contractData - the contract data (contract, progress tracker, attested time, writer)
     * @return - the job output records containing the file information
     */
    public List<JobOutput> process(Path outputDirPath, ContractData contractData) {
        var contractNumber = contractData.getContract().getContractNumber();
        log.info("Beginning to process contract {}", keyValue(CONTRACT_LOG, contractNumber));

        ProgressTracker progressTracker = contractData.getProgressTracker();
        Map<String, CoverageSummary> patients = progressTracker.getPatients();
        int patientCount = patients.size();
        log.info("Contract [{}] has [{}] Patients", contractNumber, patientCount);

        long numberOfEobs = 0;
        var jobUuid = progressTracker.getJobUuid();
        Job job = jobRepository.findByJobUuid(jobUuid);
        List<Path> dataFiles = new ArrayList<>();
        List<Path> errorFiles = new ArrayList<>();
        int recordsProcessedCount = 0;
        try (StreamHelper helper = new TextStreamHelperImpl(outputDirPath, contractNumber, getRollOverThreshold(), tryLockTimeout,
                eventLogger, job)) {
            var futureHandles = new ArrayList<Future<EobSearchResult>>();
            for (Map.Entry<String, CoverageSummary> patient : patients.entrySet()) {
                ++recordsProcessedCount;
                futureHandles.add(processPatient(patient.getValue(), contractData));
                // Periodically check if cancelled
                if (recordsProcessedCount % cancellationCheckFrequency == 0) {
                    if (hasJobBeenCancelled(jobUuid)) {
                        log.warn("Job [{}] has been cancelled. Attempting to stop processing the job shortly ... ", jobUuid);
                        cancelFuturesInQueue(futureHandles);
                        break;
                    }
                    numberOfEobs += processHandles(futureHandles, progressTracker, patients, helper);
                }
            }
            while (!futureHandles.isEmpty()) {
                sleep();
                numberOfEobs += processHandles(futureHandles, progressTracker, patients, helper);
                if (hasJobBeenCancelled(jobUuid)) {
                    cancelFuturesInQueue(futureHandles);
                    final String errMsg = "Job was cancelled while it was being processed";
                    log.warn("{}", errMsg);
                    throw new JobCancelledException(errMsg);
                }
            }

            log.info("Finished writing {} EOBs for contract {}", numberOfEobs, contractNumber);
            // All jobs are done, return the job output records
            dataFiles = helper.getDataFiles();
            errorFiles = helper.getErrorFiles();
        } catch (IOException ex) {
            log.error("Unable to open output file");
        }
        return createJobOutputs(dataFiles, errorFiles);
    }

    /**
     * Create a token from newRelic for the transaction.
     * <p>
     * On using new-relic tokens with async calls
     * See https://docs.newrelic.com/docs/agents/java-agent/async-instrumentation/java-agent-api-asynchronous-applications
     *
     * @param patient      - process to process
     * @param contractData - the contract data information
     * @return a Future<EobSearchResult>
     */
    private Future<EobSearchResult> processPatient(CoverageSummary patient, ContractData contractData) {
        final Token token = NewRelic.getAgent().getTransaction().getToken();

        // Using a ThreadLocal to communicate contract number to RoundRobinBlockingQueue
        // could be viewed as a hack by many; but on the other hand it saves us from writing
        // tons of extra code.
        var jobUuid = contractData.getProgressTracker().getJobUuid();
        RoundRobinBlockingQueue.CATEGORY_HOLDER.set(jobUuid);
        try {
            var patientClaimsRequest = new PatientClaimsRequest(patient,
                    contractData.getContract().getAttestedOn(),
                    contractData.getSinceTime(),
                    contractData.getUserId(), jobUuid,
                    contractData.getContract() != null ? contractData.getContract().getContractNumber() : null, token);
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
     * @param futureHandles - the running threads
     */
    private void cancelFuturesInQueue(List<Future<EobSearchResult>> futureHandles) {

        // cancel any futures that have not started processing and are waiting in the queue.
        futureHandles.parallelStream().forEach(future -> future.cancel(false));

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
        return CANCELLED.equals(jobStatus);
    }

    /**
     * For each future, check to see if it's done. If it is, remove it from the list of future handles
     * and increment the number processed
     *
     * @param futureHandles   - the thread futures
     * @param progressTracker - the tracker with updated tracker information
     */
    private int processHandles(List<Future<EobSearchResult>> futureHandles, ProgressTracker progressTracker,
                               Map<String, CoverageSummary> patients, StreamHelper helper) {
        int numberOfEobs = 0;
        var iterator = futureHandles.iterator();
        while (iterator.hasNext()) {
            var future = iterator.next();
            if (future.isDone()) {
                EobSearchResult result = processFuture(futureHandles, progressTracker, future, patients);
                if (result != null) {
                    addMbiIdsToEobs(result.getEobs(), patients);
                }
                if (result != null) {
                    numberOfEobs += writeOutResource(result.getEobs(), helper);
                }
                iterator.remove();
            }
        }

        // update the progress in the DB & logs periodically
        trackProgress(progressTracker);
        return numberOfEobs;
    }

    void addMbiIdsToEobs(List<ExplanationOfBenefit> eobs, Map<String, CoverageSummary> patients) {
        if (eobs == null || eobs.isEmpty()) {
            return;
        }
        // Get first EOB Bene ID
        ExplanationOfBenefit eob = eobs.get(0);

        // Add extesions only if beneficiary id is present and known to memberships
        String benId = getPatientIdFromEOB(eob);
        if (benId != null && patients.containsKey(benId)) {
            Identifiers patient = patients.get(benId).getIdentifiers();

            // Add each mbi to each eob
            if (patient.getCurrentMbi() != null) {
                Extension currentMbiExtension = createExtension(patient.getCurrentMbi(), true);
                eobs.forEach(e -> e.addExtension(currentMbiExtension));
            }

            for (String mbi : patient.getHistoricMbis()) {
                Extension mbiExtension = createExtension(mbi, false);
                eobs.forEach(e -> e.addExtension(mbiExtension));
            }
        }
    }

    /**
     * Create an extension for the EOB containing a patient's mbi
     * @param mbi the mbi (value) to set the extension to
     * @param current whether the mbi is currently active (true) or historical (false)
     * @return mbi extension
     */
    Extension createExtension(String mbi, boolean current) {
        Identifier identifier = new Identifier().setSystem(CoverageMappingCallable.MBI_ID).setValue(mbi);

        Coding coding = new Coding()
                .setCode(current ? CoverageMappingCallable.CURRENT_MBI : CoverageMappingCallable.HISTORIC_MBI);

        Extension currencyExtension = new Extension()
                .setUrl(CoverageMappingCallable.CURRENCY_IDENTIFIER)
                .setValue(coding);
        identifier.setExtension(List.of(currencyExtension));

        return new Extension().setUrl(ID_EXT).setValue(identifier);
    }

    private int writeOutResource(List<ExplanationOfBenefit> eobs, StreamHelper helper) {
        var jsonParser = fhirContext.newJsonParser();

        String payload = "";
        int resourceCount = 0;
        try {
            for (ExplanationOfBenefit resource : eobs) {
                ++resourceCount;
                try {
                    payload = jsonParser.encodeResourceToString(resource) + System.lineSeparator();
                    helper.addData(payload.getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    log.warn("Encountered exception while processing job resources: {}", e.getMessage());
                    writeExceptionToContractErrorFile(helper, payload, e);
                }
            }
        } catch (Exception e) {
            try {
                writeExceptionToContractErrorFile(helper, payload, e);
            } catch (IOException e1) {
                //should not happen - original exception will be thrown
                log.error("error during exception handling to write error record");
            }

            throw new RuntimeException(e.getMessage(), e);
        }
        return resourceCount;
    }

    /**
     * process the future that is marked as done.
     * On doing a get(), if an exception is thrown, analyze it to decide whether to stop the batch or not.
     *
     * @param futureHandles   - List of Futures
     * @param progressTracker - progress tracker instance
     * @param future          - a specific future
     */
    private EobSearchResult processFuture(List<Future<EobSearchResult>> futureHandles, ProgressTracker progressTracker,
                                          Future<EobSearchResult> future, Map<String, CoverageSummary> patients) {
        progressTracker.incrementProcessedCount();
        try {
            EobSearchResult result = future.get();
            if (result != null) {
                result.setEobs(result.getEobs().stream().filter(c -> validPatientInContract(c, patients)).collect(Collectors.toList()));
            }
            return result;
        } catch (CancellationException e) {
            // This could happen in the rare event that a job was cancelled mid-process.
            // due to which the futures in the queue (that were not yet in progress) were cancelled.
            // Nothing to be done here
            log.warn("CancellationException while calling Future.get() - Job may have been cancelled");
        } catch (InterruptedException | ExecutionException | RuntimeException e) {
            analyzeException(futureHandles, progressTracker, e);
        }
        return null;
    }

    public String getPatientIdFromEOB(ExplanationOfBenefit eob) {
        if (eob == null) {
            return null;
        }
        String patientId = eob.getPatient().getReference();
        if (patientId == null) {
            return null;
        }
        return patientId.replaceFirst("Patient/", "");
    }

    /**
     * returns true if the patient is a valid member of a contract, false otherwise. If either value is empty,
     * it returns false
     *
     * @param benefit  - The benefit to check
     * @param patients - the patient map containing the patient id & patient object
     * @return true if this patient is a member of the correct contract
     */
    boolean validPatientInContract(ExplanationOfBenefit benefit, Map<String, CoverageSummary> patients) {
        if (benefit == null || patients == null) {
            log.debug("Passed an invalid benefit or an invalid list of patients");
            return false;
        }
        String patientId = getPatientIdFromEOB(benefit);
        if (patientId == null || patients.get(patientId) == null) {
            log.error(patientId + " returned in EOB, but not a member of a contract");
            return false;
        }
        return true;
    }

    private void analyzeException(List<Future<EobSearchResult>> futureHandles, ProgressTracker progressTracker, Exception e) {
        progressTracker.incrementFailureCount();

        if (progressTracker.isErrorCountBelowThreshold()) {
            final Throwable rootCause = ExceptionUtils.getRootCause(e);
            log.error("exception while processing patient {}", rootCause.getMessage(), rootCause);
            // log exception, but continue processing job as errorCount is below threshold
        } else {
            cancelFuturesInQueue(futureHandles);
            String description = progressTracker.getFailureCount() + " out of " + progressTracker.getTotalCount() + " records failed. Stopping job";
            eventLogger.log(new ErrorEvent(null, progressTracker.getJobUuid(),
                    ErrorEvent.ErrorType.TOO_MANY_SEARCH_ERRORS, description));
            log.error("{} out of {} records failed. Stopping job", progressTracker.getFailureCount(), progressTracker.getTotalCount());
            throw new RuntimeException("Too many patient records in the job had failures");
        }
    }

    void writeExceptionToContractErrorFile(StreamHelper helper, String data, Exception e) throws IOException {
        var errMsg = ExceptionUtils.getRootCauseMessage(e);
        var operationOutcome = FHIRUtil.getErrorOutcome(errMsg);

        var jsonParser = fhirContext.newJsonParser();
        var payload = jsonParser.encodeResourceToString(operationOutcome) + System.lineSeparator();

        var byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(payload.getBytes(StandardCharsets.UTF_8));
        helper.addError(data);
    }

    /**
     * Update the database or log with the % complete on the job periodically
     *
     * @param progressTracker - the progress tracker
     */

    private void trackProgress(ProgressTracker progressTracker) {
        if (progressTracker.isTimeToUpdateDatabase(reportProgressDbFrequency)) {
            final int percentageCompleted = progressTracker.getPercentageCompleted();

            if (percentageCompleted > progressTracker.getLastUpdatedPercentage()) {
                jobRepository.updatePercentageCompleted(progressTracker.getJobUuid(), percentageCompleted);
                progressTracker.setLastUpdatedPercentage(percentageCompleted);
            }
        }

        var processedCount = progressTracker.getProcessedCount();
        if (progressTracker.isTimeToLog(reportProgressLogFrequency)) {
            progressTracker.setLastLogUpdateCount(processedCount);

            var totalCount = progressTracker.getTotalCount();
            var percentageCompleted = progressTracker.getPercentageCompleted();
            log.info("[{}/{}] records processed = [{}% completed]", processedCount, totalCount, percentageCompleted);
        }
    }

    /**
     * Once the job writer is finished, create a list of job output objects with
     * the data files and the error files
     *
     * @param dataFiles  - the results of writing the contract
     * @param errorFiles - any errors that arose due to writing the contract
     * @return the list of job output objects
     */
    List<JobOutput> createJobOutputs(List<Path> dataFiles, List<Path> errorFiles) {

        // create Job Output records for data files from the job writer
        final List<JobOutput> jobOutputs = dataFiles.stream()
                .map(dataFile -> createJobOutput(dataFile, false)).collect(Collectors.toList());

        // create Job Output record for error file
        final List<JobOutput> errorJobOutputs = errorFiles.stream()
                .map(errorFile -> createJobOutput(errorFile, true))
                .collect(Collectors.toList());
        jobOutputs.addAll(errorJobOutputs);

        if (jobOutputs.isEmpty()) {
            var errMsg = "The export process has produced no results";
            throw new RuntimeException(errMsg);
        }

        return jobOutputs;
    }

    /**
     * From a file, return the JobOutput object
     *
     * @param outputFile - the output file from the job
     * @param isError    - if there was an error
     * @return - the job output object
     */
    @SuppressFBWarnings
    private JobOutput createJobOutput(Path outputFile, boolean isError) {
        JobOutput jobOutput = new JobOutput();
        jobOutput.setFilePath(outputFile.getFileName().toString());
        jobOutput.setFhirResourceType(EOB);
        jobOutput.setError(isError);
        jobOutput.setChecksum(fileService.generateChecksum(outputFile.toFile()));
        jobOutput.setFileLength(outputFile.toFile().length());
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
