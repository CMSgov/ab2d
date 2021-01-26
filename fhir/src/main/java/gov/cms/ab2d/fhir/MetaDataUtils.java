package gov.cms.ab2d.fhir;

import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseConformance;

@Slf4j
public class MetaDataUtils {
    public static boolean metaDataValid(IBaseConformance resource, Versions.FhirVersions version) {
        if (resource == null) {
            return false;
        }
        try {
            Object val = Versions.invokeGetMethod(resource, "getStatus");
            Object activeEnum = Versions.instantiateEnum(version, "Enumerations", "PublicationStatus", "ACTIVE");
            if (val == activeEnum) {
                return true;
            }
        } catch (Exception ex) {
            log.error("Unable to invoke getStatus from capability statement");
            return false;
        }
        return false;
    }

    public static Class<? extends IBaseConformance> getCapabilityClass(Versions.FhirVersions version) {
        try {
            return (Class<? extends IBaseConformance>)
                    Class.forName(Versions.getClassName(version, "CapabilityStatement"));
        } catch (Exception ex) {
            return null;
        }
    }
}
