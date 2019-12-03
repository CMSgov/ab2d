package gov.cms.ab2d.filter;

import lombok.Data;
import lombok.experimental.Accessors;
import org.hl7.fhir.dstu3.model.*;

import java.util.List;

@Data
@Accessors(chain = true)
public class AB2DExplanationOfBenefit {
    private Reference patient;
    private CodeableConcept type;
    private ResourceType resourceType;
    private List<ExplanationOfBenefit.DiagnosisComponent> diagnosis;
    private List<ExplanationOfBenefit.ProcedureComponent> procedure;
    private List<AB2DItemComponent> item;
    private Reference provider;
    private Reference organization;
    private Reference facility;
    private List<ExplanationOfBenefit.CareTeamComponent> careTeam;
    private List<Identifier> identifier;
}
