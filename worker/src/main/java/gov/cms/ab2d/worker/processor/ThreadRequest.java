package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
public class ThreadRequest {
    private GetPatientsByContractResponse.PatientDTO patientDTO;
    private StreamHelper helper;
    private OffsetDateTime attTime;
}
