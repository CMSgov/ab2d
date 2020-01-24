package gov.cms.ab2d.worker.adapter.bluebutton;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;

public interface PatientClaimsProcessor {

    Future<Integer> process(GetPatientsByContractResponse.PatientDTO patientId, Lock lock, Path outputFile,
                            Path errorFile, OffsetDateTime attTime);

}
