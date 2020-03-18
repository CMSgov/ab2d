package gov.cms.ab2d.worker.processor.domainmodel;

import com.newrelic.api.agent.Token;
import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse;
import gov.cms.ab2d.worker.processor.StreamHelper;

import java.time.OffsetDateTime;

public class PatientClaimsRequest {
    private final GetPatientsByContractResponse.PatientDTO patientDTO;
    private final StreamHelper helper;
    private final OffsetDateTime attTime;
    private final OffsetDateTime sinceTime;
    private final Token token;

    public PatientClaimsRequest(GetPatientsByContractResponse.PatientDTO patientDTO, StreamHelper helper, OffsetDateTime attTime, OffsetDateTime sinceTime, Token token) {
        this.patientDTO = patientDTO;
        this.helper = helper;
        this.attTime = attTime;
        this.sinceTime = sinceTime;
        this.token = token;
    }

    public GetPatientsByContractResponse.PatientDTO getPatientDTO() {
        return patientDTO;
    }

    public StreamHelper getHelper() {
        return helper;
    }

    public OffsetDateTime getAttTime() {
        return attTime;
    }

    public OffsetDateTime getSinceTime() {
        return sinceTime;
    }

    public Token getToken() {
        return token;
    }
}
