package gov.cms.ab2d.worker.processor.domainmodel;

import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneficiaries;
import lombok.Data;
import lombok.AllArgsConstructor;
import org.hl7.fhir.dstu3.model.Resource;

import java.util.List;

@Data
@AllArgsConstructor
public class EobSearchResponse {
    private ContractBeneficiaries.PatientDTO patient;
    private List<Resource> resources;
}
