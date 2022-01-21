package gov.cms.ab2d.common.service;

import gov.cms.ab2d.common.model.*; // NOPMD
import gov.cms.ab2d.common.repository.JobRepository;
import gov.cms.ab2d.common.util.EventUtils;
import gov.cms.ab2d.common.util.JobUtil;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.FileEvent;
import gov.cms.ab2d.eventlogger.reports.sql.LoggerEventSummary;
import gov.cms.ab2d.fhir.FhirVersion;
import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import static gov.cms.ab2d.common.util.Constants.ADMIN_ROLE;
import static gov.cms.ab2d.eventlogger.Ab2dEnvironment.PROD_LIST;
import static gov.cms.ab2d.eventlogger.events.SlackEvents.ORG_FIRST;

@Slf4j
@Service
@Transactional
public class JobServiceImpl implements JobService {

    private final PdpClientService pdpClientService;
    private final JobRepository jobRepository;
    private final JobOutputService jobOutputService;
    private final LogManager eventLogger;
    private final LoggerEventSummary loggerEventSummary;
    private final String fileDownloadPath;

    public static final String INITIAL_JOB_STATUS_MESSAGE = "0%";

    public JobServiceImpl(PdpClientService pdpClientService, JobRepository jobRepository, JobOutputService jobOutputService,
                          LogManager eventLogger, LoggerEventSummary loggerEventSummary,
                          @Value("${efs.mount}") String fileDownloadPath) {
        this.pdpClientService = pdpClientService;
        this.jobRepository = jobRepository;
        this.jobOutputService = jobOutputService;
        this.eventLogger = eventLogger;
        this.loggerEventSummary = loggerEventSummary;
        this.fileDownloadPath = fileDownloadPath;
    }

    @Override
    public Job createJob(String resourceTypes, String url, String contractNumber, String outputFormat,
                         OffsetDateTime since, FhirVersion version) {
        Job job = new Job();
        job.setResourceTypes(resourceTypes);
        job.setJobUuid(UUID.randomUUID().toString());
        job.setRequestUrl(url);
        job.setStatusMessage(INITIAL_JOB_STATUS_MESSAGE);
        job.setCreatedAt(OffsetDateTime.now());
        job.setOutputFormat(outputFormat);
        job.setProgress(0);
        job.setSince(since);
        job.setFhirVersion(version);
        job.setPdpClient(pdpClientService.getCurrentClient());

        // Check to see if there is any attestation
        PdpClient pdpClient = pdpClientService.getCurrentClient();
        Contract contract = pdpClient.getContract();
        if (contractNumber != null && !contractNumber.equals(contract.getContractNumber())) {
            String errorMsg = "Specifying contract: " + contractNumber + " not associated with internal id: " + pdpClient.getId();
            log.error(errorMsg);
            throw new InvalidContractException(errorMsg);
        }

        if (!contract.hasAttestation()) {
            String errorMsg = "Contract: " + contractNumber + " is not attested.";
            log.error(errorMsg);
            throw new InvalidContractException(errorMsg);
        }

        eventLogger.log(EventUtils.getJobChangeEvent(job, JobStatus.SUBMITTED, "Job Created"));

        // Report client running first job in prod
        if (clientHasNeverCompletedJob(contract.getContractNumber())) {
            String firstJobMessage = String.format(ORG_FIRST + " Organization %s is running their first job for contract %s",
                    pdpClient.getOrganization(), contract.getContractNumber());
            eventLogger.alert(firstJobMessage, PROD_LIST);
        }
        job.setContractNumber(contract.getContractNumber());
        job.setStatus(JobStatus.SUBMITTED);
        return jobRepository.save(job);
    }

    @Override
    public void cancelJob(String jobUuid) {
        Job job = getAuthorizedJobByJobUuid(jobUuid);

        if (!job.getStatus().isCancellable()) {
            log.error("Job had a status of {} so it was not able to be cancelled", job.getStatus());
            throw new InvalidJobStateTransition("Job has a status of " + job.getStatus() + ", so it cannot be cancelled");
        }
        eventLogger.log(EventUtils.getJobChangeEvent(job, JobStatus.CANCELLED, "Job Cancelled"));
        jobRepository.cancelJobByJobUuid(jobUuid);
    }

    private Job getAuthorizedJobByJobUuid(String jobUuid) {
        Job job = getJobByJobUuid(jobUuid);

        PdpClient pdpClient = pdpClientService.getCurrentClient();
        if (!pdpClient.equals(job.getPdpClient())) {
            log.error("Client attempted to download a file where they had a valid UUID, but was not logged in as the " +
                    "client that created the job");
            throw new InvalidJobAccessException("Unauthorized");
        }

        return job;
    }

    @Override
    public Job getAuthorizedJobByJobUuidAndRole(String jobUuid) {
        PdpClient pdpClient = pdpClientService.getCurrentClient();

        for (Role role : pdpClient.getRoles()) {
            if (role.getName().equals(ADMIN_ROLE)) {
                log.info("Admin accessed job {}", jobUuid);
                return getJobByJobUuid(jobUuid);
            }
        }

        return getAuthorizedJobByJobUuid(jobUuid);
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
        Job job = getAuthorizedJobByJobUuid(jobUuid);

        // Make sure that there is a path that matches a job output for the job they are requesting
        boolean jobOutputMatchesPath = false;
        JobOutput foundJobOutput = null;
        for (JobOutput jobOutput : job.getJobOutputs()) {
            if (jobOutput.getFilePath().equals(fileName)) {
                jobOutputMatchesPath = true;
                foundJobOutput = jobOutput;
                break;
            }
        }

        if (!jobOutputMatchesPath) {
            log.error("No Job Output with the file name {} exists in our records", fileName);
            throw new ResourceNotFoundException("No Job Output with the file name " + fileName + " exists in our records");
        }

        Path file = Paths.get(fileDownloadPath, job.getJobUuid(), fileName);
        Resource resource = new UrlResource(file.toUri());

        if (!resource.exists()) {
            String errorMsg;
            if (foundJobOutput.getDownloaded()) {
                errorMsg = "The file is not present as it has already been downloaded. Please resubmit the job.";
            } else if (job.getExpiresAt().isBefore(OffsetDateTime.now())) {
                errorMsg = "The file is not present as it has expired. Please resubmit the job.";
            } else {
                errorMsg = "The file is not present as there was an error. Please resubmit the job.";
            }
            log.error(errorMsg);
            throw new JobOutputMissingException(errorMsg);
        }

        return resource;
    }

    @Override
    public void deleteFileForJob(File file, String jobUuid) {
        String fileName = file.getName();
        Job job = jobRepository.findByJobUuid(jobUuid);
        JobOutput jobOutput = jobOutputService.findByFilePathAndJob(fileName, job);
        jobOutput.setDownloaded(true);
        jobOutputService.updateJobOutput(jobOutput);
        eventLogger.log(EventUtils.getFileEvent(job, file, FileEvent.FileStatus.DELETE));
        if (JobUtil.isJobDone(job)) {
            eventLogger.log(LogManager.LogType.KINESIS, loggerEventSummary.getSummary(job.getJobUuid()));
        }
        boolean deleted = file.delete();
        if (!deleted) {
            log.error("Was not able to delete the file {}", file.getName());
        }
    }

    @Override
    public boolean checkIfCurrentClientCanAddJob() {
        PdpClient pdpClient = pdpClientService.getCurrentClient();
        List<Job> jobs = jobRepository.findActiveJobsByClient(pdpClient);
        return jobs.size() < pdpClient.getMaxParallelJobs();
    }

    @Override
    public List<String> getActiveJobIds() {
        //Sorting with stream so we don't affect existing code that uses findActiveJobsByClient.
        //Number of jobs returned should be small
        return jobRepository.findActiveJobsByClient(pdpClientService.getCurrentClient()).stream()
                .sorted(Comparator.comparing(Job::getCreatedAt))
                .map(Job::getJobUuid).collect(Collectors.toList());
    }

    private boolean clientHasNeverCompletedJob(String contractNumber) {
        int completedJobs = jobRepository.countJobByContractNumberAndStatus(contractNumber,
                List.of(JobStatus.SUBMITTED, JobStatus.IN_PROGRESS, JobStatus.SUCCESSFUL));
        return completedJobs == 0;
    }
}
