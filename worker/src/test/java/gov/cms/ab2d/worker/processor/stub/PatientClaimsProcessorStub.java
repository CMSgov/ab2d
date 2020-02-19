package gov.cms.ab2d.worker.processor.stub;

import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse;
import gov.cms.ab2d.worker.processor.PatientClaimsProcessor;
import gov.cms.ab2d.worker.processor.StreamHelper;
import org.springframework.scheduling.annotation.AsyncResult;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.concurrent.Future;

public class PatientClaimsProcessorStub implements PatientClaimsProcessor {

    @Override
    public Future<Void> process(GetPatientsByContractResponse.PatientDTO patientDTO, StreamHelper writer, OffsetDateTime attTime) {

        writer.getDataFiles().add(Path.of("TEST_DATA_FILE"));
        writer.getErrorFiles().add(Path.of("TEST_ERROR_FILE"));
        return new AsyncResult<>(null);
    }
}