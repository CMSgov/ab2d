package gov.cms.ab2d.filter;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.ab2d.fhir.EobUtils;
import gov.cms.ab2d.fhir.Versions;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import static gov.cms.ab2d.fhir.Versions.FhirVersions.R4;
import static gov.cms.ab2d.fhir.Versions.FhirVersions.STU3;

/**
 * Loads Explanation of Benefits object from a file or reader
 */
public class EOBLoadUtilities {
    public static final String EOB_TYPE_CODE_SYS = "eob-type";
    public static final String EOB_TYPE_PART_D_CODE_VAL = "PDE";

    /**
     * Parse and return the ExplanationOfBenefit file from a JOSN file in the classpath
     * @param fileInClassPath - the file name and path in the classpath containing the
     *             Explanation of Benefit data retrieved from Blue
     * @return the ExplanationOfBenefit object
     */
    public static org.hl7.fhir.dstu3.model.ExplanationOfBenefit getSTU3EOBFromFileInClassPath(String fileInClassPath, FhirContext context) {
        if (StringUtils.isBlank(fileInClassPath)) {
            return null;
        }
        ClassLoader classLoader = EOBLoadUtilities.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileInClassPath);
        return Versions.getJsonParser(STU3).parseResource(org.hl7.fhir.dstu3.model.ExplanationOfBenefit.class, inputStream);
    }

    /**
     * Retrieve the Explanation of Benefit object data from a Reader object
     * @param reader - the reader
     * @return the Explanation of Benefit object
     * @throws IOException if the file is invalid or unreadable
     */
    public static Object getEOBFromReader(Reader reader, FhirContext context) throws IOException {
        if (reader == null) {
            return null;
        }
        String response = IOUtils.toString(reader);
        switch (context.getVersion().getVersion()) {
            case DSTU3:
                return Versions.getJsonParser(STU3).parseResource(org.hl7.fhir.dstu3.model.ExplanationOfBenefit.class, response);
            case R4:
                return Versions.getJsonParser(R4).parseResource(org.hl7.fhir.r4.model.ExplanationOfBenefit.class, response);
            default:
                return null;
        }
    }

    /**
     * Returns if a resource is a part D explanation of benefit
     *
     * @param resource - resource to test
     * @return true if it's a part D explanation of benefit
     */
    public static boolean isPartD(org.hl7.fhir.dstu3.model.Resource resource) {

        if (resource == null || resource.getResourceType() == null ||
                resource.getResourceType() != org.hl7.fhir.dstu3.model.ResourceType.ExplanationOfBenefit) {
            return false;
        }
        org.hl7.fhir.dstu3.model.ExplanationOfBenefit eob = (org.hl7.fhir.dstu3.model.ExplanationOfBenefit) resource;
        org.hl7.fhir.dstu3.model.CodeableConcept c = eob.getType();
        if (c == null || c.getCoding() == null) {
            return false;
        }
        // See if there is a coding value for EOB-TYPE that equals PDE
        return c.getCoding().stream()
                .filter(code -> code.getSystem().endsWith(EOB_TYPE_CODE_SYS))
                .anyMatch(code -> code.getCode().equalsIgnoreCase(EOB_TYPE_PART_D_CODE_VAL));
    }

    public static boolean isPartD(IBaseResource resource) {
        return EobUtils.isPartD(resource);
    }
}
