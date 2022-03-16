package gov.cms.ab2d.audit.remote;

import com.amazonaws.services.s3control.model.transform.JobDescriptorStaxUnmarshaller;
import gov.cms.ab2d.audit.dto.AuditMockJob;
import gov.cms.ab2d.common.dto.StaleJob;
import gov.cms.ab2d.common.model.JobStatus;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static gov.cms.ab2d.common.model.JobStatus.SUCCESSFUL;

@Primary
@Component
public class JobAuditClientMock extends JobAuditClient {

    private final Map<String, AuditMockJob> jobMap = new HashMap<>(89);

    public JobAuditClientMock() {
        super(null);
    }

    @Override
    public List<StaleJob> checkForExpiration(List<String> jobUuids, int ttl) {
        // TODO - copied directly from JobServiceImpl.  It would be better to share the code.
        return jobMap.values().stream()
                .filter(job -> jobUuids.contains(job.getJobUuid()))
                .filter(job -> job.getStatus().isFinished())
                .filter(job -> successFilter(job, ttl))
                .map(AuditMockJob::getStaleJob)
                .toList();
    }

    private boolean successFilter(AuditMockJob job, int ttl) {
        if (SUCCESSFUL != job.getStatus()) {
            // Unsuccessful jobs don't need to filter based on completion timestamps
            return true;
        }

        OffsetDateTime completedTime = job.getCompletedAt();
        // This really should be an assert as if a job is successful, it should have a completion timestamp.
        if (completedTime == null) {
            return false;
        }

        boolean retVal = completedBeforeTTL(completedTime, ttl);
        return retVal;
    }

    private boolean completedBeforeTTL(OffsetDateTime completedAt, int ttl) {
        final Instant deleteBoundary = Instant.now().minus(ttl, ChronoUnit.HOURS);
        return completedAt.toInstant().isBefore(deleteBoundary);
    }

    public void update(AuditMockJob job) {
        jobMap.put(job.getStaleJob().getJobUuid(), job);
    }

    public void cleanup() {
        jobMap.clear();
    }
}
