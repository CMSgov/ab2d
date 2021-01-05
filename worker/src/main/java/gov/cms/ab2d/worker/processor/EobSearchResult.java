package gov.cms.ab2d.worker.processor;

import lombok.Data;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.List;

@Data
public class EobSearchResult {
    private String jobId;
    private String contractNum;
    private List<IBaseResource> eobs;
}
