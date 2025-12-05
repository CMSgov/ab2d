package gov.cms.ab2d.filter;

import gov.cms.ab2d.fhir.FhirVersion;
import lombok.experimental.UtilityClass;
import org.hl7.fhir.instance.model.api.IBaseResource;

@UtilityClass
public class ExplanationOfBenefitTrimmer {
    /**
     * Return the trimmed version of the EOB. Currently, only R4 & DSTU3 are supported
     *
     * @param resource - the resource
     * @return - the trimmed resource
     */
    public static IBaseResource getBenefit(IBaseResource resource, FhirVersion fhirVersion) {
        if (resource == null) {
            return null;
        }
        if (fhirVersion == FhirVersion.R4v3) {
            return ExplanationOfBenefitTrimmerR4V3.getBenefit(resource);
        }
        return switch (resource.getStructureFhirVersionEnum()) {
            case R4 -> ExplanationOfBenefitTrimmerR4.getBenefit(resource);
            case DSTU2 -> null;
            case DSTU2_HL7ORG -> null;
            case DSTU2_1 -> null;
            case DSTU3 -> ExplanationOfBenefitTrimmerSTU3.getBenefit(resource);
            case R5 -> null;
            default -> null;
        };
    }
}
