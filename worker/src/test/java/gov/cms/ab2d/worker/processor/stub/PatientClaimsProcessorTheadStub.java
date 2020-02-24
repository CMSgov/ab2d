package gov.cms.ab2d.worker.processor.stub;

import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse;
import gov.cms.ab2d.worker.processor.PatientClaimsProcessor;
import gov.cms.ab2d.worker.processor.StreamHelper;
import org.springframework.scheduling.annotation.AsyncResult;

import java.time.OffsetDateTime;
import java.util.concurrent.Future;

public class PatientClaimsProcessorTheadStub implements PatientClaimsProcessor {

    @Override
    public Future<Void> process(String contractId, GetPatientsByContractResponse.PatientDTO patientDTO, StreamHelper writer, OffsetDateTime attTime) {
        try {
            Thread.sleep((long) Math.random() * 5000);
        } catch (Exception ex) {
            return AsyncResult.forExecutionException(ex);
        }
        return new AsyncResult<>(null);
    }
}