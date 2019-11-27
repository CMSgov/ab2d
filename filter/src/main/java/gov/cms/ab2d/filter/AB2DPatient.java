package gov.cms.ab2d.filter;

import lombok.Data;
import org.hl7.fhir.dstu3.model.Identifier;

@Data
public class AB2DPatient {
    Identifier identifier;
}
