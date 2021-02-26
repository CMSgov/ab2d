package gov.cms.ab2d.worker.processor;

import com.newrelic.api.agent.Token;
import gov.cms.ab2d.common.model.CoverageSummary;
import gov.cms.ab2d.fhir.FhirVersion;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@AllArgsConstructor
public class PatientClaimsRequest {
    private final CoverageSummary coverageSummary;
    private final OffsetDateTime attTime;
    private final OffsetDateTime sinceTime;
    private final String clientId;
    private final String job;
    private final String contractNum;
    private final Token token;
    private final FhirVersion version;
}
