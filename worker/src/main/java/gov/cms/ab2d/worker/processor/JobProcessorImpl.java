package gov.cms.ab2d.worker.processor;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.repository.JobOutputRepository;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.util.EventUtils;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.ContractBeneSearchEvent;
import gov.cms.ab2d.eventlogger.events.FileEvent;
import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneSearch;
import gov.cms.ab2d.worker.processor.domainmodel.ContractData;
import gov.cms.ab2d.worker.processor.domainmodel.ProgressTracker;
import gov.cms.ab2d.worker.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static gov.cms.ab2d.common.model.JobStatus.FAILED;
import static gov.cms.ab2d.common.model.JobStatus.SUCCESSFUL;
import static gov.cms.ab2d.worker.processor.StreamHelperImpl.FileOutputType.NDJSON;
import static gov.cms.ab2d.worker.processor.StreamHelperImpl.FileOutputType.ZIP;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobProcessorImpl implements JobProcessor {

    @Value("${efs.mount}")
    private String efsMount;

    @Value("${audit.files.ttl.hours}")
    private int auditFilesTTLHours;

    /** Failure threshold an integer expressed as a percentage of failure tolerated in a batch **/
    @Value("${failure.threshold}")
    private int failureThreshold;

    private final FileService fileService;
    private final JobRepository jobRepository;
    private final JobOutputRepository jobOutputRepository;
    private final ContractBeneSearch contractBeneSearch;
    private final ContractProcessor contractProcessor;
    private final LogManager eventLogger;

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
            eventLogger.log(EventUtils.getJobChangeEvent(job, FAILED, "Job Failed - " + e.getMessage()));
            log.error("Unexpected exception ", e);
            job.setStatus(FAILED);
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
     * @param contract - the contract to process
     * @param job - the job in which the contract belongs
     * @param month - the month to search for beneficiaries for
     * @param outputDirPath - the location of the job output
     * @param progressTracker - the progress tracker which indicates how far the job is along
     * @throws ExecutionException when there is an issue with searching
     * @throws InterruptedException - when the search is interrupted
     */
    void processContract(Contract contract, Job job, int month, Path outputDirPath, ProgressTracker progressTracker) throws ExecutionException, InterruptedException {
        log.info("Job [{}] - contract [{}] ", job.getJobUuid(), contract.getContractNumber());
        // Retrieve the contract beneficiaries
        try {
            processContractBenes(job, contract, month, progressTracker);
        } catch (ExecutionException | InterruptedException ex) {
            log.error("Having issue retrieving patients for contract " + contract.getContractNumber());
            throw ex;
        }

        // Create a holder for the contract, writer, progress tracker and attested date
        ContractData contractData = new ContractData(contract, progressTracker, job.getSince(),
                job.getUser() != null ? job.getUser().getUsername() : null);

        final Segment contractSegment = NewRelic.getAgent().getTransaction().startSegment("Patient processing of contract " + contract.getContractNumber());
        var jobOutputs = contractProcessor.process(outputDirPath, contractData);
        contractSegment.end();

        // For each job output, add to the job and save the result
        jobOutputs.forEach(job::addJobOutput);
        jobOutputRepository.saveAll(jobOutputs);

        eventLogger.log(new ContractBeneSearchEvent(job.getUser() == null ? null : job.getUser().getUsername(),
                job.getJobUuid(),
                contract.getContractNumber(),
                progressTracker.getContractCount(contract.getContractNumber()),
                progressTracker.getProcessedCount(),
                progressTracker.getFailureCount()));
    }

    void processContractBenes(Job job, Contract contract, int month, ProgressTracker progressTracker) throws ExecutionException, InterruptedException {
        try {
            progressTracker.addPatientsByContract(contractBeneSearch.getPatients(contract.getContractNumber(), month, progressTracker));
            int progress = progressTracker.getPercentageCompleted();
            job.setProgress(progress);
            job.setStatusMessage(progress + "% complete");
            jobRepository.save(job);
        } catch (ExecutionException | InterruptedException ex) {
            log.error("Having issue retrieving patients for contract " + contract.getContractNumber());
            throw ex;
        }
    }

    /**
     * Process the Job and put the contents into the output directory
     *
     * @param job - the job to process
     * @param outputDirPath - the output directory to put all the files
     */
    @SuppressFBWarnings("REC_CATCH_EXCEPTION")
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

        for (Contract contract : attestedContracts) {
            // Retrieve the contract beneficiaries
            try {
                processContract(contract, job, month, outputDirPath, progressTracker);
            } catch (ExecutionException | InterruptedException ex) {
                log.error("Having issue retrieving patients for contract " + contract);
                throw ex;
            }
        }

        completeJob(job);
    }

    /**
     * Given a path to a directory, create it. If it already exists, delete it and its contents and recreate it
     *
     * @param outputDirPath - the path to the output directory you want to create
     * @return the path to the newly created directory
     */
    private Path createOutputDirectory(Path outputDirPath, Job job) {
        Path directory = null;
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
        return directory;
    }

    /**
     * Delete directory with all the ndjson files or zip files. If the files are directories or symbolic links, write
     * error, but continue. If it's a regular file, delete it, then delete the directory. If the directory is not
     * empty, throws an exception (if it has files other than ndjson or zip in it)
     *
     * @param outputDirPath - the directory to delete
     */
    @SuppressFBWarnings
    private void deleteExistingDirectory(Path outputDirPath, Job job) {
        final File[] files = outputDirPath.toFile().listFiles(getFilenameFilter());

        for (File file : files) {
            final Path filePath = file.toPath();
            if (file.isDirectory() || Files.isSymbolicLink(filePath)) {
                var errMsg = "File is not a regular file";
                log.error("{} - isDirectory: {}", errMsg, file.isDirectory());
                continue;
            }

            if (Files.isRegularFile(filePath)) {
                eventLogger.log(EventUtils.getFileEvent(job, filePath.toFile(), FileEvent.FileStatus.DELETE));
                doDelete(filePath);
            }
        }

        doDelete(outputDirPath);
    }

    /**
     * @return a Filename filter for ndjson and zip files
     */
    private FilenameFilter getFilenameFilter() {
        return (dir, name) -> {
            final String filename = name.toLowerCase();
            final String ndjson = NDJSON.getSuffix();
            final String zip = ZIP.getSuffix();
            return filename.endsWith(ndjson) || filename.endsWith(zip);
        };
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
        if (jobSpecificContract != null && jobSpecificContract.hasAttestation()) {
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
     * Set the job as complete in the database
     *
     * @param job - The job to set as complete
     */
    private void completeJob(Job job) {
        eventLogger.log(EventUtils.getJobChangeEvent(job, SUCCESSFUL, "Job Finished"));
        job.setStatus(SUCCESSFUL);
        job.setStatusMessage("100%");
        job.setProgress(100);
        job.setExpiresAt(OffsetDateTime.now().plusHours(auditFilesTTLHours));
        job.setCompletedAt(OffsetDateTime.now());

        jobRepository.save(job);
        log.info("Job: [{}] is DONE", job.getJobUuid());
    }
}
