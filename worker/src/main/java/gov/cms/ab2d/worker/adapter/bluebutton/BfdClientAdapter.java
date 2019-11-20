package gov.cms.ab2d.worker.adapter.bluebutton;

import java.nio.file.Path;
import java.util.concurrent.Future;

public interface BfdClientAdapter {

    Future<String> processPatient(String patientId, Path outputFile);

}
