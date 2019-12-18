package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.JobOutput;
import gov.cms.ab2d.common.model.User;
import gov.cms.ab2d.common.repository.JobRepository;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.repository.ContractRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class JobServiceImpl implements JobService {

    @Autowired
    private UserService userService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Value("${efs.mount}")
    private String fileDownloadPath;

    public static final String INITIAL_JOB_STATUS_MESSAGE = "0%";

    @Override
    public Job createJob(String resourceTypes, String url) {
        return createJob(resourceTypes, url, null);
    }

    @Override
    public Job createJob(String resourceTypes, String url, String contractNumber) {
        Job job = new Job();
        job.setResourceTypes(resourceTypes);
        job.setJobUuid(UUID.randomUUID().toString());
        job.setRequestUrl(url);
        job.setStatus(JobStatus.SUBMITTED);
        job.setStatusMessage(INITIAL_JOB_STATUS_MESSAGE);
        job.setCreatedAt(OffsetDateTime.now());
        job.setProgress(0);
        job.setUser(userService.getCurrentUser());

        if (contractNumber != null) {
            contractRepository.findContractByContractNumber(contractNumber).ifPresentOrElse(contractFound -> {
                User user = userService.getCurrentUser();
                Sponsor userSponsor = user.getSponsor();

                List<Contract> contracts = userSponsor.getAggregatedAttestedContracts();
                if (!contracts.contains(contractFound)) {
                    log.error("No attested contract with contract number {} found for the user", contractFound.getContractNumber());
                    throw new InvalidContractException("No attested contract with contract number " + contractFound.getContractNumber() +
                        " found for the user");
                }

                job.setContract(contractFound);
            }, () -> {
                log.error("Contract {} was not found", contractNumber);
                throw new ResourceNotFoundException("Contract " + contractNumber + " was not found");
            });
        }

        return jobRepository.save(job);
    }

    @Override
    public void cancelJob(String jobUuid) {
        Job job = getJobByJobUuid(jobUuid);

        if (!job.getStatus().isCancellable()) {
            log.error("Job had a status of {} so it was not able to be cancelled", job.getStatus());
            throw new InvalidJobStateTransition("Job has a status of " + job.getStatus() + ", so it cannot be cancelled");
        }

        jobRepository.cancelJobByJobUuid(jobUuid);
    }

    @Override
    public Job getJobByJobUuid(String jobUuid) {
        Job job = jobRepository.findByJobUuid(jobUuid);

        if (job == null) {
            log.error("Job was searched for and was not found");
            throw new ResourceNotFoundException("No job with jobUuid " +  jobUuid + " was found");
        }

        return job;
    }

    @Override
    public Job updateJob(Job job) {
        return jobRepository.save(job);
    }

    @Override
    public Resource getResourceForJob(String jobUuid, String fileName) throws MalformedURLException {
        Job job = getJobByJobUuid(jobUuid);

        // Make sure that there is a path that matches a job output for the job they are requesting
        boolean jobOutputMatchesPath = false;
        for (JobOutput jobOutput : job.getJobOutputs()) {
            if (jobOutput.getFilePath().equals(fileName)) {
                jobOutputMatchesPath = true;
                break;
            }
        }

        if (!jobOutputMatchesPath) {
            log.error("No Job Output with the file name {} exists in our records", fileName);
            throw new ResourceNotFoundException("No Job Output with the file name " + fileName + " exists in our records");
        }

        Path file = Paths.get(fileDownloadPath + job.getJobUuid() + File.separator +  fileName);
        Resource resource = new UrlResource(file.toUri());

        if (!resource.exists()) {
            log.error("The job output exists in our records, but the file is not present on our system: {}", fileName);
            throw new JobOutputMissingException("The job output exists in our records, but the file is not present on our system: " + fileName);
        }

        return resource;
    }

    @Override
    public void deleteFileForJob(File file) {
        boolean deleted = file.delete();
        if (!deleted) {
            log.error("Was not able to delete the file {}", file.getName());
        }
    }

    @Override
    public boolean checkIfCurrentUserHasActiveJob() {
        User user = userService.getCurrentUser();
        List<Job> jobs = jobRepository.findActiveJobsByUser(user);
        return jobs.size() > 0;
    }

    @Override
    public boolean checkIfCurrentUserHasActiveJobForContractNumber(String contractNumber) {
        User user = userService.getCurrentUser();
        Contract contract = contractRepository.findContractByContractNumber(contractNumber)
            .orElseThrow(() -> {
                log.error("Contract number {} }was not found", contractNumber);
                return new ResourceNotFoundException("Contract number " + contractNumber + " was not found");
            });
        List<Job> jobs = jobRepository.findActiveJobsByUserAndContract(user, contract);
        return jobs.size() > 0;
    }
}
