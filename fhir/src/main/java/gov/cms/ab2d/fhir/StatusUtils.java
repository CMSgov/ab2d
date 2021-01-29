package gov.cms.ab2d.fhir;

import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;

@Slf4j
/**
 * Time Utils for different versions of FHIR
 */
public class StatusUtils {
    /**
     * Return the DateTimeType.toHumanDisplay" value from an OffsetDateTIme
     *
     * @param version - the FHIR version
     * @param dateTime - the OffsetDateTime
     * @return the String for the human readable time
     */
    public static String getFhirTime(Versions.FhirVersions version, OffsetDateTime dateTime) {
        Object dt = Versions.getObject(version, "DateTimeType", dateTime.toString(), String.class);
        return (String) Versions.invokeGetMethod(dt, "toHumanDisplay");
    }
}
