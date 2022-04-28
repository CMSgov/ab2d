package gov.cms.ab2d.audit.dto;

import gov.cms.ab2d.job.dto.StaleJob;
import gov.cms.ab2d.job.model.JobStatus;
import lombok.Value;

import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;

@Value
public class AuditMockJob {
    @NotNull
    StaleJob staleJob;
    @NotNull
    JobStatus status;
    OffsetDateTime completedAt;

    public String getJobUuid() {
        return staleJob.getJobUuid();
    }

    public String getOrganization() {
        return staleJob.getOrganization();
    }

    public boolean isExpired(int ttlHours) {
        return status.isExpired(completedAt, ttlHours);
    }
}
