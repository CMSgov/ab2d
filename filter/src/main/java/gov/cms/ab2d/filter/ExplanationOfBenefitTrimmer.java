package gov.cms.ab2d.filter;

import org.hl7.fhir.instance.model.api.IBaseResource;

import static gov.cms.ab2d.common.util.Constants.EOB;

public class ExplanationOfBenefitTrimmer {
    public static IBaseResource getBenefit(IBaseResource resource) {
        if (resource == null || !resource.fhirType().equalsIgnoreCase(EOB)) {
            return null;
        }

        if (resource.getClass() == org.hl7.fhir.r4.model.ExplanationOfBenefit.class) {
            return ExplanationOfBenefitTrimmerR4.getBenefit(
                    (org.hl7.fhir.r4.model.ExplanationOfBenefit) resource);
        }
        if (resource.getClass() == org.hl7.fhir.dstu3.model.ExplanationOfBenefit.class) {
            return ExplanationOfBenefitTrimmerR3.getBenefit(
                    (org.hl7.fhir.dstu3.model.ExplanationOfBenefit) resource);
        }
        return null;
    }

    public static boolean isPartD(IBaseResource resource) {
        if (resource.getClass() == org.hl7.fhir.r4.model.ExplanationOfBenefit.class) {
            return EOBLoadUtilities.isPartD((org.hl7.fhir.r4.model.ExplanationOfBenefit) resource);
        }
        if (resource.getClass() == org.hl7.fhir.dstu3.model.ExplanationOfBenefit.class) {
            return EOBLoadUtilities.isPartD((org.hl7.fhir.dstu3.model.ExplanationOfBenefit) resource);
        }
        return false;
    }
}
