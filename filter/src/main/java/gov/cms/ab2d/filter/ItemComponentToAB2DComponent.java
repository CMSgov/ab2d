package gov.cms.ab2d.filter;

import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.PositiveIntType;

/**
 * copy a subset of an ExplanationOfBenefit.item component object to an AB2D object
 */
public class ItemComponentToAB2DComponent {
    public static AB2DItemComponent from(ExplanationOfBenefit.ItemComponent c) {
        AB2DItemComponent component = new AB2DItemComponent();
        component.setSequence(new PositiveIntType(c.getSequence()));
        component.setLocation(c.getLocation());
        component.setQuantity(c.getQuantity());
        component.setService(c.getService());
        component.setServicedPeriod(c.getServicedPeriod());
        component.setCareTeamLinkId(c.getCareTeamLinkId());
        return component;
    }
}
