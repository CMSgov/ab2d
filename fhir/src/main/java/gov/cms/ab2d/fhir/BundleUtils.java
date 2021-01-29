package gov.cms.ab2d.fhir;

import ca.uhn.fhir.model.api.IElement;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hl7.fhir.instance.model.api.IBaseBundle.LINK_NEXT;

/**
 * Utilities to create and parse Bundle objects for different FHIR versions
 */
@Slf4j
public final class BundleUtils {
    public static final String EOB = "ExplanationOfBenefit";
    public static final String PATIENT = "Patient";

    private BundleUtils() { }

    /**
     * Get available links from a bundle
     *
     * @param bundle - the Bundle object
     * @return a comma delimited list of relations
     */
    public static String getAvailableLinks(IBaseBundle bundle) {
        if (bundle == null) {
            return null;
        }
        List<?> links = (List<?>) Versions.invokeGetMethod(bundle, "getLink");
        List<String> listValues = new ArrayList<>();
        for (Object o : links) {
            listValues.add((String) Versions.invokeGetMethod(o, "getRelation"));
        }
        return String.join(" , ", listValues);
    }

    /**
     * Get available links from a bundle in a way that looks pretty
     *
     * @param bundle - the Bundle object
     * @return a comma delimited list of links and urls
     */
    public static String getAvailableLinksPretty(IBaseBundle bundle) {
        if (bundle == null) {
            return null;
        }
        List<?> links = (List<?>) Versions.invokeGetMethod(bundle, "getLink");
        List<String> listValues = new ArrayList<>();
        for (Object o : links) {
            listValues.add(Versions.invokeGetMethod(o, "getRelation") + " -> " +
                    Versions.invokeGetMethod(o, "getUrl"));
        }
        return String.join(" , ", listValues);
    }

    /**
     * Get the next link from a bundle if there are more results
     *
     * @param bundle - the Bundle object
     * @return the next link url
     */
    public static IBaseBackboneElement getNextLink(IBaseBundle bundle) {
        if (bundle == null) {
            return null;
        }
        try {
            return (IBaseBackboneElement) Versions.invokeGetMethodWithArg(bundle, "getLink", LINK_NEXT, String.class);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Return a stream of the Patient Resources from a bundle
     *
     * @param bundle - the Bundle object
     * @param version - the FHIR version
     * @return a stream of Patient Resources
     */
    public static Stream<IDomainResource> getPatientStream(IBaseBundle bundle, Versions.FhirVersions version) {
        if (bundle == null) {
            return null;
        }
        Object patientEnum = Versions.instantiateEnum(version, "ResourceType", PATIENT);
        List entries = getEntries(bundle);
        return entries.stream()
                .map(c -> Versions.invokeGetMethod(c, "getResource"))
                .filter(c -> Versions.invokeGetMethod(c, "getResourceType") == patientEnum);
    }

    /**
     * Bet the list of Bundle entries
     *
     * @param bundle - the bundle
     * @return the list of entries
     */
    @SuppressWarnings("unchecked")
    public static List<IBaseBackboneElement> getEntries(IBaseBundle bundle) {
        if (bundle == null) {
            return null;
        }
        return (List<IBaseBackboneElement>) Versions.invokeGetMethod(bundle, "getEntry");
    }

    /**
     * Given a bundle entries, return the EOB resources
     *
     * @param bundleComponents - the bundle entries
     * @return the List of ExplanationOfBenefit objects
     */
    public static List<IBaseResource> getEobResources(List<IBaseBackboneElement> bundleComponents) {
        try {
            return bundleComponents.stream()
                    .map(c -> (IBaseResource) Versions.invokeGetMethod(c, "getResource"))
                    .filter(Objects::nonNull)
                    .filter(BundleUtils::isExplanationOfBenefitResource)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Returns true if a Resource is an ExplanationOfBenefit object
     *
     * @param resource - the resource
     * @return true if it's an ExplanationOfBenefit object
     */
    public static boolean isExplanationOfBenefitResource(IElement resource) {
        return resource != null && resource.fhirType() != null && resource.fhirType().endsWith(EOB);
    }

    /**
     * Return the total number of returned values for the search
     *
     * @param bundle - the Bundle object
     * @return the total returned in the search
     */
    public static int getTotal(IBaseBundle bundle) {
        if (bundle == null) {
            return 0;
        }
        return (int) Versions.invokeGetMethod(bundle, "getTotal");
    }
}
