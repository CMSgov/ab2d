package gov.cms.ab2d.worker.processor;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.List;

@RequiredArgsConstructor
@Getter
public class EobSearchResult {
    private final String jobId;
    private final String contractNum;
    private final List<IBaseResource> eobs;
}
