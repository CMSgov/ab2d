package gov.cms.ab2d.worker.processor.domainmodel;

import lombok.Data;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;

import java.util.List;

@Data
public class EobSearchResult {
    String jobId;
    String contractNum;
    List<ExplanationOfBenefit> eobs;
}
