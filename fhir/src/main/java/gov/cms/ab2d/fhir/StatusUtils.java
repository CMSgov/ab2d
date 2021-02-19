package gov.cms.ab2d.fhir;

import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;

import gov.cms.ab2d.fhir.Versions.FhirVersions;

/**
 * Time Utils for different versions of FHIR
 */
@Slf4j
public final class StatusUtils {

    private StatusUtils() { }
    /**
     * Return the DateTimeType.toHumanDisplay" value from an OffsetDateTIme
     *
     * @param version - the FHIR version
     * @param dateTime - the OffsetDateTime
     * @return the String for the human readable time
     */
    public static String getFhirTime(FhirVersions version, OffsetDateTime dateTime) {
        Object dt = Versions.getObject(version, "DateTimeType", dateTime.toString(), String.class);
        return (String) Versions.invokeGetMethod(dt, "toHumanDisplay");
    }
}
