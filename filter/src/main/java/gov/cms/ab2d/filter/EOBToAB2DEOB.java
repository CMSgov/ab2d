package gov.cms.ab2d.filter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Does most of the work of converting an Explanation of Benefits object to the object that can
 * be returned to a part D group
 */
public class EOBToAB2DEOB {
    /**
     * Map the data between the two objects
     * @param eob - the Explanation of Benefits object
     * @return - the AB2D object
     */
    public static AB2DExplanationOfBenefit from(ExplanationOfBenefit eob) {
        if (eob == null) {
            throw new ResourceNotFoundException("No explanation of benefit information provided");
        }

        AB2DExplanationOfBenefit ab2DExplanationOfBenefit = new AB2DExplanationOfBenefit();
        ab2DExplanationOfBenefit.setPatient(eob.getPatient());
        ab2DExplanationOfBenefit.setType(eob.getType());
        ab2DExplanationOfBenefit.setResourceType(eob.getResourceType());
        ab2DExplanationOfBenefit.setDiagnosis(eob.getDiagnosis());
        ab2DExplanationOfBenefit.setProcedure(eob.getProcedure());
        ab2DExplanationOfBenefit.setProvider(eob.getProvider());
        ab2DExplanationOfBenefit.setOrganization(eob.getOrganization());
        ab2DExplanationOfBenefit.setFacility(eob.getFacility());
        ab2DExplanationOfBenefit.setCareTeam(eob.getCareTeam());
        ab2DExplanationOfBenefit.setIdentifier(eob.getIdentifier());

        List<ExplanationOfBenefit.ItemComponent> items = eob.getItem();
        if (items != null) {
            List<AB2DItemComponent> components = new ArrayList<>();
            items.forEach(c -> components.add(ItemComponentToAB2DComponent.from(c)));
            ab2DExplanationOfBenefit.setItem(components);
        }

        return ab2DExplanationOfBenefit;
    }

    /**
     * Load the AB2dExplanationOfBenefit object from a JSON file
     *
     * @param path - the file name and path in the classpath containing the
     *             Explanation of Benefit data retrieved from Blue
     * @return the AB2D object
     */
    public static AB2DExplanationOfBenefit fromFileInClasspath(String path) {
        ExplanationOfBenefit retVal = getEOBFromFileInClassPath(path);
        return EOBToAB2DEOB.from(retVal);
    }

    /**
     * Load the AB2dExplanationOfBenefit object from a reader
     *
     * @param reader - the reader with the Explanation of Benefit data retrieved from Blue
     * @return the AB2D object
     * @throws IOException if there is issue reading the file
     */
    public static AB2DExplanationOfBenefit fromReader(Reader reader) throws IOException {
        if (reader == null) {
            return null;
        }
        ExplanationOfBenefit retVal = getEOBFromReader(reader);
        return EOBToAB2DEOB.from(retVal);
    }

    /**
     * Parse and return the ExplanationOfBenefit file from a JOSN file in the classpath
     * @param fileInClassPath - the file name and path in the classpath containing the
     *             Explanation of Benefit data retrieved from Blue
     * @return the ExplanationOfBenefit object
     */
    public static ExplanationOfBenefit getEOBFromFileInClassPath(String fileInClassPath) {
        if (StringUtils.isBlank(fileInClassPath)) {
            return null;
        }
        ClassLoader classLoader = EOBToAB2DEOB.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileInClassPath);
        return getParser().parseResource(ExplanationOfBenefit.class, inputStream);
    }

    /**
     * Retrieve the Explanation of Benefit object data from a Reader object
     * @param reader - the reader
     * @return the Explanation of Benefit object
     * @throws IOException if the file is invalid or unreadable
     */
    public static ExplanationOfBenefit getEOBFromReader(Reader reader) throws IOException {
        if (reader == null) {
            return null;
        }
        String inputStr;
        StringBuilder responseStrBuilder = new StringBuilder();
        BufferedReader bf = new BufferedReader(reader);
        while ((inputStr = bf.readLine()) != null) {
            responseStrBuilder.append(inputStr);
        }
        return getParser().parseResource(ExplanationOfBenefit.class, responseStrBuilder.toString());
    }

    /**
     * Convenience method to create the parser for any method that needs it.
     *
     * @return the parser
     */
    private static IParser getParser() {
        EncodingEnum respType = EncodingEnum.forContentType(EncodingEnum.JSON_PLAIN_STRING);
        return respType.newParser(FhirContext.forDstu3());
    }
}
