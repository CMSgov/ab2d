package gov.cms.ab2d.fhir;

import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseConformance;

/**
 * Used for parsing data related to capability statements for different versions of FHIR
 */
@Slf4j
public final class MetaDataUtils {

    private MetaDataUtils () { }

    /**
     * returns if metadata.getStatus() == ACTIVE to verify that the service is active
     *
     * @param resource - the meta data resource
     * @param version the FHIR version
     * @return true if the value is ACTIVE
     */
    public static boolean metaDataValid(IBaseConformance resource, Versions.FhirVersions version) {
        if (resource == null) {
            return false;
        }
        Object val = Versions.invokeGetMethod(resource, "getStatus");
        Object activeEnum = Versions.instantiateEnum(version, "Enumerations", "PublicationStatus", "ACTIVE");
        return val == activeEnum;
    }

    /**
     * Return the correct CapabilityStatement object for the correct version
     *
     * @param version - the version
     * @return the object
     */
    @SuppressWarnings("unchecked")
    public static Class<? extends IBaseConformance> getCapabilityClass(Versions.FhirVersions version) {
        try {
            return (Class<? extends IBaseConformance>) Class.forName(Versions.getClassName(version, "CapabilityStatement"));
        } catch (Exception ex) {
            return null;
        }
    }
}
