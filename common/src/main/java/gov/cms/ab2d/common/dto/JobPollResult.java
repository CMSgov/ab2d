package gov.cms.ab2d.common.dto;

import gov.cms.ab2d.common.model.JobOutput;
import gov.cms.ab2d.common.model.JobStatus;
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
