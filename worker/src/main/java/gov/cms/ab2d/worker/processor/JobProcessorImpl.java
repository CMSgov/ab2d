package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobProgress;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.repository.JobProgressRepository;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.worker.adapter.bluebutton.ContractAdapter;
import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse.PatientDTO;
import gov.cms.ab2d.worker.processor.contract.ContractSliceCreator;
import gov.cms.ab2d.worker.processor.contract.ContractSliceProcessor;
import gov.cms.ab2d.worker.processor.domainmodel.ContractData;
import gov.cms.ab2d.worker.processor.domainmodel.JobDM;
import gov.cms.ab2d.worker.processor.domainmodel.JobDM.ContractDM;
import gov.cms.ab2d.worker.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
    private final JobProgressRepository jobProgressRepository;
    private final ContractAdapter contractAdapter;
    private final ContractSliceCreator sliceCreator;
    private final ContractSliceProcessor sliceProcessor;


    @Override
    @Transactional(propagation = Propagation.NEVER)
    public Job process(final String jobUuid) {

        var job = jobRepository.findByJobUuid(jobUuid);
        log.info("Found job [{}]", jobUuid);

        try {
            var outputDirPath = Paths.get(efsMount, jobUuid);
            processJob(job, outputDirPath);
        } catch (Exception e) {
            log.error("Unexpected exception ", e);
            job.setStatus(JobStatus.FAILED);
            job.setStatusMessage(ExceptionUtils.getRootCauseMessage(e));
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
        var jobDM = createJobDomainModel(job, attestedContracts);

        for (Contract contract : attestedContracts) {
            log.info("Job [{}] - contract [{}] ", jobUuid, contract.getContractNumber());
            var contractData = new ContractData(outputDir, contract, jobDM);
            processContract(contractData);
        }

        var jobStatus = jobRepository.findJobStatus(jobUuid);
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
            var cause = e.getCause();
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
            var filePath = file.toPath();
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
        var jobSpecificContract = job.getContract();
        if (jobSpecificContract != null && jobSpecificContract.getAttestedOn() != null) {
            log.info("Job [{}] submitted for a specific attested contract [{}] ", jobSpecificContract.getContractNumber());
            return Collections.singletonList(jobSpecificContract);
        }

        //Job does not specify a contract.
        //Get the aggregated attested Contracts for the sponsor & process all of them
        var sponsor = job.getUser().getSponsor();
        var attestedContracts = sponsor.getAggregatedAttestedContracts();

        log.info("Job [{}] has [{}] attested contracts", job.getJobUuid(), attestedContracts.size());
        return attestedContracts;
    }


    private JobDM createJobDomainModel(Job job, List<Contract> attestedContracts) {
        return JobDM.builder()
                .jobId(job.getId())
                .jobUuid(job.getJobUuid())
                .contracts(buildContractDomainModels(job, attestedContracts))
                .build();
    }


    private List<ContractDM> buildContractDomainModels(Job job, List<Contract> attestedContracts) {
        return attestedContracts
                .stream()
                .map(contract -> fetchContractInfo(job, contract))
                .collect(Collectors.toList());
    }


    private ContractDM fetchContractInfo(Job job, Contract contract) {
        var contractNumber = contract.getContractNumber();
        var response = contractAdapter.getPatients(contractNumber);

        var slices = sliceCreator.createSlices(response.getPatients());
        for (var slice : slices.entrySet()) {
            createJobProgress(job, contract, slice);
        }

        return toContractDomainModel(contract, contractNumber, slices);
    }


    private void createJobProgress(Job job, Contract contract, Map.Entry<Integer, List<PatientDTO>> slice) {
        final JobProgress jobProgress = new JobProgress();
        jobProgress.setJob(job);
        jobProgress.setContract(contract);
        jobProgress.setSliceNumber(slice.getKey());
        jobProgress.setSliceTotal(slice.getValue().size());
        jobProgress.setRecordsProcessed(0);

        jobProgressRepository.saveAndFlush(jobProgress);
    }


    private ContractDM toContractDomainModel(Contract contract, String contractNumber, Map<Integer, List<PatientDTO>> slices) {
        return ContractDM.builder()
                .contractId(contract.getId())
                .contractNumber(contractNumber)
                .slices(slices)
                .build();
    }


    private void processContract(ContractData contractData) {
        var startedAt = Instant.now();
        var isJobStillInProgress = isJobStillInProgress(contractData);
        if (!isJobStillInProgress) {
            return;
        }

        var contractNumber = contractData.getContract().getContractNumber();
        var jobDM = contractData.getJobDM();
        var contractDM = getContractDomainModel(contractNumber, jobDM);
        logContractInfo(contractDM);

        var futures = contractDM.getSlices().entrySet().stream()
                .map(slice -> sliceProcessor.process(slice, contractData))
                .collect(Collectors.toList());

        CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[futures.size()]))
                .join();

        log.info("allFutures.join() is DONE.");
        logTimeTaken(contractNumber, startedAt);
    }


    private boolean isJobStillInProgress(ContractData contractData) {
        var jobUuid = contractData.getJobDM().getJobUuid();
        var jobStatus = jobRepository.findJobStatus(jobUuid);
        if (IN_PROGRESS.equals(jobStatus)) {
            return true;
        }

        var errMsg = "Job [%s] is no longer in progress. It is in [%s] status. Stopping processing";
        log.warn("{}", String.format(errMsg, jobUuid, jobStatus));
        return false;
    }


    private ContractDM getContractDomainModel(String contractNumber, JobDM jobDM) {
        return jobDM.getContracts().stream()
                .filter(cs -> cs.getContractNumber().equals(contractNumber))
                .findFirst()
                .get();
    }


    private void logContractInfo(ContractDM contractDM) {
        var patientCountPerContract = getPatientCountPerContract(contractDM);
        var sliceCount = contractDM.getSlices().size();

        log.info("Contract [{}] with [{}] has been sliced into [{}] slices", contractDM.getContractNumber(), patientCountPerContract, sliceCount);
    }


    private long getPatientCountPerContract(ContractDM contractDM) {
        return contractDM.getSlices().entrySet().stream()
                .map(e -> e.getValue())
                .mapToInt(e -> e.size())
                .sum();
    }


    private void logTimeTaken(String contractNumber, Instant start) {
        var timeTaken = Duration.between(start, Instant.now()).toSeconds();
        log.info("Processed contract [{}] in [{}] seconds", contractNumber, timeTaken);
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
