package gov.cms.ab2d.fhir;

import ca.uhn.fhir.model.api.IElement;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hl7.fhir.instance.model.api.IBaseBundle.LINK_NEXT;

@Slf4j
public class BundleUtils {
    public static final String EOB = "ExplanationOfBenefit";

    public static String getAvailableLinks(IBaseBundle bundle) {
        try {
            List links = (List) Versions.invokeGetMethod(bundle, "getLink");
            List<String> listValues = new ArrayList<>();
            for (Object o : links) {
                listValues.add((String) Versions.invokeGetMethod((IBase) o, "getRelation"));
            }
            return listValues.stream().collect(Collectors.joining(" , "));
        } catch (Exception ex) {
            log.error("Unable to find links from the bundle");
            return null;
        }
    }

    public static String getAvailableLinksPretty(IBaseBundle bundle) {
        try {
            List links = (List) Versions.invokeGetMethod(bundle, "getLink");
            List<String> listValues = new ArrayList<>();
            for (Object o : links) {
                listValues.add(Versions.invokeGetMethod((IBase) o, "getRelation") + " -> " +
                        Versions.invokeGetMethod((IBase) o, "getUrl"));
            }
            return listValues.stream().collect(Collectors.joining(" , "));
        } catch (Exception ex) {
            log.error("Unable to find links from the bundle", ex);
            return null;
        }
    }

    public static IBaseBackboneElement getNextLink(IBaseBundle bundle) {
        try {
            return (IBaseBackboneElement) Versions.invokeGetMethodWithArg(bundle, "getLink", LINK_NEXT, String.class);
        } catch (Exception ex) {
            log.error("Can't get next link from bundle", ex);
            return null;
        }
    }

    public static Stream<IDomainResource> getPatientStream(IBaseBundle bundle, Versions.FHIR_VERSIONS version) {
        try {
            Object patientEnum = Versions.instantiateEnum(version, "ResourceType", "Patient");
            List entries = getEntries(bundle);
            return entries.stream()
                    .map(c -> {
                        try {
                            return Versions.invokeGetMethod(c, "getResource");
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(c -> {
                        try {
                            return Versions.invokeGetMethod(c, "getResourceType") == patientEnum;
                        } catch (Exception e) {
                            return false;
                        }
                    });
        } catch (Exception ex) {
            log.error("Can't get entries from bundle", ex);
            return null;
        }
    }

    public static List<IBaseBackboneElement> getEntries(IBaseBundle bundle) {
        try {
            return (List<IBaseBackboneElement>) Versions.invokeGetMethod(bundle, "getEntry");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<IBaseResource> getEobResources(List<IBaseBackboneElement> bundleComponents) {
        try {
            return bundleComponents.stream()
                    .map(c -> {
                        try {
                            return (IBaseResource) Versions.invokeGetMethod((IBase) c, "getResource");
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(c -> c != null)
                    .filter(c -> isExplanationOfBenefitResource(c))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Can't find bundle components in bundle");
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
        try {
            return (int) Versions.invokeGetMethod(bundle, "getTotal");
        } catch (Exception e) {
            log.error("Unable to determine total number of values in bundle");
            return 0;
        }
    }
}
