package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.CoverageSummary;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.Map;

@Getter
@AllArgsConstructor
public class JobData {
    private String jobUuid;
    private final OffsetDateTime sinceTime;
    private final String organization;
    private final Map<String, CoverageSummary> patients;
}
