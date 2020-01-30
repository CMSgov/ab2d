package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse;
import java.time.OffsetDateTime;
import java.util.concurrent.Future;

public interface PatientClaimsProcessor {

    Future<Integer> process(GetPatientsByContractResponse.PatientDTO patientId, JobDataWriter writer, OffsetDateTime attTime);

}
