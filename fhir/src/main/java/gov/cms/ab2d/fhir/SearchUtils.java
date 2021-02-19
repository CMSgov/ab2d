package gov.cms.ab2d.fhir;

import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;

import gov.cms.ab2d.fhir.Versions.FhirVersions;

/**
 * Used to identify the correct classes for the correct FHIR version for searches
 */
@Slf4j
public final class SearchUtils {

    private SearchUtils() { }
    /**
     * Return the proper patient class for the version
     *
     * @param version - the FHIR version
     * @return the class
     */
    @SuppressWarnings("unchecked")
    public static Class<? extends IBaseResource> getPatientClass(FhirVersions version) {
        try {
            return (Class<? extends IBaseResource>) Class.forName(Versions.getClassName(version, "Patient"));
        } catch (Exception e) {
            log.error("Unable to get the right class for Patient", e);
            return null;
        }
    }

    /**
     * Return the proper Bundle class for the version
     *
     * @param version - the FHIR version
     * @return the class
     */
    @SuppressWarnings("unchecked")
    public static Class<? extends IBaseBundle> getBundleClass(FhirVersions version) {
        try {
            return (Class<? extends IBaseBundle>) Class.forName(Versions.getClassName(version, "Bundle"));
        } catch (Exception e) {
            log.error("Unable to get the right class for Bundle", e);
            return null;
        }
    }
}
