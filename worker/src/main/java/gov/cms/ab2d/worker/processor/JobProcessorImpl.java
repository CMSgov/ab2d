package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.worker.adapter.bluebutton.ContractAdapter;
import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse;
import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse.PatientDTO;
import gov.cms.ab2d.worker.processor.domainmodel.ContractData;
import gov.cms.ab2d.worker.processor.domainmodel.ProgressTracker;
import gov.cms.ab2d.worker.service.FileService;
import gov.cms.ab2d.worker.slice.PatientSliceCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static gov.cms.ab2d.common.model.JobStatus.CANCELLED;
import static gov.cms.ab2d.common.model.JobStatus.IN_PROGRESS;
import static gov.cms.ab2d.common.model.JobStatus.SUCCESSFUL;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobProcessorImpl implements JobProcessor {

    @Value("${efs.mount}")
    private String efsMount;

    private final FileService fileService;
    private final JobRepository jobRepository;
    private final ContractAdapter contractAdapter;
    private final PatientSliceCreator sliceCreator;
    private final PatientSliceProcessor patientSliceProcessor;


    @Override
    @Transactional(propagation = Propagation.NEVER)
    public Job process(final String jobUuid) {

        final Job job = jobRepository.findByJobUuid(jobUuid);
        log.info("Found job [{}]", jobUuid);

        try {
            Path outputDirPath = Paths.get(efsMount, jobUuid);
            processJob(job, outputDirPath);
        } catch (Exception e) {
            log.error("Unexpected exception ", e);
            job.setStatus(JobStatus.FAILED);
            job.setStatusMessage(e.getMessage());
            job.setCompletedAt(OffsetDateTime.now());
            jobRepository.save(job);
            log.info("Job: [{}] FAILED", jobUuid);
        }

        return job;
    }

    private void processJob(Job job, Path outputDirPath) {
        var outputDir = createOutputDirectory(outputDirPath);

        var attestedContracts = getAttestedContracts(job);
        var jobUuid = job.getJobUuid();
        var progressTracker = initializeProgressTracker(jobUuid, attestedContracts);

        for (Contract contract : attestedContracts) {
            log.info("Job [{}] - contract [{}] ", jobUuid, contract.getContractNumber());
            var contractData = new ContractData(outputDir, contract, progressTracker);
            processContract(contractData);
        }

        final JobStatus jobStatus = jobRepository.findJobStatus(jobUuid);
        if (IN_PROGRESS.equals(jobStatus)) {
            completeJob(job);
        } else if (CANCELLED.equals(jobStatus)) {
            log.warn("Job: [{}] CANCELLED", jobUuid);
            log.info("Deleting output directory : {} ", outputDirPath.toAbsolutePath());
            deleteExistingDirectory(outputDirPath);
        }
    }


    private Path createOutputDirectory(Path outputDirPath) {
        Path directory = null;
        try {
            directory = fileService.createDirectory(outputDirPath);
        } catch (UncheckedIOException e) {
            final IOException cause = e.getCause();
            if (cause != null && cause.getMessage().equalsIgnoreCase("Directory already exists")) {
                log.warn("Directory already exists. Delete and create afresh ...");
                deleteExistingDirectory(outputDirPath);
                directory = fileService.createDirectory(outputDirPath);
            } else {
                throw e;
            }
        }
        return directory;
    }

    private void deleteExistingDirectory(Path outputDirPath) {
        final File[] files = outputDirPath.toFile()
                .listFiles((dir, name) -> name.toLowerCase().endsWith(".ndjson"));

        for (File file : files) {
            final Path filePath = file.toPath();
            if (file.isDirectory() || Files.isSymbolicLink(filePath)) {
                var errMsg = "File is not a regular file";
                log.error("{} - isDirectory: {}", errMsg, file.isDirectory());
                continue;
            }

            if (Files.isRegularFile(filePath)) {
                try {
                    Files.delete(filePath);
                } catch (IOException ex) {
                    var errMsg = "Could not delete file ";
                    log.error("{} : {}", errMsg, filePath.toAbsolutePath());
                    throw new UncheckedIOException(errMsg + filePath.toFile().getName(), ex);
                }
            }
        }

        try {
            Files.delete(outputDirPath);
        } catch (IOException ex) {
            var errMsg = "Could not delete directory ";
            log.error("{} : {} ", errMsg, outputDirPath.toAbsolutePath());
            throw new UncheckedIOException(errMsg + outputDirPath.toFile().getName(), ex);
        }
    }

    private List<Contract> getAttestedContracts(Job job) {

        // when the job is submitted for a specific contract, process the export for only that contract.
        final Contract jobSpecificContract = job.getContract();
        if (jobSpecificContract != null && jobSpecificContract.getAttestedOn() != null) {
            log.info("Job [{}] submitted for a specific attested contract [{}] ", jobSpecificContract.getContractNumber());
            return Collections.singletonList(jobSpecificContract);
        }

        //Job does not specify a contract.
        //Get the aggregated attested Contracts for the sponsor & process all of them
        final Sponsor sponsor = job.getUser().getSponsor();
        final List<Contract> attestedContracts = sponsor.getAggregatedAttestedContracts();

        log.info("Job [{}] has [{}] attested contracts", job.getJobUuid(), attestedContracts.size());
        return attestedContracts;
    }


    /**
     * iterates through each contract,
     * calls the contract Adapter
     * to make a list of contracts to process.
     *
     * @param attestedContracts
     * @return
     */
    private ProgressTracker initializeProgressTracker(String jobUuid, List<Contract> attestedContracts) {
        return ProgressTracker.builder()
                .jobUuid(jobUuid)
                .patientsByContracts(fetchPatientsForAllContracts(attestedContracts))
                .build();
    }

    private List<GetPatientsByContractResponse> fetchPatientsForAllContracts(List<Contract> attestedContracts) {
        return attestedContracts
                .stream()
                .map(contract -> contract.getContractNumber())
                .map(contractNumber -> contractAdapter.getPatients(contractNumber))
                .collect(Collectors.toList());
    }


    private void processContract(ContractData contractData) {
        var startedAt = Instant.now();
        final boolean isJobStillInProgress = isJobStillInProgress(contractData);
        if (!isJobStillInProgress) {
            return;
        }

        var contractNumber = contractData.getContract().getContractNumber();
        var progressTracker = contractData.getProgressTracker();

        var patientsByContract = getPatientsByContract(contractNumber, progressTracker);
        var patients = patientsByContract.getPatients();

        final Map<Integer, List<PatientDTO>> slices = sliceCreator.createSlices(patients);
        log.info("Contract [{}] with [{}] has been sliced into [{}] slices", contractNumber, patients.size(), slices.size());

        var futureHandles = slices.entrySet().stream()
                .map(slice -> patientSliceProcessor.process(slice, contractData))
                .collect(Collectors.toList());

        var allFutures = CompletableFuture.allOf(futureHandles.toArray(new CompletableFuture[futureHandles.size()]));

        log.info("Waiting on allFutures to complete . .. ... ");
        try {
            allFutures.get();
        } catch (InterruptedException e) {
            //should not happen
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            //should not happen
            log.error("ExecutionException ", e);
            throw new RuntimeException(e.getCause());
        }

        log.info("allFutures.get() is DONE.");
        logTimeTaken(contractNumber, startedAt);
    }

    private boolean isJobStillInProgress(ContractData contractData) {
        var jobUuid = contractData.getProgressTracker().getJobUuid();
        var jobStatus = jobRepository.findJobStatus(jobUuid);
        if (IN_PROGRESS.equals(jobStatus)) {
            return true;
        }

        var errMsg = "Job [%s] is no longer in progress. It is in [%s] status. Stopping processing";
        log.warn("{}", String.format(errMsg, jobUuid, jobStatus));
        return false;
    }

    private GetPatientsByContractResponse getPatientsByContract(String contractNumber, ProgressTracker progressTracker) {
        return progressTracker.getPatientsByContracts()
                .stream()
                .filter(byContract -> byContract.getContractNumber().equals(contractNumber))
                .findFirst()
                .get();
    }

    private void logTimeTaken(String contractNumber, Instant start) {
        var timeTaken = Duration.between(start, Instant.now()).toSeconds();
        log.info("Processed contract[{}] in [{}] seconds", contractNumber, timeTaken);
    }


    private void completeJob(Job job) {
        job.setStatus(SUCCESSFUL);
        job.setStatusMessage("100%");
        job.setProgress(100);
        job.setExpiresAt(OffsetDateTime.now().plusDays(1));
        job.setCompletedAt(OffsetDateTime.now());

        jobRepository.saveAndFlush(job);
        log.info("Job: [{}] is DONE", job.getJobUuid());
    }


}
