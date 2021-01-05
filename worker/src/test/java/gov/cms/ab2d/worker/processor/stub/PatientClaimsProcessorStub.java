package gov.cms.ab2d.worker.processor.stub;

import gov.cms.ab2d.worker.processor.PatientClaimsProcessor;
import gov.cms.ab2d.worker.processor.EobSearchResult;
import gov.cms.ab2d.worker.processor.PatientClaimsRequest;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Reference;
import org.springframework.scheduling.annotation.AsyncResult;

import java.util.Collections;
import java.util.Date;
import java.util.concurrent.Future;

public class PatientClaimsProcessorStub implements PatientClaimsProcessor {

    @Override
    public Future<EobSearchResult> process(PatientClaimsRequest request) {
        EobSearchResult result = new EobSearchResult();
        ExplanationOfBenefit eob = new ExplanationOfBenefit();
        Reference ref = new Reference("Patient/" + request.getCoverageSummary().getIdentifiers().getBeneficiaryId());
        eob.setPatient(ref);
        Period period = new Period();
        period.setStart(new Date(0));
        period.setEnd(new Date());
        eob.setBillablePeriod(period);
        result.setEobs(Collections.singletonList(eob));
        result.setJobId(request.getJob());
        result.setContractNum(request.getContractNum());
        return new AsyncResult<>(result);
    }
}