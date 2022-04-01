package gov.cms.ab2d.audit.remote;

import gov.cms.ab2d.common.dto.StaleJob;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Primary
@Component
public class JobOutputAuditClientMock extends JobOutputAuditClient {

    private final Map<StaleJob, List<String>> jobMap = new HashMap<>(89);

    public JobOutputAuditClientMock() {
        super(null);
    }

    @Override
    public Map<StaleJob, List<String>> checkForDownloadedExpiredFiles(int interval) {
        return jobMap;
    }

    public void update(Map<StaleJob, List<String>> jobsFiles) {
        jobMap.putAll(jobsFiles);
    }

    public void cleanup() {
        jobMap.clear();
    }
}
