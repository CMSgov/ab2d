package gov.cms.ab2d.filter;

import lombok.Data;
import lombok.experimental.Accessors;
import org.hl7.fhir.dstu3.model.*;

import java.util.List;

@Data
@Accessors(chain = true)
public class AB2DExplanationOfBenefit {
    Reference patient;
    CodeableConcept type;
    ResourceType resourceType;
    List<ExplanationOfBenefit.DiagnosisComponent> diagnosis;
    List<ExplanationOfBenefit.ProcedureComponent> procedure;
    List<AB2DItemComponent> item;
    Reference provider;
    Reference organization;
    Reference facility;
    List<ExplanationOfBenefit.CareTeamComponent> careTeam;
    List<Identifier> identifier;
}
