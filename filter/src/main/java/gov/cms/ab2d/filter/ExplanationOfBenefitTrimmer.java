package gov.cms.ab2d.filter;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBaseResource;

public class ExplanationOfBenefitTrimmer {
    public static IBaseResource getBenefit(IBaseResource resource) {
        if (resource.getStructureFhirVersionEnum() == FhirVersionEnum.R4) {
            return ExplanationOfBenefitTrimmerR4.getBenefit(resource);
        }
        if (resource.getStructureFhirVersionEnum() == FhirVersionEnum.DSTU3) {
            return ExplanationOfBenefitTrimmerSTU3.getBenefit(resource);
        }
        return null;
    }
}
