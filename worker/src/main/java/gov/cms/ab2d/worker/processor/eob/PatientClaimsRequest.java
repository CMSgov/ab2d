package gov.cms.ab2d.worker.processor.eob;

import com.newrelic.api.agent.Token;
import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneficiaries.PatientDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@AllArgsConstructor
public class PatientClaimsRequest {
    private final PatientDTO patientDTO;
    private final OffsetDateTime attTime;
    private final OffsetDateTime sinceTime;
    private final String user;
    private final String job;
    private final String contractNum;
    private final Token token;
}
