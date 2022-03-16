package gov.cms.ab2d.audit.remote;

import gov.cms.ab2d.audit.dto.AuditMockJob;
import gov.cms.ab2d.common.dto.StaleJob;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        //noinspection ConstantConditions
        return jobMap.values().stream()
                .filter(job -> job.getStatus().isFinished())
                .filter(job -> job.getCompletedAt() != null)
                .filter(job -> completedBeforeTTL(job.getCompletedAt(), ttl))
                .map(AuditMockJob::getStaleJob)
                .toList();
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
