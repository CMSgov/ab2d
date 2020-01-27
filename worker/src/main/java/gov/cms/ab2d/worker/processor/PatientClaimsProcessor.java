package gov.cms.ab2d.worker.processor;

import java.nio.file.Path;
import java.util.concurrent.locks.Lock;

public interface PatientClaimsProcessor {

    int process(String patientId, Lock lock, Path outputFile, Path errorFile);

}
