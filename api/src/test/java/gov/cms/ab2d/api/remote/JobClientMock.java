package gov.cms.ab2d.api.remote;

import gov.cms.ab2d.common.dto.JobPollResult;
import gov.cms.ab2d.common.dto.StartJobDTO;
import gov.cms.ab2d.common.model.JobStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyList;

@Primary
@Component
public class JobClientMock extends JobClient {

    private final Map<String, StartJobDTO> createdJobs = new HashMap<>(89);

    @Autowired
    public JobClientMock() {
        super(null);
    }

    // Returns job_guid
    public String createJob(StartJobDTO startJobDTO) {
        String jobId = UUID.randomUUID().toString();
        createdJobs.put(jobId, startJobDTO);
        return jobId;
    }

    public StartJobDTO lookupJob(String jobId) {
        return createdJobs.get(jobId);
    }

    public int activeJobs(String organization) {
        return 0;
    }

    public List<String> getActiveJobIds(String organization) {
        throw new UnsupportedOperationException();
    }

    public Resource getResourceForJob(String jobGuid, String fileName, String organization) {
        throw new UnsupportedOperationException();
    }

    public void deleteFileForJob(File file, String jobGuid) {
        throw new UnsupportedOperationException();
    }

    public JobPollResult poll(boolean admin, String jobUuid, String organization, int delaySeconds) {
        StartJobDTO startJobDTO = createdJobs.get(jobUuid);
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(5);
        return new JobPollResult(startJobDTO.getUrl(), JobStatus.SUCCESSFUL, 100, "", expiresAt, emptyList());
    }

    public void cancelJob(String jobUuid, String organization) {
        throw new UnsupportedOperationException();
    }

    public void cleanup(String jobID) {
        createdJobs.remove(jobID);
    }

    public void cleanupAll() {
        createdJobs.clear();
    }
}
