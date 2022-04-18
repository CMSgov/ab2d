package gov.cms.ab2d.audit.remote;

import gov.cms.ab2d.audit.dto.AuditMockJob;
import gov.cms.ab2d.common.dto.StaleJob;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

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
    public List<StaleJob> checkForExpiration(List<String> jobUuids, int ttlHours) {
        return jobMap.values().stream()
                .filter(job -> jobUuids.contains(job.getJobUuid()))
                .filter(job -> job.isExpired(ttlHours))
                .map(AuditMockJob::getStaleJob)
                .toList();
    }

    public void update(AuditMockJob job) {
        jobMap.put(job.getStaleJob().getJobUuid(), job);
    }

    public void cleanup() {
        jobMap.clear();
    }
}
