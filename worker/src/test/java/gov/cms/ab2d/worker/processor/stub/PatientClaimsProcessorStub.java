package gov.cms.ab2d.worker.processor.stub;

import gov.cms.ab2d.worker.processor.PatientClaimsProcessor;
import gov.cms.ab2d.worker.processor.EobSearchResult;
import gov.cms.ab2d.worker.processor.PatientClaimsRequest;
import org.springframework.scheduling.annotation.AsyncResult;

import java.util.Collections;
import java.util.Date;
import java.util.concurrent.Future;

public class PatientClaimsProcessorStub implements PatientClaimsProcessor {

    @Override
    public Future<EobSearchResult> process(PatientClaimsRequest request) {
        org.hl7.fhir.dstu3.model.ExplanationOfBenefit eob = new org.hl7.fhir.dstu3.model.ExplanationOfBenefit();
        org.hl7.fhir.dstu3.model.Reference ref = new org.hl7.fhir.dstu3.model.Reference("Patient/" + request.getCoverageSummary().getIdentifiers().getBeneficiaryId());
        eob.setPatient(ref);
        org.hl7.fhir.dstu3.model.Period period = new org.hl7.fhir.dstu3.model.Period();
        period.setStart(new Date(0));
        period.setEnd(new Date());
        eob.setBillablePeriod(period);

        EobSearchResult result = new EobSearchResult(request.getJob(), request.getContractNum(), Collections.singletonList(eob));
        return new AsyncResult<>(result);
    }
}