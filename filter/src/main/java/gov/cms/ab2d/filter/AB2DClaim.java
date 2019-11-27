package gov.cms.ab2d.filter;

import lombok.Data;
import org.hl7.fhir.dstu3.model.*;

import java.util.List;

@Data
public class AB2DClaim {
    AB2DPatient patientTarget;
    CodeableConcept type;
    ResourceType resourceType;
    List<Claim.DiagnosisComponent> diagnosis;
    List<Claim.ProcedureComponent> procedure;
    List<AB2DItemComponent> item;
    Reference provider;
    Reference organization;
    Reference facility;
    List<Claim.CareTeamComponent> careTeam;
    List<Identifier> identifier;
}
