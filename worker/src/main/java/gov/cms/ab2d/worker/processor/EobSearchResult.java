package gov.cms.ab2d.worker.processor;

import lombok.Data;
import java.util.List;

@Data
public class EobSearchResult {
    private String jobId;
    private String contractNum;
    private List<org.hl7.fhir.dstu3.model.ExplanationOfBenefit> eobs;
}
