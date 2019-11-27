package gov.cms.ab2d.filter;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class AB2DItemComponent {
    CodeableConcept service;
    SimpleQuantity quantity;
    DateType serviced;
    Period servicedPeriod;
    Type location;
    List<PositiveIntType> careTeamLinkId;
    PositiveIntType sequence;
}
