package gov.cms.ab2d.worker.processor;

import ca.uhn.fhir.context.FhirContext;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.model.*;
import gov.cms.ab2d.common.repository.JobOutputRepository;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.util.Constants;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.*;
import gov.cms.ab2d.filter.FilterOutByDate;
import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneficiaries;
import gov.cms.ab2d.worker.config.RoundRobinBlockingQueue;
import gov.cms.ab2d.worker.processor.domainmodel.*;
import gov.cms.ab2d.worker.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static gov.cms.ab2d.common.model.JobStatus.CANCELLED;
import static gov.cms.ab2d.common.model.JobStatus.SUCCESSFUL;
import static gov.cms.ab2d.common.util.Constants.EOB;
import static gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneficiaries.*;
import static gov.cms.ab2d.worker.processor.StreamHelperImpl.FileOutputType.NDJSON;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobProcessorImpl implements JobProcessor {
    private static final int SLEEP_DURATION = 250;

    @Value("${job.file.rollover.ndjson:200}")
    private long ndjsonRollOver;

    @Value("${efs.mount}")
    private String efsMount;

    @Value("${audit.files.ttl.hours}")
    private int auditFilesTTLHours;

    /** Failure threshold an integer expressed as a percentage of failure tolerated in a batch **/
    @Value("${failure.threshold}")
    private int failureThreshold;

    @Value("${claims.skipBillablePeriodCheck}")
    private boolean skipBillablePeriodCheck;

    @Value("${file.try.lock.timeout}")
    private int tryLockTimeout;

    @Value("${bfd.earliest.data.date:01/01/2020}")
    private String startDate;
    @Value("${bfd.earliest.data.date.special.contracts}")
    private String startDateSpecialContracts;
    @Value("#{'${bfd.special.contracts}'.split(',')}")
    private List<String> specialContracts;

    private final FileService fileService;
    private final JobRepository jobRepository;
    private final JobOutputRepository jobOutputRepository;
    private final LogManager eventLogger;
    private final BFDClient bfdClient;
    private final PatientClaimsProcessor patientClaimsProcessor;
    private final FhirContext fhirContext;

    @Qualifier("patientContractThreadPool")
    private final ThreadPoolTaskExecutor patientContractThreadPool;
    /**
     * Load the job and process it
     *
     * @param jobUuid - the job id
     * @return the processed job
     */
    @Override
    @Transactional(propagation = Propagation.NEVER)
    @Trace(metricName = "Job Processing", dispatcher = true)
    public Job process(final String jobUuid) {

        // Load the job
        final Job job = jobRepository.findByJobUuid(jobUuid);
        log.info("Found job");

        // Determine the output directory based on the job id
        Path outputDirPath = null;
        try {
            outputDirPath = Paths.get(efsMount, jobUuid);
            processJob(job, outputDirPath);

        } catch (JobCancelledException e) {
            log.warn("Job: [{}] CANCELLED", jobUuid);

            if (outputDirPath != null) {
                log.info("Deleting output directory : {} ", outputDirPath.toAbsolutePath());
                deleteExistingDirectory(outputDirPath, job);
            }

        } catch (Exception e) {
            eventLogger.log(new JobStatusChangeEvent(
                    job.getUser() == null ? null : job.getUser().getUsername(),
                    job.getJobUuid(),
                    job.getStatus() == null ? null : job.getStatus().name(),
                    JobStatus.FAILED.name(), "Job Failed - " + e.getMessage()));

            log.error("Unexpected exception ", e);
            job.setStatus(JobStatus.FAILED);
            job.setStatusMessage(e.getMessage());
            job.setCompletedAt(OffsetDateTime.now());
            jobRepository.save(job);
            log.info("Job: [{}] FAILED", jobUuid);
        }

        return job;
    }

    /**
     * Process in individual contract
     *
     * @param contracts - all the contract to process
     * @param job - the job in which the contract belongs
     * @param month - the month to search for beneficiaries for
     * @param outputDirPath - the location of the job output
     * @param progressTracker - the progress tracker which indicates how far the job is along
     * @throws ExecutionException when there is an issue with searching
     * @throws InterruptedException - when the search is interrupted
     */
    private void processContracts(List<Contract> contracts, Job job, int month, Path outputDirPath,
                                  ProgressTracker progressTracker) throws ExecutionException, InterruptedException {
        List<ContractData> cData = new ArrayList<>();
        List<JobOutput> jobOutputs = new ArrayList<>();

        for (Contract contract : contracts) {
            String contractNum = contract.getContractNumber();
            ContractEobManager contractEobManager = new ContractEobManager(fhirContext, skipBillablePeriodCheck,
                    getStartDate(contractNum), contract.getAttestedOn());

            // Retrieve the contract beneficiaries
            //for (Contract contract : contracts) {
            try {
                // Init objects
                StreamHelper helper = new TextStreamHelperImpl(outputDirPath, contractNum, ndjsonRollOver * Constants.ONE_MEGA_BYTE, tryLockTimeout, eventLogger, job);
                ContractData contractData = new ContractData(contract, progressTracker, contract.getAttestedOn(), job.getSince(),
                        job.getUser() != null ? job.getUser().getUsername() : null, helper);
                cData.add(contractData);

                ContractBeneficiaries contractBeneficiaries = buildCB(contractNum);

                progressTracker.setCurrentMonth(month);

                List<Future<ContractMapping>> contractBeneFutureHandles = createAllContractMappingFutures(month, contractNum, contract.getAttestedOn());
                List<Future<EobSearchResponse>> eobFutureHandles = new ArrayList<>();

                 // Iterate through the futures
                while (!contractBeneFutureHandles.isEmpty() || !eobFutureHandles.isEmpty()) {
                    // See if there are any threads left to finish
                    Iterator<Future<ContractMapping>> contactBeneSearchIterator = contractBeneFutureHandles.iterator();
                    while (contactBeneSearchIterator.hasNext()) {
                        Future<ContractMapping> future = contactBeneSearchIterator.next();
                        // If it's done, claim the data and add it to the results, then remove it from the active threads
                        if (future.isDone()) {
                            ContractMapping contractMapping = getContractMappingFromFuture(future, contractNum);
                            if (contractMapping == null) {
                                continue;
                            }
                            progressTracker.incrementTotalContractBeneficiariesSearchFinished();
                            updateJobStatus(job, progressTracker);
                            Set<String> patients = contractMapping.getPatients();
                            eventLogger.log(new ReloadEvent(null, ReloadEvent.FileType.CONTRACT_MAPPING,
                                    "Contract: " + contractNum +
                                            " retrieved " + patients.size() + " contract beneficiaries for month " +
                                            contractMapping.getMonth(), patients.size()));
                            if (!patients.isEmpty()) {
                                List<PatientDTO> newPatients = addDateRangeToExistingOrNewPatient(contractBeneficiaries,
                                        toDateRange(contractMapping.getMonth()), patients);
                                newPatients.forEach(p -> {
                                    progressTracker.addPatientByContract(contractNum, p);
                                    eobFutureHandles.add(addPatientSearch(p, contractData));
                                });
                            }
                            contactBeneSearchIterator.remove();
                        }
                    }
                    processBeneFuturesList(eobFutureHandles, progressTracker, contractEobManager, helper);
                    updateJobStatus(job, progressTracker);
                    // If we haven't removed all items from the running futures lists, sleep for a bit
                    if (!contractBeneFutureHandles.isEmpty() || !eobFutureHandles.isEmpty()) {
                        sleepABit();
                    }
                }
                final JobStatus jobStatus = jobRepository.findJobStatus(progressTracker.getJobUuid());
                if (CANCELLED.equals(jobStatus)) {
                    cancelIt(contractBeneFutureHandles, eobFutureHandles, progressTracker.getJobUuid());
                }
                updateJobStatus(job, progressTracker);
            } catch (ExecutionException | InterruptedException ex) {
                log.error("Having issue retrieving patients for contract " + contractNum);
                throw ex;
            } catch (IOException e) {
                cData.forEach(c -> close(c.getHelper()));
                throw new UncheckedIOException(e);
            }
            eventLogger.log(new ContractBeneSearchEvent(job.getUser() == null ? null : job.getUser().getUsername(),
                    job.getJobUuid(),
                    contractNum,
                    progressTracker.getContractCount(contractNum),
                    progressTracker.getProcessedCount(),
                    progressTracker.getOptOutCount(),
                    progressTracker.getFailureCount()));
        }
        // final Segment contractSegment = NewRelic.getAgent().getTransaction().startSegment("Patient processing of contract " + contractNum);
        // contractSegment.end();

        if (contracts.size() > 0) {
            cData.forEach(c -> close(c.getHelper()));

            // All jobs are done, return the job output records
            cData.forEach(c -> jobOutputs.addAll(createJobOutputs(c.getHelper().getDataFiles(), false)));
            cData.forEach(c -> jobOutputs.addAll(createJobOutputs(c.getHelper().getErrorFiles(), true)));
            if (jobOutputs.isEmpty()) {
                var errMsg = "The export process has produced no results";
                throw new RuntimeException(errMsg);
            }
            // For each job output, add to the job and save the result
            jobOutputs.forEach(job::addJobOutput);
            jobOutputRepository.saveAll(jobOutputs);
        }
    }

    private void cancelIt(List<Future<ContractMapping>> contractBeneFutureHandles, List<Future<EobSearchResponse>> eobFutureHandles,
                          String jobId) {
        log.warn("Job [{}] has been cancelled. Attempting to stop processing the job shortly ... ", jobId);
        contractBeneFutureHandles.parallelStream().forEach(f -> f.cancel(false));
        eobFutureHandles.parallelStream().forEach(f -> f.cancel(false));
        final String errMsg = "Job was cancelled while it was being processed";
        log.warn("{}", errMsg);
        throw new JobCancelledException(errMsg);
    }

    private void sleepABit() {
        try {
            Thread.sleep(SLEEP_DURATION);
        } catch (InterruptedException e) {
            log.warn("interrupted exception in thread.sleep(). Ignoring");
        }
    }

    private ContractBeneficiaries buildCB(String contractNumber) {
        ContractBeneficiaries contractBeneficiaries = new ContractBeneficiaries();
        contractBeneficiaries.setContractNumber(contractNumber);
        contractBeneficiaries.setPatients(new HashMap<>());
        return contractBeneficiaries;
    }

    List<Future<ContractMapping>> createAllContractMappingFutures(int month, String contractNumber, OffsetDateTime attestDate) {
        List<Future<ContractMapping>> contractBeneFutureHandles = new ArrayList<>();

        OffsetDateTime dateStub = OffsetDateTime.now().withDayOfMonth(1);
        for (var m = 1; m <= month; m++) {
            OffsetDateTime dateToCheck = dateStub.withMonth(m);
            if (attestDate.isBefore(dateToCheck)) {
                PatientContractCallable callable = new PatientContractCallable(m, contractNumber, bfdClient);
                contractBeneFutureHandles.add(patientContractThreadPool.submit(callable));
            }
        }
        return contractBeneFutureHandles;
    }

    private void processBeneFuturesList(List<Future<EobSearchResponse>> benes, ProgressTracker progressTracker,
                                                  ContractEobManager contractEobManager, StreamHelper helper) {
        EobSearchResponse response = null;
        Iterator<Future<EobSearchResponse>> beneIterator = benes.iterator();
        while (beneIterator.hasNext()) {
            var future = beneIterator.next();
            if (future.isDone()) {
                progressTracker.incrementProcessedCount();
                try {
                    response = future.get();
                    contractEobManager.addResources(response);
                    contractEobManager.validateResources();
                    contractEobManager.writeValidEobs(helper);
                } catch (IOException e) {
                    String errorMsg = "Unable to write to EOB data file " + e.getLocalizedMessage();
                    log.error(errorMsg, e);
                    throw new RuntimeException(errorMsg);
                } catch (InterruptedException | ExecutionException e) {
                    progressTracker.incrementFailureCount();
                    if (progressTracker.isErrorCountBelowThreshold()) {
                        final Throwable rootCause = ExceptionUtils.getRootCause(e);
                        log.error("exception while processing patient {}", rootCause.getMessage(), rootCause);
                        // log exception, but continue processing job as errorCount is below threshold
                    } else {
                        benes.parallelStream().forEach(f -> f.cancel(false));
                        String description = progressTracker.getFailureCount() + " out of " + progressTracker.getTotalCount() + " records failed. Stopping job";
                        eventLogger.log(new ErrorEvent(null, progressTracker.getJobUuid(),
                                ErrorEvent.ErrorType.TOO_MANY_SEARCH_ERRORS, description));
                        log.error("{} out of {} records failed. Stopping job", progressTracker.getFailureCount(), progressTracker.getTotalCount());
                        throw new RuntimeException("Too many patient records in the job had failures");
                    }
                } catch (CancellationException e) {
                    // This could happen in the rare event that a job was cancelled mid-process.
                    // due to which the futures in the queue (that were not yet in progress) were cancelled.
                    // Nothing to be done here
                    log.warn("CancellationException while calling Future.get() - Job may have been cancelled");
                }
                beneIterator.remove();
            }
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

    /**
     * Create a token from newRelic for the transaction.
     *
     * On using new-relic tokens with async calls
     * See https://docs.newrelic.com/docs/agents/java-agent/async-instrumentation/java-agent-api-asynchronous-applications
     *
     * @param patient - process to process
     * @param contractData - the contract data information
     * @return a Future<Void>
     */
    private Future<EobSearchResponse> addPatientSearch(PatientDTO patient, ContractData contractData) {
        final Token token = NewRelic.getAgent().getTransaction().getToken();

        // Using a ThreadLocal to communicate contract number to RoundRobinBlockingQueue
        // could be viewed as a hack by many; but on the other hand it saves us from writing
        // tons of extra code.
        var jobUuid = contractData.getProgressTracker().getJobUuid();
        RoundRobinBlockingQueue.CATEGORY_HOLDER.set(jobUuid);
        try {
            var patientClaimsRequest = new PatientClaimsRequest(patient,
                    contractData.getUserId(),
                    jobUuid,
                    contractData.getContract() != null ? contractData.getContract().getContractNumber() : null,
                    token);
            return patientClaimsProcessor.process(patientClaimsRequest);

        } finally {
            RoundRobinBlockingQueue.CATEGORY_HOLDER.remove();
        }
    }

    private void updateJobStatus(Job job, ProgressTracker tracker) {
        int progress = tracker.getPercentageCompleted();
        job.setProgress(progress);
        job.setStatusMessage(progress + "% complete");
        jobRepository.save(job);
    }

    /**
     * Once the job writer is finished, create a list of job output objects with
     * the data files and the error files
     *
     * @param files - the results of writing the contract
     * @param isError - If we are creating an error output list
     * @return the list of job output objects
     */
    private List<JobOutput> createJobOutputs(List<Path> files, boolean isError) {
        final List<JobOutput> jobOutputs = new ArrayList<>();
        for (Path p : files) {
            JobOutput jobOutput = new JobOutput();
            jobOutput.setFilePath(p.getFileName().toString());
            jobOutput.setFhirResourceType(EOB);
            jobOutput.setError(isError);
            jobOutput.setChecksum(fileService.generateChecksum(p.toFile()));
            jobOutput.setFileLength(p.toFile().length());
            jobOutputs.add(jobOutput);
        }
        return jobOutputs;
    }

    /**
     * Process the Job and put the contents into the output directory
     *
     * @param job - the job to process
     * @param outputDirPath - the output directory to put all the files
     */
    private void processJob(Job job, Path outputDirPath) throws ExecutionException, InterruptedException {
        // Create the output directory
        createOutputDirectory(outputDirPath, job);
        int month = LocalDate.now().getMonthValue();

        // Get all attested contracts for that job (or the one specified in the job)
        var attestedContracts = getAttestedContracts(job);
        // Retrieve the patients for each contract and start a progress tracker
        ProgressTracker progressTracker = ProgressTracker.builder()
                .jobUuid(job.getJobUuid())
                .numContracts(attestedContracts.size())
                .failureThreshold(failureThreshold)
                .currentMonth(month)
                .build();

        // Retrieve the contract beneficiaries
        try {
            processContracts(attestedContracts, job, month, outputDirPath, progressTracker);
        } catch (ExecutionException | InterruptedException ex) {
            log.error("Having issue retrieving patients for contract " + attestedContracts.stream().map(Contract::getContractNumber).collect(Collectors.joining()));
            throw ex;
        }

        eventLogger.log(new JobStatusChangeEvent(
                job.getUser() == null ? null : job.getUser().getUsername(),
                job.getJobUuid(),
                job.getStatus() == null ? null : job.getStatus().name(),
                JobStatus.SUCCESSFUL.name(), "Job Finished"));
        job.setStatus(SUCCESSFUL);
        job.setStatusMessage("100%");
        job.setProgress(100);
        job.setExpiresAt(OffsetDateTime.now().plusHours(auditFilesTTLHours));
        job.setCompletedAt(OffsetDateTime.now());

        jobRepository.save(job);
        log.info("Job: [{}] is DONE", job.getJobUuid());
    }

    /**
     * Given a path to a directory, create it. If it already exists, delete it and its contents and recreate it
     *
     * @param outputDirPath - the path to the output directory you want to create
     */
    private void createOutputDirectory(Path outputDirPath, Job job) {
        Path directory;
        try {
            directory = fileService.createDirectory(outputDirPath);
        } catch (UncheckedIOException e) {
            final IOException cause = e.getCause();
            if (cause != null && cause.getMessage().equalsIgnoreCase("Directory already exists")) {
                log.warn("Directory already exists. Delete and create afresh ...");
                deleteExistingDirectory(outputDirPath, job);
                directory = fileService.createDirectory(outputDirPath);
            } else {
                throw e;
            }
        }

        log.info("Created job output directory: {}", directory.toAbsolutePath());
    }

    /**
     * Delete directory with all the ndjson files or zip files. If the files are directories or symbolic links, write
     * error, but continue. If it's a regular file, delete it, then delete the directory. If the directory is not
     * empty, throws an exception (if it has files other than ndjson or zip in it)
     *
     * @param outputDirPath - the directory to delete
     */
    private void deleteExistingDirectory(Path outputDirPath, Job job) {
        final File[] files = outputDirPath.toFile().listFiles((dir, name) -> name.toLowerCase().endsWith(NDJSON.getSuffix()));
        if (files == null) {
            return;
        }
        for (File file : files) {
            final Path filePath = file.toPath();
            if (file.isDirectory() || Files.isSymbolicLink(filePath)) {
                var errMsg = "File is not a regular file";
                log.error("{} - isDirectory: {}", errMsg, file.isDirectory());
                continue;
            }

            if (Files.isRegularFile(filePath)) {
                eventLogger.log(new FileEvent(
                        job == null || job.getUser() == null ? null : job.getUser().getUsername(),
                        job == null ? null : job.getJobUuid(),
                        filePath.toFile(), FileEvent.FileStatus.DELETE));

                doDelete(filePath);
            }
        }

        doDelete(outputDirPath);
    }

    private void doDelete(Path path) {
        try {
            Files.delete(path);
        } catch (IOException ex) {
            var errMsg = "Could not delete ";
            log.error("{} : {} ", errMsg, path.toAbsolutePath());
            throw new UncheckedIOException(errMsg + path.toFile().getName(), ex);
        }
    }

    /**
     * Return the list of attested contracts for a job. If a contract was specified in the job, just return that
     * after checking to make sure the sponsor has access to the contract, otherwise, search for all the contracts
     * for the sponsor
     *
     * @param job - the submitted job
     * @return the list of contracts (all or only 1 if the contract was specified in the job).
     */
    private List<Contract> getAttestedContracts(Job job) {

        // Get the aggregated attested Contracts for the sponsor
        final Sponsor sponsor = job.getUser().getSponsor();
        final List<Contract> attestedContracts = sponsor.getAggregatedAttestedContracts();

        // If a contract was specified for request, make sure the sponsor can access the contract and then return only it
        final Contract jobSpecificContract = job.getContract();
        if (jobSpecificContract != null && jobSpecificContract.getAttestedOn() != null) {
            boolean ownsContract = attestedContracts.stream()
                    .anyMatch(c -> jobSpecificContract.getContractNumber().equalsIgnoreCase(c.getContractNumber()));
            if (!ownsContract) {
                log.info("Job [{}] submitted for a specific attested contract [{}] that the sponsor [{}] does not own",
                        job.getJobUuid(), jobSpecificContract.getContractNumber(), sponsor.getOrgName());
            }
            log.info("Job [{}] submitted for a specific attested contract [{}] ", job.getJobUuid(), jobSpecificContract.getContractNumber());
            return Collections.singletonList(jobSpecificContract);
        }

        // Otherwise, return the list of attested contracts
        log.info("Job [{}] has [{}] attested contracts", job.getJobUuid(), attestedContracts.size());
        return attestedContracts;
    }

    /**
     * Given the ordinal for a month,
     * creates a date range from the start of the month to the end of the month for the current year
     *
     * @param month - the month of the year, 1-12
     * @return a DateRange - the date range created
     */
    private FilterOutByDate.DateRange toDateRange(int month) {
        FilterOutByDate.DateRange dateRange = null;
        try {
            dateRange = FilterOutByDate.getDateRange(month, LocalDate.now().getYear());
        } catch (ParseException e) {
            log.error("unable to create Date Range ", e);
            //ignore
        }

        return dateRange;
    }

    /**
     * Retrieve the data from the future after it's "done"
     *
     * @param future     - the future
     * @param contractId - the contract number
     * @return - the mapping
     */
    private ContractMapping getContractMappingFromFuture(Future<ContractMapping> future, String contractId) throws ExecutionException, InterruptedException {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            log.warn("InterruptedException while calling Future.get() - Getting Mapping for " + contractId);
            throw e;
        } catch (CancellationException e) {
            // This could happen in the rare event that a job was cancelled mid-process.
            // due to which the futures in the queue (that were not yet in progress) were cancelled.
            // Nothing to be done here
            log.warn("CancellationException while calling Future.get() - Getting Mapping for " + contractId);
        }
        return null;
    }

    private List<PatientDTO> addDateRangeToExistingOrNewPatient(ContractBeneficiaries beneficiaries,
                                                    FilterOutByDate.DateRange monthDateRange, Set<String> bfdPatientIds) {
        if (beneficiaries == null) {
            return new ArrayList<>();
        }
        List<PatientDTO> newPatients = new ArrayList<>();
        Map<String, PatientDTO> patientDTOMap = beneficiaries.getPatients();
        for (String patientId : bfdPatientIds) {
            PatientDTO patientDTO = patientDTOMap.get(patientId);

            if (patientDTO != null) {
                // patient id was already active on this contract in previous month(s)
                // So just add this month to the patient's dateRangesUnderContract
                if (monthDateRange != null) {
                    patientDTO.getDateRangesUnderContract().add(monthDateRange);
                }
            } else {
                // new patient id.
                // Create a new PatientDTO for this patient
                // And then add this month to the patient's dateRangesUnderContract

                patientDTO = PatientDTO.builder()
                        .patientId(patientId)
                        .dateRangesUnderContract(new ArrayList<>())
                        .build();
                newPatients.add(patientDTO);
                if (monthDateRange != null) {
                    patientDTO.getDateRangesUnderContract().add(monthDateRange);
                }
                beneficiaries.getPatients().put(patientId, patientDTO);
            }
        }
        return newPatients;
    }

    private Date getStartDate(String contract) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

        String dateToUse = startDate;
        if (isContractSpecial(contract)) {
            dateToUse = startDateSpecialContracts;
        }
        Date date;
        try {
            date = sdf.parse(dateToUse);
        } catch (ParseException e) {
            LocalDateTime d = LocalDateTime.of(2020, 1, 1, 0, 0, 0, 0);
            date = new Date(d.toInstant(ZoneOffset.UTC).toEpochMilli());
        }
        return date;
    }

    private boolean isContractSpecial(String contract) {
        return this.specialContracts != null && !this.specialContracts.isEmpty() && specialContracts.contains(contract);
    }
}
