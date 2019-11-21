package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobOutput;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.repository.JobOutputRepository;
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.repository.SponsorRepository;
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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
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

    @Value("${efs.mount}")
    private String efsMount;

    private final FileService fileService;
    private final JobRepository jobRepository;
    private final SponsorRepository sponsorRepository;
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
        log.info("Found job : {} - {}", job.getId(), job.getJobUuid());

        final Sponsor sponsor = job.getUser().getSponsor();

        final List<Contract> attestedContracts = getAggregatedAttestedContracts(sponsor);
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

    private List<Contract> getAggregatedAttestedContracts(Sponsor sponsor) {
        return Optional.of(sponsor)
                .filter(s -> s.getParent() == null)
                .map(s -> getChildrenContractsOfParent(s))
                .orElseGet(() -> sponsor.getAttestedContracts());
    }


    private List<Contract> getChildrenContractsOfParent(Sponsor sponsor) {
        final List<Sponsor> childrenSponsors = sponsorRepository.findByParent(sponsor);
        return childrenSponsors.stream()
                .map(s -> s.getAttestedContracts())
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }


    private List<JobOutput> processContract(final Path outputDir, Contract contract) {

        var contractNumber = contract.getContractNumber();
        var outputFile = fileService.createFile(outputDir, contractNumber + OUTPUT_FILE_SUFFIX);
        var errorFile = fileService.createFile(outputDir, contractNumber + ERROR_FILE_SUFFIX);

        var patientsByContract = beneficiaryAdapter.getPatientsByContract(contractNumber);
        var patients = patientsByContract.getPatients();
        int patientCount = patients.size();

        // A mutex lock that all threads for a contract uses while writing into the shared files
        var lock = new ReentrantLock();
        var futureResourcesHandles = patients.stream()
                .map(patient -> bfdClientAdapter.processPatient(patient.getPatientId(), lock, outputFile, errorFile))
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
        if (errorCount > 0) {
            final JobOutput jobOutput = createPartialJobOutput(errorFile);
            jobOutput.setError(true);
            jobOutputs.add(jobOutput);
        }

        return jobOutputs;
    }

    private int processHandles(List<Future<Integer>> futureResourcesHandles) {
        int errorCount = 0;

        var iterator = futureResourcesHandles.iterator();
        while (iterator.hasNext()) {
            var future = iterator.next();
            if (future.isDone()) {
                try {
                    var responseCount = future.get();
                    if (responseCount > 0) {
                        errorCount += responseCount;
                    }
                } catch (InterruptedException  e) {
                    ++errorCount;
                    log.error("interrupted exception while processing patient ", e);
                } catch (ExecutionException e) {
                    ++errorCount;
                    log.error("exception while processing patient ", e.getCause());
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
