package gov.cms.ab2d.job.dto;

import gov.cms.ab2d.job.model.JobOutput;
import gov.cms.ab2d.job.model.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class JobPollResult {
    private String requestUrl;
    private JobStatus status;
    private int progress;
    private String transactionTime;
    private OffsetDateTime expiresAt;
    private List<JobOutput> jobOutputs;
}
