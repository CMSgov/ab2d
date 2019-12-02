package gov.cms.ab2d.filter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;

import java.io.InputStream;
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
    public static AB2DExplanationOfBenefit fromFile(String path) {
        System.out.println("Loading file " + path);
        ExplanationOfBenefit retVal = getEOB(path);
        return EOBToAB2DEOB.from(retVal);
    }

    /**
     * Parse and return the ExplanationOfBenefit file from a JOSN file
     * @param path - the file name and path in the classpath containing the
     *             Explanation of Benefit data retrieved from Blue
     * @return the ExplanationOfBenefit object
     */
    public static ExplanationOfBenefit getEOB(String path) {
        ClassLoader classLoader = EOBToAB2DEOB.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(path);

        EncodingEnum respType = EncodingEnum.forContentType(EncodingEnum.JSON_PLAIN_STRING);
        IParser parser = respType.newParser(FhirContext.forDstu3());
        return parser.parseResource(ExplanationOfBenefit.class, inputStream);
    }
}
