package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse.PatientDTO;

import java.time.OffsetDateTime;
import java.util.concurrent.Future;

public interface PatientClaimsProcessor {

    Future<Void> process(PatientDTO patientDTO, JobDataWriter writer, OffsetDateTime attTime);

}
