package gov.cms.ab2d.filter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.EncodingEnum;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.ResourceType;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

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
    public static ExplanationOfBenefit getEOBFromFileInClassPath(String fileInClassPath, FhirContext context) {
        if (StringUtils.isBlank(fileInClassPath)) {
            return null;
        }
        ClassLoader classLoader = EOBLoadUtilities.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileInClassPath);
        return getParser(context).parseResource(ExplanationOfBenefit.class, inputStream);
    }

    /**
     * Retrieve the Explanation of Benefit object data from a Reader object
     * @param reader - the reader
     * @return the Explanation of Benefit object
     * @throws IOException if the file is invalid or unreadable
     */
    public static ExplanationOfBenefit getEOBFromReader(Reader reader, FhirContext context) throws IOException {
        if (reader == null) {
            return null;
        }
        String response = IOUtils.toString(reader);
        return getParser(context).parseResource(ExplanationOfBenefit.class, response);
    }

    /**
     * Convenience method to create the parser for any method that needs it.
     *
     * @return the parser
     */
    private static IParser getParser(FhirContext context) {
        EncodingEnum respType = EncodingEnum.forContentType(EncodingEnum.JSON_PLAIN_STRING);
        return respType.newParser(context);
    }

    /**
     * Returns if a resource is a part D explanation of benefit
     *
     * @param resource - resource to test
     * @return true if it's a part D explanation of benefit
     */
    public static boolean isPartD(Resource resource) {
        if (resource == null || resource.getResourceType() == null || resource.getResourceType() != ResourceType.ExplanationOfBenefit) {
            return false;
        }
        ExplanationOfBenefit eob = (ExplanationOfBenefit) resource;
        CodeableConcept c = eob.getType();
        if (c == null || c.getCoding() == null) {
            return false;
        }
        // See if there is a coding value for EOB-TYPE that equals PDE
        return c.getCoding().stream()
                .filter(code -> code.getSystem().endsWith(EOB_TYPE_CODE_SYS))
                .anyMatch(code -> code.getCode().equalsIgnoreCase(EOB_TYPE_PART_D_CODE_VAL));
    }
}
