package gov.cms.ab2d.worker.processor;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.JobOutput;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.model.OptOut;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.repository.OptOutRepository;
import gov.cms.ab2d.common.util.Constants;
import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse;
import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse.PatientDTO;
import gov.cms.ab2d.worker.config.RoundRobinBlockingQueue;
import gov.cms.ab2d.worker.processor.StreamHelperImpl.FileOutputType;
import gov.cms.ab2d.worker.processor.domainmodel.ContractData;
import gov.cms.ab2d.worker.processor.domainmodel.ProgressTracker;
import gov.cms.ab2d.worker.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static gov.cms.ab2d.common.model.JobStatus.CANCELLED;
import static gov.cms.ab2d.common.util.Constants.CONTRACT_LOG;
import static gov.cms.ab2d.common.util.Constants.EOB;
import static gov.cms.ab2d.worker.processor.StreamHelperImpl.FileOutputType.ZIP;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractProcessorImpl implements ContractProcessor {
    private static final int SLEEP_DURATION = 250;

    @Value("${job.file.rollover.ndjson:200}")
    private long ndjsonRollOver;

    @Value("${job.file.rollover.zip:200}")
    private long zipRollOver;

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
    private final OptOutRepository optOutRepository;


    /**
     * Process the contract - retrieve all the patients for the contract and create a thread in the
     * patientProcessorThreadPool to handle searching for EOBs for each patient. Periodically check to
     * see if the job is cancelled and cancel the threads if necessary, otherwise, wait until all threads
     * have processed.
     *
     * @param contractData - the contract data (contract, progress tracker, attested time, writer)
     * @return - the job output records containing the file information
     */
    public List<JobOutput> process(Path outputDirPath, ContractData contractData, FileOutputType outputType) {

        var contract = contractData.getContract();
        log.info("Beginning to process contract {}", keyValue(CONTRACT_LOG, contract.getContractName()));

        var contractNumber = contract.getContractNumber();

        var progressTracker = contractData.getProgressTracker();

        var patients = getPatientsByContract(contractNumber, progressTracker);
        int patientCount = patients.size();
        log.info("Contract [{}] has [{}] Patients", contractNumber, patientCount);

        boolean isCancelled = false;

        StreamHelper helper = null;
        try {
            helper = createOutputHelper(outputDirPath, contractNumber, outputType);

            int recordsProcessedCount = 0;
            var futureHandles = new ArrayList<Future<Void>>();
            for (PatientDTO patient : patients) {
                ++recordsProcessedCount;

                if (isOptOutPatient(patient.getPatientId())) {
                    // this patient has opted out. skip patient record.
                    continue;
                }

                futureHandles.add(processPatient(contractData, contract, progressTracker, helper, patient));

                // Periodically check if cancelled
                if (recordsProcessedCount % cancellationCheckFrequency == 0) {

                    var jobUuid = contractData.getProgressTracker().getJobUuid();
                    isCancelled = hasJobBeenCancelled(jobUuid);
                    if (isCancelled) {
                        log.warn("Job [{}] has been cancelled. Attempting to stop processing the job shortly ... ", jobUuid);
                        cancelFuturesInQueue(futureHandles);
                        break;
                    }

                    processHandles(futureHandles, progressTracker);
                }
            }
            awaitTermination(progressTracker, futureHandles);

        } finally {
            close(helper);
        }

        handleCancellation(isCancelled);

        // All jobs are done, return the job output records
        return createJobOutputs(helper.getDataFiles(), helper.getErrorFiles());
    }

    private StreamHelper createOutputHelper(Path outputDirPath, String contractNumber, FileOutputType outputType) {
        try {
            if (outputType == ZIP) {
                return new ZipStreamHelperImpl(outputDirPath, contractNumber, getZipRolloverThreshold(), getRollOverThreshold(), tryLockTimeout);
            } else {
                return new TextStreamHelperImpl(outputDirPath, contractNumber, getRollOverThreshold(), tryLockTimeout);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * While there are still patient records in progress, sleep for a bit and check progress
     * @param progressTracker
     * @param futureHandles
     */
    private void awaitTermination(ProgressTracker progressTracker, ArrayList<Future<Void>> futureHandles) {
        while (!futureHandles.isEmpty()) {
            sleep();
            processHandles(futureHandles, progressTracker);
        }
    }

    private void close(StreamHelper helper) {
        if (helper != null) {
            try {
                helper.close();
            } catch (Exception ex) {
                log.error("Unable to close the helper", ex);
            }
        }
    }

    private void handleCancellation(boolean isCancelled) {
        if (isCancelled) {
            final String errMsg = "Job was cancelled while it was being processed";
            log.warn("{}", errMsg);
            throw new JobCancelledException(errMsg);
        }
    }

    /**
     * Create a token from newRelic for the transaction.
     *
     * On using new-relic tokens with async calls
     * See https://docs.newrelic.com/docs/agents/java-agent/async-instrumentation/java-agent-api-asynchronous-applications
     *
     * @param contractData
     * @param contract
     * @param progressTracker
     * @param helper
     * @param patient
     * @return
     */
    private Future<Void> processPatient(ContractData contractData, Contract contract, ProgressTracker progressTracker, StreamHelper helper, PatientDTO patient) {
        final Token token = NewRelic.getAgent().getTransaction().getToken();

        // Using a ThreadLocal to communicate contract number to RoundRobinBlockingQueue
        // could be viewed as a hack by many; but on the other hand it saves us from writing
        // tons of extra code.
        RoundRobinBlockingQueue.CATEGORY_HOLDER.set(progressTracker.getJobUuid());
        try {
            var attestedOn = contract.getAttestedOn();
            var sinceTime = contractData.getSinceTime();
            return patientClaimsProcessor.process(patient, helper, attestedOn, sinceTime, token);

        } finally {
            RoundRobinBlockingQueue.CATEGORY_HOLDER.remove();
        }

    }

    /**
     * Retrieve the patients by contract
     *
     * @param contractNumber - the contract number
     * @param progressTracker - the progress tracker for all contracts and patients for the job
     * @return the contract's patients
     */
    private List<PatientDTO> getPatientsByContract(String contractNumber, ProgressTracker progressTracker) {
        return progressTracker.getPatientsByContracts()
                .stream()
                .filter(byContract -> byContract.getContractNumber().equals(contractNumber))
                .findFirst()
                .map(GetPatientsByContractResponse::getPatients)
                .orElse(Collections.emptyList());
    }


    /**
     * Return the number of bytes when to rollover given the number of megabytes in a zip file if used
     *
     * @return the number of bytes
     */
    private long getZipRolloverThreshold() {
        return zipRollOver * Constants.ONE_MEGA_BYTE;
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
     * Check the opt out repository to see if a patient has opted out of data services
     *
     * @param patientId - the patient id
     * @return true if the patient has opted out
     */
    private boolean isOptOutPatient(String patientId) {

        final List<OptOut> optOuts = optOutRepository.findByCcwId(patientId);
        if (optOuts.isEmpty()) {
            // No opt-out record found for this patient - Opt-In by default.
            return false;
        }

        // opt-out record has an effective date.
        // if any of the opt-out records for a patient is effective as of today or earlier, the patient has opted-out
        final LocalDate tomorrow = LocalDate.now().plusDays(1);
        return optOuts.stream()
                .anyMatch(optOut -> optOut.getEffectiveDate().isBefore(tomorrow));
    }

    /**
     * Cancel threads
     *
     * @param futureHandles - the running threads
     */
    private void cancelFuturesInQueue(List<Future<Void>> futureHandles) {

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
     * @param futureHandles - the thread futures
     * @param progressTracker - the tracker with updated tracker information
     */
    private void processHandles(List<Future<Void>> futureHandles, ProgressTracker progressTracker) {
        var iterator = futureHandles.iterator();
        while (iterator.hasNext()) {
            var future = iterator.next();
            if (future.isDone()) {
                processFuture(futureHandles, progressTracker, future);
                iterator.remove();
            }
        }

        // update the progress in the DB & logs periodically
        trackProgress(progressTracker);
    }

    /**
     * process the future that is marked as done.
     * On doing a get(), if an exception is thrown, analyze it to decide whether to stop the batch or not.
     * @param futureHandles - List of Futures
     * @param progressTracker - progress tracker instance
     * @param future - a specific future
     */
    private void processFuture(List<Future<Void>> futureHandles, ProgressTracker progressTracker, Future<Void> future) {
        progressTracker.incrementProcessedCount();
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            analyzeException(futureHandles, progressTracker, e);

        } catch (CancellationException e) {
            // This could happen in the rare event that a job was cancelled mid-process.
            // due to which the futures in the queue (that were not yet in progress) were cancelled.
            // Nothing to be done here
            log.warn("CancellationException while calling Future.get() - Job may have been cancelled");
        }
    }

    private void analyzeException(List<Future<Void>> futureHandles, ProgressTracker progressTracker, Exception e) {
        progressTracker.incrementFailureCount();

        if (progressTracker.isErrorCountBelowThreshold()) {
            final Throwable rootCause = ExceptionUtils.getRootCause(e);
            log.error("exception while processing patient {}", rootCause.getMessage(), rootCause);
            // log exception, but continue processing job as errorCount is below threshold
        } else {
            cancelFuturesInQueue(futureHandles);
            log.error("{} out of {} records failed. Stopping job", progressTracker.getFailureCount(), progressTracker.getTotalCount());
            throw new RuntimeException("Too many patient records in the job had failures");
        }
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
     * @param dataFiles - the results of writing the contract
     * @param errorFiles - any errors that arose due to writing the contract
     * @return the list of job output objects
     */
    private List<JobOutput> createJobOutputs(List<Path> dataFiles, List<Path> errorFiles) {

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
     * @param isError - if there was an error
     * @return - the joub output object
     */
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
