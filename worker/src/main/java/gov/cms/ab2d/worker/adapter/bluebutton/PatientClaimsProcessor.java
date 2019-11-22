package gov.cms.ab2d.worker.adapter.bluebutton;

import java.nio.file.Path;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

public interface PatientClaimsProcessor {

    Future<Integer> process(String patientId, ReentrantLock lock, Path outputFile, Path errorFile);

}
