package gov.cms.ab2d.fhir;

import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;

@Slf4j
public class StatusUtils {
    public static String getFhirTime(Versions.FhirVersions version, OffsetDateTime dateTime) {
        Object dt = Versions.instantiateClassWithParam(version, "DateTimeType", dateTime.toString(), String.class);
        return (String) Versions.invokeGetMethod(dt, "toHumanDisplay");
    }
}
