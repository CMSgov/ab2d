package gov.cms.ab2d.api.remote;

import gov.cms.ab2d.common.dto.JobPollResult;
import gov.cms.ab2d.common.dto.StartJobDTO;
import gov.cms.ab2d.common.model.JobOutput;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.model.TooFrequentInvocations;
import gov.cms.ab2d.common.service.InvalidJobStateTransition;
import gov.cms.ab2d.common.service.JobOutputMissingException;
import gov.cms.ab2d.common.service.ResourceNotFoundException;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.JobStatusChangeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static gov.cms.ab2d.common.model.JobStatus.CANCELLED;
import static gov.cms.ab2d.common.model.JobStatus.SUBMITTED;
import static gov.cms.ab2d.common.model.JobStatus.SUCCESSFUL;

@Primary
@Component
public class JobClientMock extends JobClient {

    public static final int EXPIRES_IN_DAYS = 100;

    private final Map<String, StartJobDTO> createdJobs = new HashMap<>(89);
    private final Map<String, OffsetDateTime> pollTimes = new HashMap<>(89);

    private final List<JobOutput> jobOutputList = new ArrayList<>();
    private JobStatus expectedStatus = JobStatus.SUCCESSFUL;
    private int progress = 100;
    private OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(EXPIRES_IN_DAYS);
    private boolean resultsCreated;

    @Value("classpath:test.ndjson")
    private Resource jobOutputResults;

    private final LogManager eventLogger;

    @Autowired
    public JobClientMock(LogManager eventLogger) {
        super(null);
        this.eventLogger = eventLogger;
    }

    // Returns job_guid
    public String createJob(StartJobDTO startJobDTO) {
        String jobId = UUID.randomUUID().toString();
        createdJobs.put(jobId, startJobDTO);
        JobStatusChangeEvent jobStatusChangeEvent = new JobStatusChangeEvent(startJobDTO.getOrganization(),
                jobId, null, SUBMITTED.name(), "Job Created");
        eventLogger.log(jobStatusChangeEvent);
        return jobId;
    }

    public StartJobDTO lookupJob(String jobId) {
        return createdJobs.get(jobId);
    }

    public String pickAJob() {
        if (createdJobs.isEmpty()) {
            return "";
        }

        return createdJobs.keySet().iterator().next();
    }

    public int activeJobs(String organization) {
        return (int) createdJobs.values().stream()
                .filter(startJobDTO -> startJobDTO.getOrganization().equals(organization)).count();
    }

    public void switchAllJobsToNewOrganization(String organization) {
        createdJobs.forEach((key, value) -> createdJobs.put(key, convert(value, organization)));
    }

    private StartJobDTO convert(StartJobDTO orig, String organization) {
        return new StartJobDTO(orig.getContractNumber(), organization, orig.getResourceTypes(),
                orig.getUrl(), orig.getOutputFormat(), orig.getSince(), orig.getVersion());
    }

    public List<String> getActiveJobIds(String organization) {
        return createdJobs.entrySet().stream()
                .filter(mapEntry -> mapEntry.getValue().getOrganization().equals(organization))
                .map(Map.Entry::getKey).toList();
    }

    public Resource getResourceForJob(String jobGuid, String fileName, String organization) {
        if (expiresAt.isBefore(OffsetDateTime.now())) {
            throw new JobOutputMissingException("The file is not present as it has expired. Please resubmit the job.");
        }

        File resourceFile;
        try {
            resourceFile = jobOutputResults.getFile();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        if (resourceFile.getName().equals(fileName)) {
            if (jobOutputList.isEmpty()) {
                throw new ResourceNotFoundException("No Job Output with the file name " + fileName + " exists in our records");
            }
            if (jobOutputList.get(0).getDownloaded()) {
                String errorMsg = "The file is not present as it has already been downloaded. Please resubmit the job.";
                throw new JobOutputMissingException(errorMsg);
            }

            if (!resultsCreated) {
                throw new JobOutputMissingException("The file is not present as there was an error. Please resubmit the job.");
            }

            return jobOutputResults;
        }

        // Not Found
        throw new ResourceNotFoundException("No Job Output with the file name " + fileName + " exists in our records");
    }

    public void deleteFileForJob(File file, String jobGuid) {
    }

    public JobPollResult poll(boolean admin, String jobUuid, String organization, int delaySeconds) {
        if (jobUuid.isBlank()) {
            throw new ResourceNotFoundException("No job with jobUuid " + jobUuid + " was found");
        }

        pollAndUpdateTime(jobUuid, delaySeconds);

        StartJobDTO startJobDTO = createdJobs.get(jobUuid);
        if (startJobDTO == null) {
            throw new ResourceNotFoundException("No job with jobUuid " + jobUuid + " was found");
        }

        String transactionTime =
                new org.hl7.fhir.dstu3.model.DateTimeType(OffsetDateTime.now().toString()).toHumanDisplay();
        return new JobPollResult(startJobDTO.getUrl(), expectedStatus, progress,
                transactionTime, expiresAt, jobOutputList);
    }

    public void cancelJob(String jobUuid, String organization) {
        if (!createdJobs.containsKey(jobUuid)) {
            throw new ResourceNotFoundException("No job with jobUuid " + jobUuid + " was found");
        }

        if (!expectedStatus.isCancellable()) {
            throw new InvalidJobStateTransition("Job has a status of " + expectedStatus + ", so it cannot be cancelled");
        }
        JobStatusChangeEvent jobStatusChangeEvent = new JobStatusChangeEvent(createdJobs.get(jobUuid).getOrganization(),
                jobUuid, expectedStatus.name(), CANCELLED.name(), "Job Cancelled");
        eventLogger.log(jobStatusChangeEvent);

    }

    public void addJobOutputForDownload(JobOutput jobOutput) {
        jobOutputList.add(jobOutput);
    }

    public void setExpectedStatus(JobStatus expectedStatus) {
        this.expectedStatus = expectedStatus;
        progress = (expectedStatus == SUCCESSFUL) ? 100 : 0;
    }

    public void setExpectedStatusAndProgress(JobStatus expectedStatus, int progress) {
        this.expectedStatus = expectedStatus;
        this.progress = progress;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public void setResultsCreated(boolean resultsCreated) {
        this.resultsCreated = resultsCreated;
    }

    public void cleanup(String jobID) {
        createdJobs.remove(jobID);
    }

    public void cleanupAll() {
        createdJobs.clear();
        jobOutputList.clear();
        expectedStatus = JobStatus.SUCCESSFUL;
        progress = 100;
        pollTimes.clear();
        expiresAt = OffsetDateTime.now().plusDays(EXPIRES_IN_DAYS);
        resultsCreated = false;
    }

    public void pollAndUpdateTime(String jobUuid, int delaySeconds) {
        if (pollingTooMuch(jobUuid, delaySeconds)) {
            throw new TooFrequentInvocations("polling too frequently");
        }
        pollTimes.put(jobUuid, OffsetDateTime.now());
    }

    private boolean pollingTooMuch(String jobUuid, int delaySeconds) {
        OffsetDateTime lastPollTime = pollTimes.get(jobUuid);
        return lastPollTime != null && lastPollTime.plusSeconds(delaySeconds).isAfter(OffsetDateTime.now());
    }
}
