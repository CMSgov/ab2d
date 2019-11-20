package gov.cms.ab2d.worker.service;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobOutput;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.repository.JobOutputRepository;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.worker.adapter.bluebutton.BeneficiaryAdapter;
import gov.cms.ab2d.worker.adapter.bluebutton.BfdClientAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static gov.cms.ab2d.common.model.JobStatus.IN_PROGRESS;
import static gov.cms.ab2d.common.model.JobStatus.SUBMITTED;
import static gov.cms.ab2d.common.model.JobStatus.SUCCESSFUL;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobProcessingServiceImpl implements JobProcessingService {
    private static final String OUTPUT_FILE_SUFFIX = ".ndjson";
    private static final String ERROR_FILE_SUFFIX = "_error.ndjson";
    private static final int GROUP_SIZE = 5;

    @Value("${efs.mount}")
    private String efsMount;

    private final FhirContext fhirContext;
    private final FileService fileService;
    private final JobRepository jobRepository;
    private final JobOutputRepository jobOutputRepository;
    private final BeneficiaryAdapter beneficiaryAdapter;
    private final BfdClientAdapter  bfdClientAdapter;


    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE)
    public Job putJobInProgress(String jobId) {

        final Job job = jobRepository.findByJobUuid(jobId);
        Assert.notNull(job, String.format("Job %s not found", jobId));

        // validate status is SUBMITTED
        if (!SUBMITTED.equals(job.getStatus())) {
            final String errMsg = String.format("Job %s is not in %s status.", jobId, SUBMITTED);
            throw new IllegalArgumentException(errMsg);
        }

        job.setStatus(IN_PROGRESS);

        return jobRepository.save(job);
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public Job processJob(final String jobId) {

        final Job job = jobRepository.findByJobUuid(jobId);
        log.info("Found job : {} - {} - {} ", job.getId(), job.getJobUuid(), job.getStatus());

        final Sponsor sponsor = job.getUser().getSponsor();

        final List<Contract> attestedContracts = sponsor.getAttestedContracts();
        log.info("Job [{}] has [{}] attested contracts", job.getJobUuid(), attestedContracts.size());


        final Path outputDirPath = Paths.get(efsMount, job.getJobUuid());
        final Path outputDir = fileService.createDirectory(outputDirPath);

        for (Contract contract : attestedContracts) {
            log.info("Job [{}] - contract [{}] ", job.getJobUuid(), contract.getContractNumber());

            var jobOutputs = processContract(outputDir, contract);

            jobOutputs.forEach(jobOutput -> job.addJobOutput(jobOutput));
            jobOutputRepository.saveAll(jobOutputs);
        }

        completeJob(job);
        return job;
    }


    private List<JobOutput> processContract(final Path outputDir, Contract contract) {

        final var contractNumber = contract.getContractNumber();
        final var outputFile = fileService.createFile(outputDir, contractNumber + OUTPUT_FILE_SUFFIX);
        final var errorFile = fileService.createFile(outputDir, contractNumber + ERROR_FILE_SUFFIX);

        final var patientsByContract = beneficiaryAdapter.getPatientsByContract(contractNumber);
        final var patients = patientsByContract.getPatients();
        final int patientCount = patients.size();

        var futureResourcesHandles = patients.stream()
                .map(patient -> bfdClientAdapter.processPatient(patient.getPatientId(), outputFile, errorFile))
                .collect(Collectors.toList());

        int errorCount = processHandles(futureResourcesHandles);
        while (!futureResourcesHandles.isEmpty()) {
            sleep();
            errorCount += processHandles(futureResourcesHandles);
        }

        final List<JobOutput> jobOutputs = new ArrayList<>();
        if (errorCount < patientCount) {
            final JobOutput jobOutput = createPartialJobOutput(outputFile);
            jobOutput.setError(false);
            jobOutputs.add(jobOutput);
        }
        if (patientCount == 0 || errorCount > 0) {
            final JobOutput jobOutput = createPartialJobOutput(errorFile);
            jobOutput.setError(true);
            jobOutputs.add(jobOutput);
        }

        return jobOutputs;
    }

    private int processHandles(List<Future<String>> futureResourcesHandles) {
        int errorCount = 0;
        final Iterator<Future<String>> iterator = futureResourcesHandles.iterator();
        while (iterator.hasNext()) {
            final Future<String> future = iterator.next();
            if (future.isDone()) {
                try {
                    future.get();
                } catch (InterruptedException  e) {
                    ++errorCount;
                    log.error("interrupted excception while processing patient ", e);
                } catch (ExecutionException e) {
                    ++errorCount;
                    log.error("-------------------------------------------------");
                    log.error("exception while processing patient ", e.getCause());
                    log.error("-------------------------------------------------");
                }
                iterator.remove();
            }
        }
        return errorCount;
    }

    private JobOutput createPartialJobOutput(Path outputFile) {
        JobOutput jobOutput = new JobOutput();
        jobOutput.setFilePath(getEfsMountPath().relativize(outputFile).toString());
        jobOutput.setFhirResourceType("ExplanationOfBenefits");
        return jobOutput;
    }

    private void completeJob(Job job) {
        job.setStatus(SUCCESSFUL);
        job.setStatusMessage("100%");
        job.setExpiresAt(OffsetDateTime.now().plusDays(1));

        jobRepository.save(job);
        log.info("Job: [{}] is DONE", job.getId());
    }


    private Path getEfsMountPath() {
        return Paths.get(efsMount);
    }

    private void sleep() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            log.warn("interrupted exception in thread.sleep(). Ignoring");
        }
    }

}
