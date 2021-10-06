package gov.cms.ab2d.common.model;

/**
 * Enums to support the default 'since' functionality. If a FHIR version supports it, when a user
 * does not pass in a _since parameter to the job, AB2D will attempt to populate the _since date
 * with the last successful job's create date, automatically providing an incremental data pull
 * without having to manually specify it. If the FHIR version does not support default since
 * behavior and the user has not specified a _since value, there will be no value specified
 * as the _since source
 */
public enum SinceSource {
    // The _since data was provided by the PDP and was not changed (populated for all FHIR versions)
    USER,
    // The _since data was calculated and provided by AB2D (Only used with supported FHIR versions)
    AB2D,
    // Default 'since' was attempted but no successful previous job was found so this is it's
    // first run (only used with supported FHIR versions
    FIRST_RUN
}
