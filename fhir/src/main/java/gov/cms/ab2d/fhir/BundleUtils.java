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

@Slf4j
public class BundleUtils {
    public static final String EOB = "ExplanationOfBenefit";

    public static String getAvailableLinks(IBaseBundle bundle) {
        if (bundle == null) {
            return null;
        }
        List links = (List) Versions.invokeGetMethod(bundle, "getLink");
        List<String> listValues = new ArrayList<>();
        for (Object o : links) {
            listValues.add((String) Versions.invokeGetMethod((IBase) o, "getRelation"));
        }
        return listValues.stream().collect(Collectors.joining(" , "));
    }

    public static String getAvailableLinksPretty(IBaseBundle bundle) {
        if (bundle == null) {
            return null;
        }
        List links = (List) Versions.invokeGetMethod(bundle, "getLink");
        List<String> listValues = new ArrayList<>();
        for (Object o : links) {
            listValues.add(Versions.invokeGetMethod((IBase) o, "getRelation") + " -> " +
                    Versions.invokeGetMethod((IBase) o, "getUrl"));
        }
        return listValues.stream().collect(Collectors.joining(" , "));
    }

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

    public static Stream<IDomainResource> getPatientStream(IBaseBundle bundle, Versions.FhirVersions version) {
        if (bundle == null) {
            return null;
        }
        Object patientEnum = Versions.instantiateEnum(version, "ResourceType", "Patient");
        List entries = getEntries(bundle);
        return entries.stream()
                .map(c -> Versions.invokeGetMethod(c, "getResource"))
                .filter(c -> Versions.invokeGetMethod(c, "getResourceType") == patientEnum);
    }

    public static List<IBaseBackboneElement> getEntries(IBaseBundle bundle) {
        if (bundle == null) {
            return null;
        }
        return (List<IBaseBackboneElement>) Versions.invokeGetMethod(bundle, "getEntry");
    }

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

    public static boolean isExplanationOfBenefitResource(IElement resource) {
        if (resource == null || resource.fhirType() == null || !resource.fhirType().endsWith(EOB)) {
            return false;
        }
        return true;
    }

    public static int getTotal(IBaseBundle bundle) {
        if (bundle == null) {
            return 0;
        }
        return (int) Versions.invokeGetMethod(bundle, "getTotal");
    }
}
