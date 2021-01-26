package gov.cms.ab2d.fhir;

import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;

@Slf4j
public class SearchUtils {
    public static Class<? extends IBaseResource> getPatientClass(Versions.FHIR_VERSIONS version) {
        try {
            return (Class<? extends IBaseResource>) Class.forName(Versions.getClassName(version, "Patient"));
        } catch (Exception e) {
            log.error("Unable to get the right class for Patient", e);
            return null;
        }
    }

    public static Class<? extends IBaseBundle> getBundleClass(Versions.FHIR_VERSIONS version) {
        try {
            return (Class<? extends IBaseBundle>) Class.forName(Versions.getClassName(version, "Bundle"));
        } catch (Exception e) {
            log.error("Unable to get the right class for Bundle", e);
            return null;
        }
    }
}
