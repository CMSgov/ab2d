package gov.cms.ab2d.worker.processor;

import java.nio.file.Path;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;

public interface PatientClaimsProcessor {

    Future<Integer> process(String patientId, Lock lock, Path outputFile, Path errorFile);

}
