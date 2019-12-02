package gov.cms.ab2d.filter;

import lombok.Data;
import lombok.experimental.Accessors;
import org.hl7.fhir.dstu3.model.*;

import java.util.List;

/**
 * The object that defines the partial content of the ExplanationOfBenefit.item values that
 * can be returned to Part D providers.
 */
@Data
@Accessors(chain = true)
public class AB2DItemComponent {
    CodeableConcept service;
    SimpleQuantity quantity;
    Period servicedPeriod;
    Type location;
    List<PositiveIntType> careTeamLinkId;
    PositiveIntType sequence;
}
