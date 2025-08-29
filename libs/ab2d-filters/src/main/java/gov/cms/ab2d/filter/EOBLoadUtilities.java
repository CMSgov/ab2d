package gov.cms.ab2d.filter;

import ca.uhn.fhir.context.FhirContext;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * Loads Explanation of Benefits object from a file or reader
 */
@Slf4j
@UtilityClass
public class EOBLoadUtilities {
    public static final String EOB_TYPE_CODE_SYS = "eob-type";
    public static final String EOB_TYPE_PART_D_CODE_VAL = "PDE";

    /**
     * Parse and return the ExplanationOfBenefit file from a JOSN file in the classpath
     * @param fileInClassPath - the file name and path in the classpath containing the
     *             Explanation of Benefit data retrieved from Blue
     * @return the ExplanationOfBenefit object
     */
    public static org.hl7.fhir.dstu3.model.ExplanationOfBenefit getSTU3EOBFromFileInClassPath(String fileInClassPath) {
        if (StringUtils.isBlank(fileInClassPath)) {
            return null;
        }
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileInClassPath)) {
            return FhirContext.forDstu3().newJsonParser().parseResource(org.hl7.fhir.dstu3.model.ExplanationOfBenefit.class, inputStream);
        } catch (Exception ex) {
            log.error("Unable to open the file", ex);
            return null;
        }
    }

    /**
     * Parse and return the ExplanationOfBenefit file from a JOSN file in the classpath
     * @param fileInClassPath - the file name and path in the classpath containing the
     *             Explanation of Benefit data retrieved from Blue
     * @return the ExplanationOfBenefit object
     */
    public static org.hl7.fhir.r4.model.ExplanationOfBenefit getR4EOBFromFileInClassPath(String fileInClassPath) {
        if (StringUtils.isBlank(fileInClassPath)) {
            return null;
        }
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileInClassPath)) {
            return FhirContext.forR4().newJsonParser().parseResource(org.hl7.fhir.r4.model.ExplanationOfBenefit.class, inputStream);
        } catch (Exception ex) {
            log.error("Unable to open the file", ex);
            return null;
        }
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
        switch (context.getVersion().getVersion()) {
            case DSTU3:
                return FhirContext.forDstu3().newJsonParser().parseResource(org.hl7.fhir.dstu3.model.ExplanationOfBenefit.class, IOUtils.toString(reader));
            case R4:
                return FhirContext.forR4().newJsonParser().parseResource(org.hl7.fhir.r4.model.ExplanationOfBenefit.class, IOUtils.toString(reader));
            default:
                return null;
        }
    }
}
