package gov.cms.ab2d.worker.processor.stub;

import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneficiaries;
import gov.cms.ab2d.worker.processor.PatientClaimsProcessor;
import gov.cms.ab2d.worker.processor.domainmodel.PatientClaimsRequest;
import org.springframework.scheduling.annotation.AsyncResult;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Future;

public class PatientClaimsProcessorStub implements PatientClaimsProcessor {

    @Override
    public Future<Void> process(PatientClaimsRequest request, Map<String, ContractBeneficiaries.PatientDTO> map) {

        request.getHelper().getDataFiles().add(Path.of("TEST_DATA_FILE"));
        request.getHelper().getErrorFiles().add(Path.of("TEST_ERROR_FILE"));
        return new AsyncResult<>(null);
    }
}