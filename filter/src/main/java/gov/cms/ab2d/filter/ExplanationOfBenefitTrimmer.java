package gov.cms.ab2d.filter;

import org.hl7.fhir.instance.model.api.IBaseResource;

public class ExplanationOfBenefitTrimmer {
    /**
     * Return the trimmed version of the EOB. Currently, only R4 & DSTU3 are supported
     *
     * @param resource - the resource
     * @return - the trimmed resource
     */
    public static IBaseResource getBenefit(IBaseResource resource) {
        if (resource == null) {
            return null;
        }
        switch (resource.getStructureFhirVersionEnum()) {
            case R4:
                return ExplanationOfBenefitTrimmerR4.getBenefit(resource);
            case DSTU2:
                return null;
            case DSTU2_HL7ORG:
                return null;
            case DSTU2_1:
                return null;
            case DSTU3:
                return ExplanationOfBenefitTrimmerSTU3.getBenefit(resource);
            case R5:
                return null;
            default:
                return null;
        }
    }
}
